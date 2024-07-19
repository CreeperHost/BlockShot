package net.creeperhost.blockshot;

import dev.architectury.platform.Platform;
import net.creeperhost.blockshot.integration.MTMessageHandler;
import net.creeperhost.blockshot.lib.MessageHandler;
import net.creeperhost.blockshot.lib.MessageHandlerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.MessageSignature;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Created by brandon3055 on 17/03/2023
 */
public class ClientUtil {
    public static final MessageSignature CHAT_UPLOAD = uuidToSig(UUID.fromString("8270052c-7d04-4331-81c5-0584c9789c47"));
    public static final MessageSignature CHAT_ENCODING_ID = uuidToSig(UUID.fromString("ef622f0a-dab2-458b-b2cc-d312a533ebcf"));

    private static MessageHandler MESSAGE_HANDLER = new MessageHandlerImpl();

    public static void init() {
        loadMTIntegration(() -> () -> MESSAGE_HANDLER = new MTMessageHandler());
    }

    private static void loadMTIntegration(Supplier<Runnable> runnable) {
        if (Platform.isModLoaded("minetogether")) {
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
}
