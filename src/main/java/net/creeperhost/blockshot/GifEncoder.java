package net.creeperhost.blockshot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.squareup.gifencoder.Color;
import com.squareup.gifencoder.Image;
import com.squareup.gifencoder.ImageOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.MemoryUtil;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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
                int newx = 856;
                int newy = 482;
                int scaleFactor = 2;
                newx = newx - (((newx / 2) / 3) * scaleFactor);
                newy = newy - (((newy / 2) / 3) * scaleFactor);
                BufferedImage screenImage = new BufferedImage(width, height, 1);
                screenImage.setRGB(0, 0, width, height, pixelValues, 0, width);
                bufferedimage.getGraphics().dispose();
                bufferedimage.flush();
                //TODO: Investigate resizing to ensure we're not breaking aspect ratio...
                java.awt.Image nativeImage = screenImage.getScaledInstance(newx, newy, java.awt.Image.SCALE_FAST);
                BufferedImage finalFrame = new BufferedImage(newx, newy, 1);
                finalFrame.getGraphics().drawImage(nativeImage, 0, 0, null);
//                int w = screenImage.getWidth();
//                int h = screenImage.getHeight();
//                Color[][] colours = new Color[h][w];
//                for (int y = 0; y < h; ++y) {
//                    for (int x = 0; x < w; ++x) {
//                        colours[y][x] = fromRgbMc(screenImage.getRGB(x, y));
//                    }
//                }
                nativeImage.flush();
                finalFrame.getGraphics().dispose();
                finalFrame.flush();
                screenImage.getGraphics().dispose();
                screenImage.flush();
//                Image frame = Image.fromColors(colours);
                GifEncoder._frames.getAndUpdate((a) -> {
                    a.add(finalFrame);
                    return a;
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
            processedFrames.incrementAndGet();
        }, rendering);
    }

    //Minecraft's r and b channels work differently to the gif library...
    private static Color fromRgbMc(int rgb) {
        int redComponent = rgb >>> 16 & 0xFF;
        int greenComponent = rgb >>> 8 & 0xFF;
        int blueComponent = rgb & 0xFF;
        return new Color(redComponent / 255.0, greenComponent / 255.0, blueComponent / 255.0);
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
//            com.squareup.gifencoder.GifEncoder encoder = null;
//            ImageOptions imageOptions = new ImageOptions();
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
            if (GifEncoder._frames.get() != null) {
                List<BufferedImage> frames = GifEncoder._frames.get();
                BufferedImage firstFrame = frames.get(0);
                GifSequenceWriter writer = null;
                ImageOutputStream outputStream = null;
                int duration = (int) (GifEncoder.totalSeconds / frames.size());

                message = new TextComponentString("[BlockShot] Preparation complete, encoding frames... ");
                if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(message, BlockShot.CHAT_ENCODING_ID);
                }
                try {
                    outputStream = new FileImageOutputStream(new File(Minecraft.getMinecraft().mcDataDir + File.separator + "screenshots" + File.separator + "test.gif"));
                    writer = new GifSequenceWriter(outputStream, firstFrame.getType(), duration, true);
                    writer.writeToSequence(firstFrame);
//                    encoder = new com.squareup.gifencoder.GifEncoder(os, firstFrame.getWidth(), firstFrame.getHeight(), 0);
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
//                        imageOptions.setDelay(duration, TimeUnit.MILLISECONDS);
//                        encoder.addImage(frame, imageOptions);
                        writer.writeToSequence(frame);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    writer.close();
                    outputStream.close();
//                    encoder.finishEncoding();
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
//                    TODO
//                    byte[] bytes = outputStream.toByteArray();
//                    BlockShot.uploadAndAddToChat(bytes);
//                    os.close();
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
