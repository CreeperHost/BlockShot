package net.creeperhost.blockshot.integration;

import net.creeperhost.blockshot.ClientUtil;
import net.creeperhost.blockshot.lib.MessageHandler;
import net.creeperhost.blockshot.lib.MessageHandlerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by brandon3055 on 18/07/2024
 */
public class MTMessageHandler implements MessageHandler {
    private final MessageHandler fallback = new MessageHandlerImpl();
    private static Method notifierMethod;
    private static boolean notifierUnavailable;

    @Override
    public void sendMessage(Component component, MessageSignature messageSignature, boolean quietly) {
        fallback.sendMessage(component, messageSignature, quietly);
        notifyMineTogether(component, messageSignature);
    }

    @Override
    public void sendMessage(Component component, boolean quietly) {
        MessageSignature signature = ClientUtil.uuidToSig(UUID.randomUUID());
        fallback.sendMessage(component, signature, quietly);
        notifyMineTogether(component, signature);
    }

    private static void notifyMineTogether(Component component, MessageSignature signature) {
        if (notifierUnavailable) return;

        Minecraft.getInstance().execute(() -> {
            if (!ClientUtil.validState()) return;

            try {
                Method method = notifierMethod;
                if (method == null) {
                    Class<?> notifier = Class.forName("net.creeperhost.minetogether.chat.FriendChatNotifier");
                    method = notifier.getMethod("addNotificationMessage", Component.class, MessageSignature.class);
                    notifierMethod = method;
                }
                method.invoke(null, component, signature);
            } catch (ReflectiveOperationException | LinkageError e) {
                notifierUnavailable = true;
            }
        });
    }
}
