package net.creeperhost.blockshot.lib;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

/**
 * Created by brandon3055 on 18/07/2024
 */
public interface MessageHandler {

    default void sendMessage(Component component, MessageSignature messageSignature) {
        sendMessage(component, messageSignature, false);
    }

    void sendMessage(Component component, MessageSignature messageSignature, boolean quietly);

    default void sendMessage(Component component) {
        sendMessage(component, false);
    }

    void sendMessage(Component component, boolean quietly);
}
