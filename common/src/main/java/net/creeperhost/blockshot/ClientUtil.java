package net.creeperhost.blockshot;

import net.creeperhost.blockshot.mixin.MixinChatComponent;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Iterator;

/**
 * Created by brandon3055 on 17/03/2023
 */
public class ClientUtil {

    public static void sendMessage(Component component, MessageSignature messageSignature) {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().gui.getChat() == null) return;
        ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokeaddMessage(component, messageSignature, null);
    }

    public static void sendMessage(Component component) {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().gui.getChat() == null) return;
        Minecraft.getInstance().gui.getChat().addMessage(component);
    }

    public static void deleteBlockshotMessages(MessageSignature messageSignature) {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().gui.getChat() == null) return;
        Iterator<GuiMessage> iterator = ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).getAllMessages().iterator();

        while (iterator.hasNext()) {
            MessageSignature messageSignature2 = (iterator.next()).headerSignature();
            if (messageSignature2 != null && messageSignature2.equals(messageSignature)) {
                iterator.remove();
            }
        }

        ((MixinChatComponent) Minecraft.getInstance().gui.getChat()).invokerefreshTrimmedMessage();
    }

}
