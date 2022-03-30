package net.creeperhost.blockshot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.mixin.MixinChatComponent;
import net.creeperhost.polylib.client.gif.GifSequenceWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

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

public class GifEncoder {
    private static ExecutorService rendering = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("blockshot-framerenderer-%d").build());
    private static ExecutorService encoding = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("blockshot-imageencoder-%d").build());
    public static AtomicInteger addedFrames = new AtomicInteger();
    public static AtomicInteger processedFrames = new AtomicInteger();
    public static boolean isRecording = false;
    public static long lastTimestamp;
    public static long frames;
    public static long totalSeconds;
    private static AtomicReference<List<BufferedImage>> _frames = new AtomicReference<>();

    public static void addFrameAndClose(NativeImage screenImage) {
        CompletableFuture.runAsync(() -> {
            addedFrames.incrementAndGet();
            try {
                int width = screenImage.getWidth();
                int height = screenImage.getHeight();
                int k = 0;
                int l = 0;
                screenImage.flipY();
                int new_width = 856;
                int new_height = 482;
                int scaleFactor = 2;
                new_width = new_width - (((new_width / 2) / 3) * scaleFactor);
                new_height = new_height - (((new_height / 2) / 3) * scaleFactor);

                if(width > height) {
                    double ratio = (double)height / width;
                    new_height = (int) Math.round(new_width * ratio);
                } else {
                    double ratio = (double)width / height;
                    new_width = (int) Math.round(new_height * ratio);
                }
                NativeImage nativeImage = new NativeImage(new_width, new_height, false);
                screenImage.resizeSubRectTo(k, l, width, height, nativeImage);
                screenImage.close();
                InputStream is = new ByteArrayInputStream(nativeImage.asByteArray());
                BufferedImage finalFrame = new BufferedImage(new_width, new_height, 1);
                finalFrame.getGraphics().drawImage(ImageIO.read(is), 0, 0, null);
                nativeImage.close();
                GifEncoder._frames.getAndUpdate((a) -> {
                    a.add(finalFrame);
                    return a;
                });
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                screenImage.close();
            }
            processedFrames.incrementAndGet();
        }, rendering);
    }

    public static void begin() {
        if (_frames == null || _frames.get() == null) {
            _frames.set(new ArrayList<BufferedImage>());
        }
        if (isRecording == true) return;
        lastTimestamp = 0;
        frames = 0;
        totalSeconds = 0;
        isRecording = true;
        CompletableFuture.runAsync(() -> {
            Component message = new TextComponent("[BlockShot] You are now recording gameplay! ");
            if (Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokeaddMessage(message, BlockShot.CHAT_ENCODING_ID);
            }
            while (isRecording) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
            message = new TextComponent("[BlockShot] Gameplay recording complete, preparing... ");
            if (Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokeaddMessage(message, BlockShot.CHAT_ENCODING_ID);
            }
            while (addedFrames.get() > processedFrames.get()) {
                try {
                    message = new TextComponent("[BlockShot] Preparing frame " + processedFrames + " of " + addedFrames.get());
                    if (Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                        ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokeaddMessage(message, BlockShot.CHAT_ENCODING_ID);
                    }
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            if (GifEncoder._frames.get() != null) {
                List<BufferedImage> frames = GifEncoder._frames.get();
                BufferedImage firstFrame = frames.get(0);
                GifSequenceWriter writer = null;
                ByteArrayOutputStream outputStream = null;
                ImageOutputStream imageStream = null;
                int duration = (int) (GifEncoder.totalSeconds / frames.size());

                message = new TextComponent("[BlockShot] Preparation complete, encoding frames... ");
                if (Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                    ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokeaddMessage(message, BlockShot.CHAT_ENCODING_ID);
                }
                try {
                    outputStream = new ByteArrayOutputStream();
                    imageStream = ImageIO.createImageOutputStream(outputStream);
                    writer = new GifSequenceWriter(imageStream, firstFrame.getType(), duration, true);
                    writer.writeToSequence(firstFrame);
                } catch (IOException e) {
                    e.printStackTrace();
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
                        message = new TextComponent("[BlockShot] Encoding frame " + i + " of " + f + dots);
                        if (Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                            ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokeaddMessage(message, BlockShot.CHAT_ENCODING_ID);
                        }
                        writer.writeToSequence(frame);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    writer.close();
                    imageStream.close();
                    frames.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                GifEncoder._frames.set(new ArrayList<BufferedImage>());
                message = new TextComponent("[BlockShot] Encoding complete... Starting upload...");
                if (Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                    ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokeremoveById(BlockShot.CHAT_ENCODING_ID);
                    ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokeaddMessage(message, BlockShot.CHAT_UPLOAD_ID);
                }
                try {
                    byte[] bytes = outputStream.toByteArray();
                    outputStream.close();
                    BlockShot.uploadAndAddToChat(bytes);
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            addedFrames.set(0);
            processedFrames.set(0);
            isRecording = false;
        }, encoding);
    }
}
