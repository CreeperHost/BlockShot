package net.creeperhost.blockshot.capture;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.platform.Platform;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.ClientUtil;
import net.creeperhost.blockshot.WebUtils;
import net.creeperhost.blockshot.WebUtils.MediaType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.jcodec.common.model.ColorSpace.RGB;

public class VideoEncoder implements Encoder {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TARGET_WIDTH = 1280;
    private static final int TARGET_HEIGHT = 720;
    private static final int FPS = 15;
    private static final int MAX_DURATION = 30;
    private static final MediaType MEDIA_TYPE = MediaType.WEBM;

    private final ExecutorService RECORDING_EXECUTOR = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder().setNameFormat("blockshot-recorder-%d").setDaemon(true).build());
    private final ExecutorService ENCODING_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("blockshot-encoder-%d").setDaemon(true).build());
    private final File tempFile = new File(Platform.getGameFolder().toFile(), "screenshots/blockshot.temp.mov");
    private final List<CompletableFuture<?>> activeFutures = new ArrayList<>();

    private AtomicDouble uploadProgress = new AtomicDouble(0);

    private SequenceEncoder activeEncoder = null;
    private boolean isRecording = false;
    private boolean stopping = false;
    private boolean canceled = false;
    private long recordStartTime = 0;
    private long lastFrameTime = 0;

    @Override
    public void startOrStopRecording() {
        if (stopping) return;
        try {
            if (isWorking()) {
                stopping = true;
            } else {
                isRecording = true;
                tempFile.getParentFile().mkdirs();
                activeEncoder = new SequenceEncoder(NIOUtils.writableChannel(tempFile), Rational.R(FPS, 1), Format.MKV, Codec.VP8, null);
                recordStartTime = lastFrameTime = System.currentTimeMillis();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateCapture() {
        if (activeEncoder == null) return;

        long time = System.currentTimeMillis();
        //Wait for frame duration.
        if (time - lastFrameTime < 1000 / FPS) {
            return;
        }

        //Auto stop after max duration
        if ((time - recordStartTime) / 1000 > MAX_DURATION) {
            stopping = true;
        }

        activeFutures.removeIf(CompletableFuture::isDone);

        if (stopping) {
            if (canceled) {
                activeFutures.forEach(e -> e.cancel(true));
            }

            if (activeFutures.isEmpty()) {
                try {
                    activeEncoder.finish();
                } catch (IOException e) {
                    LOGGER.error("Something went wrong while encoding video", e);
                }
                uploadAndCleanup();
            }
            return;
        }

        RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
        NativeImage nativeImage = new NativeImage(renderTarget.width, renderTarget.height, false);
        RenderSystem.bindTexture(renderTarget.getColorTextureId());
        nativeImage.downloadTexture(0, true);

        CompletableFuture<BufferedImage> converter = CompletableFuture.supplyAsync(() -> toBufferedImage(nativeImage, TARGET_WIDTH, TARGET_HEIGHT), RECORDING_EXECUTOR);
        activeFutures.add(converter);

        //Duplicates frames when required to achieve target frame rate.
        while (time - lastFrameTime > 1000 / FPS) {
            addFrame(converter);
            lastFrameTime += 1000 / FPS;
        }
    }

    private void addFrame(CompletableFuture<BufferedImage> converter) {
        CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
            try {
                BufferedImage image = converter.get();
                synchronized (ENCODING_EXECUTOR) {
                    activeEncoder.encodeNativeFrame(fromBufferedImageRGB(image));
                }
            } catch (ExecutionException | InterruptedException | IOException e) {
                if (!canceled) {
                    LOGGER.error("Something went wrong while encoding video frame", e);
                    canceled = true;
                    stopping = true;
                }
            }
        }, ENCODING_EXECUTOR);
        activeFutures.add(future);
    }

    @Override
    public void cancelRecording() {
        if (activeEncoder != null) {
            stopping = canceled = true;
        }
    }

    @Override
    public boolean isWorking() {
        return isRecording;
    }

    private void uploadAndCleanup() {
        activeEncoder = null;

        if (canceled) {
            tempFile.delete();
            isRecording = stopping = canceled = false;
            return;
        }

        uploadProgress.set(0);
        //Upload
        CompletableFuture.runAsync(() -> {
            uploadAndAddToChat(true, "mov", uploadProgress);
            isRecording = stopping = false;
            tempFile.delete();
        });
    }

    public void uploadAndAddToChat(boolean writeOnFail, String fallbackExt, @Nullable AtomicDouble progress) {
        Component finished = Component.translatable("chat.blockshot.upload.uploading");
        ClientUtil.sendMessage(finished, BlockShot.CHAT_UPLOAD_ID);

        String result = uploadImage(tempFile, progress);
        if (result == null) {
            finished = Component.translatable("chat.blockshot.upload.error");
            ClientUtil.sendMessage(finished, BlockShot.CHAT_UPLOAD_ID);

            //Fallback
            if (writeOnFail) {
                try (FileInputStream is = new FileInputStream(tempFile)) {
                    ScreenshotHandler.saveLocal(is.readAllBytes(), Platform.getGameFolder().toFile(), null, fallbackExt, ClientUtil::sendMessage, "chat.blockshot.fallback.success", "chat.blockshot.fallback.failure");
                } catch (IOException e) {
                    LOGGER.error("An error occurred while uploading image", e);
                }
            }
        } else if (result.startsWith("http")) {
            Component link = (Component.literal(result)).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, result)));
            finished = Component.translatable("chat.blockshot.upload.uploaded").append(" ").append(link);
            ClientUtil.deleteMessage(BlockShot.CHAT_UPLOAD_ID);
            ClientUtil.sendMessage(finished);
        }
    }

    public String uploadImage(File file, @Nullable AtomicDouble progress) {
        try (InputStream is = new FileInputStream(file)) {
            String rsp = WebUtils.post("https://blockshot.ch/upload", Base64.getEncoder().encodeToString(is.readAllBytes()), MEDIA_TYPE, progress);
            return ScreenshotHandler.readJsonResponse(rsp);
        } catch (Throwable t) {
            LOGGER.error("An error occurred while uploading video", t);
        }
        return null;
    }

    @Override
    public boolean showRecordIcon() {
        return isWorking() && !stopping;
    }

    @Override
    public List<Component> getHudText() {
        List<Component> list = new ArrayList<>();
        String screenshotKey = Minecraft.getInstance().options.keyScreenshot.getTranslatedKeyMessage().getString();

        if (!stopping) {
            list.add(Component.translatable("overlay.blockshot.recording").withStyle(ChatFormatting.RED));
            list.add(Component.translatable("overlay.blockshot.finish", screenshotKey).withStyle(ChatFormatting.GRAY));
        }

        if (stopping && !activeFutures.isEmpty()) {
            list.add(Component.translatable("overlay.blockshot.encoding", activeFutures.size()).withStyle(ChatFormatting.RED));
        } else if (stopping && activeEncoder == null) {
            list.add(Component.translatable("overlay.blockshot.uploading").append(": " + Math.round(uploadProgress.get() * 100) + "%").withStyle(ChatFormatting.RED));
        }

        if (!canceled && activeEncoder != null) {
            list.add(Component.translatable("overlay.blockshot.cancel", screenshotKey).withStyle(ChatFormatting.GRAY));
        }

        return list;
    }

    private static Picture fromBufferedImageRGB(BufferedImage src) {
        Picture dst = Picture.create(src.getWidth(), src.getHeight(), RGB);
        fromBufferedImage(src, dst);
        return dst;
    }

    private static void fromBufferedImage(BufferedImage src, Picture dst) {
        byte[] dstData = dst.getPlaneData(0);
        int off = 0;
        for (int i = 0; i < src.getHeight(); i++) {
            for (int j = 0; j < src.getWidth(); j++) {
                int rgb1 = src.getRGB(j, i);
                dstData[off++] = (byte) (((rgb1 >> 16) & 0xff) - 128);
                dstData[off++] = (byte) (((rgb1 >> 8) & 0xff) - 128);
                dstData[off++] = (byte) ((rgb1 & 0xff) - 128);
            }
        }
    }
}
