package net.creeperhost.blockshot.mixin;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface MixinChatComponent {
    @Invoker("isChatFocused")
    boolean invokereisChatFocused();

    @Accessor("allMessages")
    List<GuiMessage> getAllMessages();

    @Accessor("trimmedMessages")
    List<GuiMessage.Line> getTrimmedMessages();

    @Accessor("chatScrollbarPos")
    int getChatScrollbarPos();

    @Accessor("newMessageSinceScroll")
    void setNewMessageSinceScroll(boolean value);
}
