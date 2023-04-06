package net.creeperhost.blockshot;

import me.shedaniel.architectury.platform.Platform;
import me.shedaniel.architectury.utils.Env;
import net.creeperhost.blockshot.gui.GuiEvents;
import net.creeperhost.blockshot.mixin.MixinMinecraft;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class BlockShot {
    public static final String MOD_ID = "blockshot";
    public static Logger LOGGER = LogManager.getLogger();
    public static Path configLocation = Platform.getGameFolder().resolve(MOD_ID + ".json");
    public static final int CHAT_UPLOAD_ID = 360360;
    public static final int CHAT_ENCODING_ID = 420420;
    public static byte[] latest;
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
