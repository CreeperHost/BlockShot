package net.creeperhost.blockshot;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.creeperhost.blockshot.capture.Encoder;
import net.creeperhost.blockshot.capture.VideoEncoder;
import net.creeperhost.blockshot.gui.GuiEvents;
import net.creeperhost.blockshot.mixin.MixinMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MessageSignature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class BlockShot {
    public static final String MOD_ID = "blockshot";
    public static final Logger LOGGER = LogManager.getLogger();
    public static Path configLocation = Platform.getGameFolder().resolve(MOD_ID + ".json");
    public static final MessageSignature CHAT_UPLOAD_ID = new MessageSignature(new byte[]{36, 03, 60});
    public static final MessageSignature CHAT_ENCODING_ID = new MessageSignature(new byte[]{42, 04, 20});
    private static boolean _active = false;

    public static void init() {
        if (Platform.getEnvironment().equals(Env.CLIENT)) {
            Config.init(configLocation.toFile());
            Auth.init();
            if (Auth.checkMojangAuth() || Platform.isDevelopmentEnvironment()) {
                _active = true;
            } else {
                LOGGER.error("BlockShot will not run in offline mode.");
            }
        }
        if (BlockShot.isActive()) {
            GuiEvents.init();
            Keybindings.init();
        }
    }

    /**
     * Set on startup if in online mode.
     * */
    public static boolean isActive() {
        return _active;
    }

    public static int getFPS() {
        return ((MixinMinecraft) Minecraft.getInstance()).getfps();
    }
}
