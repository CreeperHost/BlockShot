package net.creeperhost.blockshot.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatComponent.class)
public interface MixinChatComponent {
    @Invoker("addMessage")
    void invokeaddMessage(Component component, int messageId);

    @Invoker("removeById")
    void invokeremoveById(int messageId);
}
