package net.creeperhost.blockshot;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.creeperhost.blockshot.gui.GuiEvents;
import net.creeperhost.blockshot.lib.MTSessionProvider;
import net.creeperhost.blockshot.mixin.MixinMinecraft;
import net.creeperhost.minetogether.session.JWebToken;
import net.creeperhost.minetogether.session.MineTogetherSession;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BlockShot {
    public static final String MOD_ID = "blockshot";
    public static final Logger LOGGER = LogManager.getLogger();
    public static Path configLocation = Platform.getGameFolder().resolve(MOD_ID + ".json");
    public static final MessageSignature CHAT_UPLOAD_ID = SignedMessageChain.Encoder.UNSIGNED.pack(new SignedMessageBody("CHAT_UPLOAD_ID-2634579823657932554", Instant.now(), 2634579823657932554L, LastSeenMessages.EMPTY));
    public static final MessageSignature CHAT_ENCODING_ID = SignedMessageChain.Encoder.UNSIGNED.pack(new SignedMessageBody("CHAT_ENCODING_ID-2634579823657932555", Instant.now(), 2634579823657932555L, LastSeenMessages.EMPTY));
    private static boolean active = false;
    private static CompletableFuture<@Nullable JWebToken> tokenFuture;

    public static void init() {
        if (Platform.getEnvironment().equals(Env.CLIENT)) {
            LOGGER.info("Init");
            Config.init(configLocation.toFile());
            MineTogetherSession.getDefault().setProvider(new MTSessionProvider());
            // Trigger session validation early in the background.
            tokenFuture = MineTogetherSession.getDefault().getTokenAsync();
            ClientLifecycleEvent.CLIENT_SETUP.register(instance -> clientStart());
        }
    }

    private static void clientStart() {
        try {
            JWebToken token = tokenFuture.get();
            if (token != null) {
                Auth.init(token);
                active = true;
            }
        } catch (InterruptedException | ExecutionException ignored) {}
        if (!active) {
            LOGGER.error("BlockShot will not run in offline mode.");
        }
        active |= Platform.isDevelopmentEnvironment();
        if (active) {
            GuiEvents.init();
        }
    }

    /**
     * Set on startup if in online mode.
     */
    public static boolean isActive() {
        return active;
    }

    public static int getFPS() {
        return ((MixinMinecraft) Minecraft.getInstance()).getfps();
    }
}
