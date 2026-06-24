package net.creeperhost.blockshot;

import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.integration.MTMessageHandler;
import net.creeperhost.blockshot.lib.MessageHandler;
import net.creeperhost.blockshot.lib.MessageHandlerImpl;
import net.creeperhost.polylib.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.MessageSignature;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Created by brandon3055 on 17/03/2023
 */
public class ClientUtil {
    public static final MessageSignature CHAT_UPLOAD = uuidToSig(UUID.fromString("8270052c-7d04-4331-81c5-0584c9789c47"));
//    public static final MessageSignature CHAT_UPLOAD_2 = uuidToSig(UUID.fromString("ebb13dda-ad94-4070-8841-512b9b99d733"));
    public static final MessageSignature CHAT_ENCODING_ID = uuidToSig(UUID.fromString("ef622f0a-dab2-458b-b2cc-d312a533ebcf"));

    private static MessageHandler MESSAGE_HANDLER = new MessageHandlerImpl();

    public static void init() {
        loadMTIntegration(() -> () -> MESSAGE_HANDLER = new MTMessageHandler());
    }

    private static void loadMTIntegration(Supplier<Runnable> runnable) {
        if (Services.PLATFORM.isModLoaded("minetogether")) {
            runnable.get().run();
        }
    }

    public static MessageHandler getMessageHandler() {
        return MESSAGE_HANDLER;
    }

    public static ChatComponent getChat() {
        return Minecraft.getInstance().gui.getChat();
    }

    public static boolean validState() {
        return Minecraft.getInstance() != null && getChat() != null;
    }

    public static MessageSignature uuidToSig(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        byteBuffer.position(32);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return new MessageSignature(byteBuffer.array());
    }

    public static byte[] nativeImageBytes(NativeImage nativeImage) throws IOException {
        Path tempFile = Files.createTempFile("blockshot-", ".png");
        try {
            nativeImage.writeToFile(tempFile);
            return Files.readAllBytes(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public static NativeImage bufferedImageToNativeImage(BufferedImage bufferedImage)
    {
        try (NativeImage nativeImage = new NativeImage(bufferedImage.getWidth(), bufferedImage.getHeight(), true)) {
            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    int arbg = bufferedImage.getRGB(x, y);
                    int a = (arbg >> 24) & 0xFF;
                    int r = (arbg >> 16) & 0xFF;
                    int g = (arbg >> 8) & 0xFF;
                    int b = arbg & 0xFF;

                    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                    nativeImage.setPixelABGR(x, y, abgr);
                }
            }
            return nativeImage;
        } catch (Exception e) {
            return null;
        }
    }
}
