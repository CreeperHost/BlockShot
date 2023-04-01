package net.creeperhost.blockshot.capture;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.ClientUtil;
import net.creeperhost.blockshot.WebUtils;
import net.creeperhost.polylib.client.gif.GifSequenceWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class GifEncoder implements Encoder {
    private static Logger LOGGER = LogManager.getLogger();
    private ExecutorService rendering = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("blockshot-framerenderer-%d").build());
    private ExecutorService encoding = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("blockshot-imageencoder-%d").build());
    private AtomicInteger addedFrames = new AtomicInteger();
    private AtomicInteger processedFrames = new AtomicInteger();
    private boolean isRecording = false;
    private boolean stopping = false;
    private boolean isCanceled = false;
    private long lastTimestamp;
    private long frames;
    private long totalSeconds;
    private AtomicReference<List<BufferedImage>> _frames = new AtomicReference<>();
    private AtomicDouble uploadProgress = new AtomicDouble(0);

    private long currentFrame = 0;
    private String totalFrames = "";

    @Override
    public void updateCapture() {
        if (stopping) return;
        int skipFrames = 6;
        if (BlockShot.getFPS() > 20) {
            skipFrames = (BlockShot.getFPS() / 10);
        }

        if (frames > skipFrames || (lastTimestamp != (System.currentTimeMillis() / 1000))) {
            frames = 0;
            if (lastTimestamp != (System.currentTimeMillis() / 1000)) {
                lastTimestamp = (System.currentTimeMillis() / 1000);
                totalSeconds++;
            }
            RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
            NativeImage nativeImage = new NativeImage(renderTarget.width, renderTarget.height, false);
            RenderSystem.bindTexture(renderTarget.getColorTextureId());
            nativeImage.downloadTexture(0, true);
            addFrame(nativeImage);
            if (totalSeconds > 30) stopping = true;
        } else {
            frames++;
        }
    }

    private void addFrame(NativeImage screenImage) {
        CompletableFuture.runAsync(() -> {
            addedFrames.incrementAndGet();
            BufferedImage image = toBufferedImage(screenImage, 572, 322);
            if (image != null) {
                _frames.getAndUpdate((a) -> {
                    a.add(image);
                    return a;
                });
            }
            processedFrames.incrementAndGet();
        }, rendering);
    }

    private void begin() {
        if (_frames == null || _frames.get() == null) {
            _frames.set(new ArrayList<>());
        }
        if (isRecording) return;
        lastTimestamp = 0;
        frames = 0;
        totalSeconds = 0;
        isRecording = true;
        CompletableFuture.runAsync(() -> {
            waitForFinish();

            if (isCanceled) {
                isCanceled = false;
                ClientUtil.sendMessage(Component.translatable("chat.blockshot.record.canceled"), BlockShot.CHAT_ENCODING_ID);
            } else {
                ClientUtil.sendMessage(Component.translatable("chat.blockshot.record.complete"), BlockShot.CHAT_ENCODING_ID);
                generateGif();
            }

            addedFrames.set(0);
            processedFrames.set(0);
            isRecording = stopping = false;
        }, encoding);
    }

    private void waitForFinish() {
        //Combined frame wait here because realistically it should not take more than a fraction of a second.
        //So do we really need to give the user a progress indicator?
        while ((isRecording && !stopping) || addedFrames.get() > processedFrames.get()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) { }
        }
    }

    private void generateGif() {
        if (_frames.get() == null) {
            return;
        }
        List<BufferedImage> frames = _frames.get();
        BufferedImage firstFrame = frames.get(0);
        GifSequenceWriter writer = null;
        ByteArrayOutputStream outputStream = null;
        ImageOutputStream imageStream = null;
        int duration = (int) (totalSeconds / frames.size());

        Component message = Component.translatable("chat.blockshot.record.preparing.complete");
        ClientUtil.sendMessage(message, BlockShot.CHAT_ENCODING_ID);
        try {
            outputStream = new ByteArrayOutputStream();
            imageStream = ImageIO.createImageOutputStream(outputStream);
            writer = new GifSequenceWriter(imageStream, firstFrame.getType(), duration, true);
            writer.writeToSequence(firstFrame);
        } catch (IOException e) {
            LOGGER.error("An error occurred while writing frames", e);
            IOUtils.closeQuietly(outputStream, imageStream);
            return;
        }
        int i = 0;
        int f = frames.size();
        for (BufferedImage frame : frames) {
            try {
                i++;
                String dots = "";
                for (int z = 0; z <= (i % 3); z++) {
                    dots += ".";
                }
                currentFrame = i;
                totalFrames = f + dots;
//                message = Component.translatable("chat.blockshot.record.encoding", i, f + dots);
//                ClientUtil.sendMessage(message, BlockShot.CHAT_ENCODING_ID);
                writer.writeToSequence(frame);
            } catch (IOException e) {
                LOGGER.error("An error occurred while writing frames", e);
                IOUtils.closeQuietly(outputStream, imageStream);
                return;
            }
        }
        try {
            writer.close();
            imageStream.close();
            frames.clear();
        } catch (IOException e) {
            LOGGER.error("An error occurred while writing frames", e);
            IOUtils.closeQuietly(outputStream, imageStream);
            return;
        }
        totalFrames = null;
        _frames.set(new ArrayList<>());
        message = Component.translatable("chat.blockshot.record.start.upload");
        ClientUtil.deleteMessage(BlockShot.CHAT_ENCODING_ID);
        ClientUtil.sendMessage(message, BlockShot.CHAT_UPLOAD_ID);
        try {
            byte[] bytes = outputStream.toByteArray();
            outputStream.close();
            ScreenshotHandler.uploadAndAddToChat(bytes, true, "gif", uploadProgress, WebUtils.MediaType.GIF);
            outputStream.close();
        } catch (Exception e) {
            LOGGER.error("An error occurred while writing frames", e);
        }
    }


    @Override
    public void startOrStopRecording() {
        if (isRecording) {
            stopping = true;
        } else if (canRecord() && !isWorking()) {
            begin();
        }
    }

    @Override
    public void cancelRecording() {
        if (isRecording) {
            isCanceled = true;
            stopping = true;
        }
    }

    @Override
    public boolean isWorking() {
        return isRecording;
    }

    private boolean canRecord() {
        return processedFrames.get() == 0 && addedFrames.get() == 0;
    }

    @Override
    public boolean showRecordIcon() {
        return isRecording && !stopping;
    }

    @Override
    public List<Component> getHudText() {
        List<Component> list = new ArrayList<>();

        if (!stopping) {
            String screenshotKey = Minecraft.getInstance().options.keyScreenshot.getTranslatedKeyMessage().getString();
            list.add(Component.translatable("overlay.blockshot.recording").withStyle(ChatFormatting.RED));
            list.add(Component.translatable("overlay.blockshot.finish", screenshotKey).withStyle(ChatFormatting.GRAY));
            list.add(Component.translatable("overlay.blockshot.cancel", screenshotKey).withStyle(ChatFormatting.GRAY));
        }

        if (stopping && totalFrames != null) {
            list.add(Component.translatable("overlay.blockshot.encoding_frame_of", currentFrame, totalFrames).withStyle(ChatFormatting.RED));
        } else if (stopping) {
            list.add(Component.translatable("overlay.blockshot.uploading").append(": " + Math.round(uploadProgress.get() * 100) + "%").withStyle(ChatFormatting.RED));
        }

        return list;
    }
}
