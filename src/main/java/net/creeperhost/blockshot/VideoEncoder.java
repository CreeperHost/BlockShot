package net.creeperhost.blockshot;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.lwjgl.BufferUtils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.jcodec.common.model.ColorSpace.RGB;

public class VideoEncoder {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TARGET_WIDTH = 1280;
    private static final int TARGET_HEIGHT = 720;
    private static final int FPS = 15;
    private static final int MAX_DURATION = 30;
    private static final File TEMP_FILE = new File(Minecraft.getMinecraft().mcDataDir, "screenshots/blockshot.temp.webm");

    private static final ExecutorService RECORDING_EXECUTOR = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder().setNameFormat("blockshot-recorder-%d").setDaemon(true).build());
    private static final ExecutorService ENCODING_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("blockshot-encoder-%d").setDaemon(true).build());
    private static final List<CompletableFuture<?>> ACTIVE_FUTURES = Collections.synchronizedList(new ArrayList<CompletableFuture<?>>());
    private static final Object ENCODER_LOCK = new Object();
    private static final AtomicDouble UPLOAD_PROGRESS = new AtomicDouble(0);

    private static SequenceEncoder activeEncoder;
    private static boolean isRecording;
    private static boolean stopping;
    private static boolean canceled;
    private static long recordStartTime;
    private static long lastFrameTime;

    public static void startOrStopRecording() {
        if (stopping) {
            return;
        }
        if (isWorking()) {
            stopping = true;
            sendEncodingMessage("[BlockShot] Gameplay recording complete, preparing...");
            return;
        }

        try {
            TEMP_FILE.getParentFile().mkdirs();
            activeEncoder = new SequenceEncoder(NIOUtils.writableChannel(TEMP_FILE), Rational.R(FPS, 1), Format.MKV, Codec.VP8, null);
            recordStartTime = lastFrameTime = System.currentTimeMillis();
            UPLOAD_PROGRESS.set(0);
            isRecording = true;
            canceled = false;
            sendEncodingMessage("[BlockShot] You are now recording gameplay!");
        } catch (IOException e) {
            LOGGER.error("Something went wrong while starting video recording", e);
            sendEncodingMessage("[BlockShot] Unable to start recording: " + e.getMessage());
            resetState();
        }
    }

    public static void updateCapture() {
        if (activeEncoder == null) {
            return;
        }

        long time = System.currentTimeMillis();
        if (time - lastFrameTime < 1000 / FPS) {
            return;
        }

        if ((time - recordStartTime) / 1000 > MAX_DURATION) {
            stopping = true;
        }

        removeCompletedFutures();
        if (stopping) {
            finishWhenReady();
            return;
        }

        final Frame frame = captureFrame();
        if (frame == null) {
            return;
        }

        while (time - lastFrameTime > 1000 / FPS) {
            addFrame(frame);
            lastFrameTime += 1000 / FPS;
        }
    }

    public static void cancelRecording() {
        if (!isWorking()) {
            return;
        }
        canceled = true;
        stopping = true;
        sendEncodingMessage("[BlockShot] Gameplay recording canceled.");
    }

    public static boolean isWorking() {
        return isRecording;
    }

    public static List<String> getHudText() {
        List<String> lines = new ArrayList<String>();
        String key = Minecraft.getMinecraft().gameSettings.keyBindScreenshot.getDisplayName();

        if (!stopping) {
            lines.add(TextFormatting.RED + "Recording with BlockShot");
            lines.add(TextFormatting.GRAY + "Press Ctrl + " + key + " to finish recording.");
            lines.add(TextFormatting.GRAY + "Press Shift + " + key + " to cancel recording.");
        } else if (activeEncoder == null) {
            lines.add(TextFormatting.RED + "Uploading: " + Math.round(UPLOAD_PROGRESS.get() * 100) + "%");
        } else {
            lines.add(TextFormatting.RED + "Encoding " + ACTIVE_FUTURES.size() + " queued frames...");
        }
        return lines;
    }

    private static Frame captureFrame() {
        try {
            Framebuffer framebuffer = Minecraft.getMinecraft().getFramebuffer();
            int width = framebuffer.framebufferTextureWidth;
            int height = framebuffer.framebufferTextureHeight;
            int[] pixelValues = new int[width * height];
            IntBuffer pixelBuffer = BufferUtils.createIntBuffer(width * height);

            GlStateManager.glPixelStorei(3333, 1);
            GlStateManager.glPixelStorei(3317, 1);
            pixelBuffer.clear();
            if (OpenGlHelper.isFramebufferEnabled()) {
                GlStateManager.bindTexture(framebuffer.framebufferTexture);
                GlStateManager.glGetTexImage(3553, 0, 32993, 33639, pixelBuffer);
            } else {
                GlStateManager.glReadPixels(0, 0, width, height, 32993, 33639, pixelBuffer);
            }
            pixelBuffer.get(pixelValues);
            TextureUtil.processPixelValues(pixelValues, width, height);
            return new Frame(width, height, pixelValues);
        } catch (Throwable t) {
            LOGGER.error("An error occurred while capturing a video frame", t);
            canceled = true;
            stopping = true;
            return null;
        }
    }

    private static void addFrame(final Frame frame) {
        CompletableFuture<?> future = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedImage image = frame.toBufferedImage(TARGET_WIDTH, TARGET_HEIGHT);
                    synchronized (ENCODER_LOCK) {
                        if (activeEncoder != null && !canceled) {
                            activeEncoder.encodeNativeFrame(fromBufferedImageRGB(image));
                        }
                    }
                    image.flush();
                } catch (Throwable t) {
                    if (!canceled) {
                        LOGGER.error("Something went wrong while encoding video frame", t);
                        canceled = true;
                        stopping = true;
                    }
                }
            }
        }, ENCODING_EXECUTOR);
        ACTIVE_FUTURES.add(future);
    }

    private static void finishWhenReady() {
        if (canceled) {
            synchronized (ACTIVE_FUTURES) {
                for (CompletableFuture<?> future : ACTIVE_FUTURES) {
                    future.cancel(true);
                }
                ACTIVE_FUTURES.clear();
            }
        }

        removeCompletedFutures();
        if (!ACTIVE_FUTURES.isEmpty()) {
            return;
        }

        try {
            synchronized (ENCODER_LOCK) {
                if (activeEncoder != null) {
                    activeEncoder.finish();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Something went wrong while finishing video encoding", e);
            canceled = true;
        }

        uploadAndCleanup();
    }

    private static void uploadAndCleanup() {
        activeEncoder = null;

        if (canceled) {
            TEMP_FILE.delete();
            resetState();
            return;
        }

        UPLOAD_PROGRESS.set(0);
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                sendUploadMessage("[BlockShot] Encoding complete... Starting upload...");
                try (FileInputStream inputStream = new FileInputStream(TEMP_FILE)) {
                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    BlockShot.uploadAndAddToChat(bytes, true, "webm", WebUtils.MediaType.WEBM, UPLOAD_PROGRESS);
                } catch (IOException e) {
                    LOGGER.error("An error occurred while uploading video", e);
                    BlockShot.saveLocalFallback(null, TEMP_FILE, "webm");
                } finally {
                    TEMP_FILE.delete();
                    resetState();
                }
            }
        }, RECORDING_EXECUTOR);
    }

    private static void resetState() {
        isRecording = false;
        stopping = false;
        canceled = false;
        activeEncoder = null;
        removeCompletedFutures();
    }

    private static void removeCompletedFutures() {
        synchronized (ACTIVE_FUTURES) {
            ACTIVE_FUTURES.removeIf(CompletableFuture::isDone);
        }
    }

    private static Picture fromBufferedImageRGB(BufferedImage src) {
        Picture dst = Picture.create(src.getWidth(), src.getHeight(), RGB);
        byte[] dstData = dst.getPlaneData(0);
        int off = 0;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                dstData[off++] = (byte) (((rgb >> 16) & 0xff) - 128);
                dstData[off++] = (byte) (((rgb >> 8) & 0xff) - 128);
                dstData[off++] = (byte) ((rgb & 0xff) - 128);
            }
        }
        return dst;
    }

    private static void sendEncodingMessage(String message) {
        sendMessage(message, BlockShot.CHAT_ENCODING_ID);
    }

    private static void sendUploadMessage(String message) {
        sendMessage(message, BlockShot.CHAT_UPLOAD_ID);
    }

    private static void sendMessage(String message, int chatId) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.ingameGUI != null && minecraft.ingameGUI.getChatGUI() != null) {
            minecraft.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new TextComponentString(message), chatId);
                }
            });
        }
    }

    private static class Frame {
        private final int width;
        private final int height;
        private final int[] pixelValues;

        private Frame(int width, int height, int[] pixelValues) {
            this.width = width;
            this.height = height;
            this.pixelValues = pixelValues;
        }

        private BufferedImage toBufferedImage(int targetWidth, int targetHeight) {
            if (width > height) {
                double ratio = (double) height / width;
                targetHeight = even((int) Math.round(targetWidth * ratio));
            } else {
                double ratio = (double) width / height;
                targetWidth = even((int) Math.round(targetHeight * ratio));
            }

            BufferedImage source = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            source.setRGB(0, 0, width, height, pixelValues, 0, width);
            Image scaled = source.getScaledInstance(targetWidth, targetHeight, Image.SCALE_FAST);
            BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D graphics = result.createGraphics();
            graphics.drawImage(scaled, 0, 0, null);
            graphics.dispose();
            scaled.flush();
            source.flush();
            return result;
        }

        private int even(int value) {
            return (value & 1) == 0 ? value : value + 1;
        }
    }
}
