package net.creeperhost.blockshot.mixin;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface MixinChatComponent {
    @Invoker("addMessage")
    void invokeaddMessage(Component component, MessageSignature messageSignature, @Nullable GuiMessageTag guiMessageTag);

    @Invoker("refreshTrimmedMessage")
    void invokerefreshTrimmedMessage();

    @Accessor("allMessages")
    List<GuiMessage> getAllMessages();
}
