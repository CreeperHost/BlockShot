package net.creeperhost.blockshot;

import net.creeperhost.blockshot.mixin.MixinChatComponent;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Created by brandon3055 on 17/03/2023
 */
public class ClientUtil {

    public static void sendMessage(Component component, MessageSignature messageSignature) { sendMessage(component, messageSignature, false); }

    public static void sendMessage(Component component, MessageSignature messageSignature, boolean quietly) {
        if (!validState()) return;
        deleteMessage(messageSignature);
        if (quietly) {
            addMessageQuietly(component, messageSignature, Minecraft.getInstance().gui.getGuiTicks(), null, false);
        } else {
            getChat().addMessage(component, messageSignature, null);
        }
    }

    public static void sendMessage(Component component) { sendMessage(component, false); }

    public static void sendMessage(Component component, boolean quietly) {
        if (!validState()) return;
        if (quietly) {
            addMessageQuietly(component, null, Minecraft.getInstance().gui.getGuiTicks(), null, false);
        } else {
            getChat().addMessage(component);
        }
    }

    private static ChatComponent getChat() {
        return Minecraft.getInstance().gui.getChat();
    }

    public static boolean validState() {
        return Minecraft.getInstance() != null && getChat() != null;
    }

    /**
     * Deletes the given message without spamming the console with the entire chat history.
     */
    public static void deleteMessage(MessageSignature messageSignature) {
        if (!validState()) return;
        MixinChatComponent chat = (MixinChatComponent) getChat();

        chat.getAllMessages().removeIf(e -> Objects.equals(e.headerSignature(), messageSignature));

        //Refresh
        chat.getTrimmedMessages().clear();

        for (int i = chat.getAllMessages().size() - 1; i >= 0; --i) {
            GuiMessage guiMessage = chat.getAllMessages().get(i);
            addMessageQuietly(guiMessage.content(), guiMessage.headerSignature(), guiMessage.addedTime(), guiMessage.tag(), true);
        }
    }

    /**
     * Re-Implementation of {@link ChatComponent#addMessage(Component, MessageSignature, int, GuiMessageTag, boolean)} but without console logging.
     */
    public static void addMessageQuietly(Component component, @Nullable MessageSignature messageSignature, int i, @Nullable GuiMessageTag guiMessageTag, boolean updateOnly) {
        ChatComponent chat = getChat();
        MixinChatComponent chatMix = (MixinChatComponent) chat;

        int j = Mth.floor((double) chat.getWidth() / chat.getScale());
        if (guiMessageTag != null && guiMessageTag.icon() != null) {
            j -= guiMessageTag.icon().width + 4 + 2;
        }

        List<FormattedCharSequence> list = ComponentRenderUtils.wrapComponents(component, j, Minecraft.getInstance().font);
        boolean bl2 = chatMix.invokereisChatFocused();

        for (int k = 0; k < list.size(); ++k) {
            FormattedCharSequence formattedCharSequence = list.get(k);
            if (bl2 && chatMix.getChatScrollbarPos() > 0) {
                chatMix.setNewMessageSinceScroll(true);
                chat.scrollChat(1);
            }

            boolean bl3 = k == list.size() - 1;
            chatMix.getTrimmedMessages().add(0, new GuiMessage.Line(i, formattedCharSequence, guiMessageTag, bl3));
        }

        while (chatMix.getTrimmedMessages().size() > 100) {
            chatMix.getTrimmedMessages().remove(chatMix.getTrimmedMessages().size() - 1);
        }

        if (!updateOnly) {
            chatMix.getAllMessages().add(0, new GuiMessage(i, component, messageSignature, guiMessageTag));

            while (chatMix.getAllMessages().size() > 100) {
                chatMix.getAllMessages().remove(chatMix.getAllMessages().size() - 1);
            }
        }
    }
}
