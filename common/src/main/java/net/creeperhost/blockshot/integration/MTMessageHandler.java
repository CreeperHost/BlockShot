package net.creeperhost.blockshot.integration;

import net.creeperhost.blockshot.ClientUtil;
import net.creeperhost.blockshot.lib.MessageHandler;
import net.creeperhost.minetogether.chat.FriendChatNotifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by brandon3055 on 18/07/2024
 */
public class MTMessageHandler implements MessageHandler {
    private static Random random = new Random();

    @Override
    public void sendMessage(Component component, MessageSignature messageSignature, boolean quietly) {
        if (!ClientUtil.validState()) return;
        FriendChatNotifier.addNotificationMessage(component, messageSignature);
    }

    @Override
    public void sendMessage(Component component, boolean quietly) {
        if (!ClientUtil.validState()) return;
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        for (int i = 0; i < 4; i++) byteBuffer.putLong(random.nextLong());
        FriendChatNotifier.addNotificationMessage(component, new MessageSignature(byteBuffer.array()));
    }
}
