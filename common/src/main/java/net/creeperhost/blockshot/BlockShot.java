package net.creeperhost.blockshot;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.creeperhost.blockshot.gui.GuiEvents;
import net.creeperhost.blockshot.mixin.MixinMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.time.Instant;

public class BlockShot {
    public static final String MOD_ID = "blockshot";
    public static final Logger LOGGER = LogManager.getLogger();
    public static Path configLocation = Platform.getGameFolder().resolve(MOD_ID + ".json");
    public static final MessageSignature CHAT_UPLOAD_ID = SignedMessageChain.Encoder.UNSIGNED.pack(new SignedMessageBody("CHAT_UPLOAD_ID-2634579823657932554", Instant.now(), 2634579823657932554L, LastSeenMessages.EMPTY));
    public static final MessageSignature CHAT_ENCODING_ID = SignedMessageChain.Encoder.UNSIGNED.pack(new SignedMessageBody("CHAT_ENCODING_ID-2634579823657932555", Instant.now(), 2634579823657932555L, LastSeenMessages.EMPTY));
    private static boolean _active = false;

    public static void init() {
        if (Platform.getEnvironment().equals(Env.CLIENT)) {
            Config.init(configLocation.toFile());
            Auth.init();
            if (Auth.hasCreeperHostAuth() || Platform.isDevelopmentEnvironment()) {
                _active = true;
            } else {
                LOGGER.error("BlockShot will not run in offline mode.");
            }
        }
        if (BlockShot.isActive()) {
            GuiEvents.init();
        }
    }

    /**
     * Set on startup if in online mode.
     */
    public static boolean isActive() {
        return _active;
    }

    public static int getFPS() {
        return ((MixinMinecraft) Minecraft.getInstance()).getfps();
    }
}
