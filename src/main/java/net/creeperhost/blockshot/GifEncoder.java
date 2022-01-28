package net.creeperhost.blockshot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.squareup.gifencoder.Color;
import com.squareup.gifencoder.Image;
import com.squareup.gifencoder.ImageOptions;
import net.creeperhost.blockshot.mixin.MixinChatComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    private static AtomicReference<List<Image>> _frames = new AtomicReference<>();

    public static void addFrameAndClose(NativeImage screenImage) {
        //TODO: Figure out wtf is used in place of NativeImage
        CompletableFuture.runAsync(() -> {
            addedFrames.incrementAndGet();
            try {
                int i = screenImage.getWidth();
                int j = screenImage.getHeight();
                int k = 0;
                int l = 0;
                screenImage.flipY();
                int newx = 856;
                int newy = 482;
                int scaleFactor = 2;
                newx = newx - (((newx / 2) / 3) * scaleFactor);
                newy = newy - (((newy / 2) / 3) * scaleFactor);
                NativeImage nativeImage = new NativeImage(newx, newy, false);
                screenImage.resizeSubRectTo(k, l, i, j, nativeImage);
                screenImage.close();
                int w = nativeImage.getWidth();
                int h = nativeImage.getHeight();
                Color[][] colours = new Color[h][w];
                for (int y = 0; y < h; ++y) {
                    for (int x = 0; x < w; ++x) {
                        colours[y][x] = fromRgbMc(nativeImage.getPixelRGBA(x, y));
                    }
                }
                nativeImage.close();
                Image frame = Image.fromColors(colours);
                GifEncoder._frames.getAndUpdate((a) -> {
                    a.add(frame);
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

    //Minecraft's r and b channels work differently to the gif library...
    private static Color fromRgbMc(int rgb) {
        int redComponent = rgb & 0xFF;
        int greenComponent = rgb >>> 8 & 0xFF;
        int blueComponent = rgb >>> 16 & 0xFF;
        return new Color(redComponent / 255.0, greenComponent / 255.0, blueComponent / 255.0);
    }

    public static void begin() {
        if (_frames == null || _frames.get() == null) {
            _frames.set(new ArrayList<Image>());
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
            com.squareup.gifencoder.GifEncoder encoder = null;
            ImageOptions imageOptions = new ImageOptions();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            if (GifEncoder._frames.get() != null) {
                List<Image> frames = GifEncoder._frames.get();
                Image firstFrame = frames.get(0);
                message = new TextComponentString("[BlockShot] Preparation complete, encoding frames... ");
                if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(message, BlockShot.CHAT_ENCODING_ID);
                }
                try {
                    encoder = new com.squareup.gifencoder.GifEncoder(os, firstFrame.getWidth(), firstFrame.getHeight(), 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int duration = (int) (GifEncoder.totalSeconds / frames.size());
                int i = 0;
                int f = frames.size();
                for (Image frame : frames) {
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
                        imageOptions.setDelay(duration, TimeUnit.MILLISECONDS);
                        encoder.addImage(frame, imageOptions);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    encoder.finishEncoding();
                    frames.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                GifEncoder._frames.set(new ArrayList<Image>());
                message = new TextComponentString("[BlockShot] Encoding complete... Starting upload...");
                if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().deleteChatLine(BlockShot.CHAT_ENCODING_ID);
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(message, BlockShot.CHAT_UPLOAD_ID);
                }
                try {
                    byte[] bytes = os.toByteArray();
                    BlockShot.uploadAndAddToChat(bytes);
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
