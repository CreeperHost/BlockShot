package net.creeperhost.blockshot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.squareup.gifencoder.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.nio.IntBuffer;
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

    public static void addFrameAndClose(int width, int height, IntBuffer pixelBuffer) {
        CompletableFuture.runAsync(() -> {
            addedFrames.incrementAndGet();
            try {
                int[] pixelValues = new int[width * height];
                pixelBuffer.get(pixelValues);
                TextureUtil.processPixelValues(pixelValues, width, height);
                BufferedImage bufferedimage = new BufferedImage(width, height, 1);
                bufferedimage.setRGB(0, 0, width, height, pixelValues, 0, width);
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

                BufferedImage screenImage = new BufferedImage(width, height, 1);
                screenImage.setRGB(0, 0, width, height, pixelValues, 0, width);
                bufferedimage.getGraphics().dispose();
                bufferedimage.flush();
                java.awt.Image nativeImage = screenImage.getScaledInstance(new_width, new_height, java.awt.Image.SCALE_FAST);
                BufferedImage finalFrame = new BufferedImage(new_width, new_height, 1);
                finalFrame.getGraphics().drawImage(nativeImage, 0, 0, null);
                nativeImage.flush();
                GifEncoder._frames.getAndUpdate((a) -> {
                    a.add(finalFrame);
                    return a;
                });
                finalFrame.getGraphics().dispose();
                finalFrame.flush();
                screenImage.getGraphics().dispose();
                screenImage.flush();
            } catch (Throwable t) {
                t.printStackTrace();
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
            ITextComponent message = new TextComponentString("[BlockShot] You are now recording gameplay! ");
            if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(message, BlockShot.CHAT_ENCODING_ID);
            }
            while (isRecording) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
            message = new TextComponentString("[BlockShot] Gameplay recording complete, preparing... ");
            if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(message, BlockShot.CHAT_ENCODING_ID);
            }
            while (addedFrames.get() > processedFrames.get()) {
                try {
                    message = new TextComponentString("[BlockShot] Preparing frame " + processedFrames + " of " + addedFrames.get());
                    if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(message, BlockShot.CHAT_ENCODING_ID);
                    }
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
            if (GifEncoder._frames.get() != null) {
                List<BufferedImage> frames = GifEncoder._frames.get();
                BufferedImage firstFrame = frames.get(0);
                GifSequenceWriter writer = null;
                ByteArrayOutputStream outputStream = null;
                ImageOutputStream imageStream = null;
                int duration = (int) (GifEncoder.totalSeconds / frames.size());

                message = new TextComponentString("[BlockShot] Preparation complete, encoding frames... ");
                if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(message, BlockShot.CHAT_ENCODING_ID);
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
                        message = new TextComponentString("[BlockShot] Encoding frame " + i + " of " + f + dots);
                        if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(message, BlockShot.CHAT_ENCODING_ID);
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
                message = new TextComponentString("[BlockShot] Encoding complete... Starting upload...");
                if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().deleteChatLine(BlockShot.CHAT_ENCODING_ID);
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(message, BlockShot.CHAT_UPLOAD_ID);
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
