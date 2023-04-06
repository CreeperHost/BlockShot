package net.creeperhost.blockshot;

import net.creeperhost.blockshot.mixin.MixinChatComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

/**
 * Created by brandon3055 on 17/03/2023
 */
public class ClientUtil {

    public static void sendMessage(Component component, int messageId) { sendMessage(component, messageId, false); }

    public static void sendMessage(Component component, int messageId, boolean quietly) {
        if (!validState()) return;
        ((MixinChatComponent)getChat()).invokeaddMessage(component, messageId);
    }

    public static void sendMessage(Component component) { sendMessage(component, false); }

    public static void sendMessage(Component component, boolean quietly) {
        if (!validState()) return;
        getChat().addMessage(component);
    }

    private static ChatComponent getChat() {
        return Minecraft.getInstance().gui.getChat();
    }

    public static boolean validState() {
        return Minecraft.getInstance() != null && getChat() != null;
    }
}
