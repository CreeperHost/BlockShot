package net.creeperhost.blockshot.lib;

import net.creeperhost.blockshot.ClientUtil;
import net.creeperhost.blockshot.mixin.MixinChatComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;


/**
 * Created by brandon3055 on 18/07/2024
 */
public class MessageHandlerImpl implements MessageHandler {

    @Override
    public void sendMessage(@Nullable Component component, MessageSignature messageSignature, boolean quietly) {
        Minecraft.getInstance().execute(() -> {
            if (!ClientUtil.validState()) return;
            deleteMessage(messageSignature);
            if (component == null) return;
            if (quietly) {
                addMessageQuietly(component, messageSignature, GuiMessageSource.SYSTEM_CLIENT, Minecraft.getInstance().gui.getGuiTicks(), null, false);
            } else {
                ClientUtil.getChat().addPlayerMessage(component, messageSignature, null);
            }
        });
    }

    @Override
    public void sendMessage(Component component, boolean quietly) {
        Minecraft.getInstance().execute(() -> {
            if (!ClientUtil.validState()) return;
            if (quietly) {
                addMessageQuietly(component, null, GuiMessageSource.SYSTEM_CLIENT, Minecraft.getInstance().gui.getGuiTicks(), null, false);
            } else {
                ClientUtil.getChat().addServerSystemMessage(component);
            }
        });
    }

    /**
     * Deletes the given message without spamming the console with the entire chat history.
     */
    private static void deleteMessage(MessageSignature messageSignature) {
        if (!ClientUtil.validState()) return;
        MixinChatComponent chat = (MixinChatComponent) ClientUtil.getChat();

        chat.getAllMessages().removeIf(e -> Objects.equals(e.signature(), messageSignature));

        //Refresh
        chat.getTrimmedMessages().clear();

        for (int i = chat.getAllMessages().size() - 1; i >= 0; --i) {
            GuiMessage guiMessage = chat.getAllMessages().get(i);
            addMessageQuietly(guiMessage.content(), guiMessage.signature(), guiMessage.source(), guiMessage.addedTime(), guiMessage.tag(), true);
        }
    }

    /**
     * Re-Implementation of ChatComponent's display queue update without console logging.
     */
    private static void addMessageQuietly(Component component, @Nullable MessageSignature messageSignature, GuiMessageSource source, int i, @Nullable GuiMessageTag guiMessageTag, boolean updateOnly) {
        ChatComponent chat = ClientUtil.getChat();
        MixinChatComponent chatMix = (MixinChatComponent) chat;

        int width = (int) Math.floor(chatMix.invokeGetWidth() / chatMix.invokeGetScale());
        GuiMessage message = new GuiMessage(i, component, messageSignature, source, guiMessageTag);
        List<FormattedCharSequence> list = message.splitLines(Minecraft.getInstance().font, width);
        boolean bl2 = chatMix.invokereisChatFocused();

        for (int k = 0; k < list.size(); ++k) {
            FormattedCharSequence formattedCharSequence = list.get(k);
            if (bl2 && chatMix.getChatScrollbarPos() > 0) {
                chatMix.setNewMessageSinceScroll(true);
                chat.scrollChat(1);
            }

            boolean bl3 = k == list.size() - 1;
            chatMix.getTrimmedMessages().add(0, new GuiMessage.Line(message, formattedCharSequence, bl3));
        }

        while (chatMix.getTrimmedMessages().size() > 100) {
            chatMix.getTrimmedMessages().remove(chatMix.getTrimmedMessages().size() - 1);
        }

        if (!updateOnly) {
            chatMix.getAllMessages().add(0, message);

            while (chatMix.getAllMessages().size() > 100) {
                chatMix.getAllMessages().remove(chatMix.getAllMessages().size() - 1);
            }
        }
    }

}
