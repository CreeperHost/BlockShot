package net.creeperhost.blockshot;

import net.covers1624.quack.net.httpapi.HttpEngine;
import net.covers1624.quack.net.httpapi.java11.Java11HttpEngine;
import net.creeperhost.blockshot.gui.GuiEvents;
import net.creeperhost.blockshot.lib.MTSessionProvider;
import net.creeperhost.blockshot.mixin.MixinMinecraft;
import net.creeperhost.blockshot.polylib.ModPackInfo;
import net.creeperhost.minetogether.lib.MineTogetherLib;
import net.creeperhost.minetogether.lib.web.ApiClient;
import net.creeperhost.minetogether.lib.web.DynamicWebAuth;
import net.creeperhost.minetogether.session.JWebToken;
import net.creeperhost.minetogether.session.MineTogetherSession;
import net.creeperhost.polylib.event.events.client.PolyClientLifecycleEvents;
import net.creeperhost.polylib.platform.Services;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BlockShot {
    public static final String MOD_ID = "blockshot";
    public static final Logger LOGGER = LogManager.getLogger();
    public static Path configLocation;
    private static boolean active = false;
    private static CompletableFuture<@Nullable JWebToken> tokenFuture;
//    public static final String FINGERPRINT = SignatureVerifier.generateSignature();
    public static final DynamicWebAuth AUTH = new DynamicWebAuth();
    private static ApiClient api;

    public static ApiClient api() {
        if (api == null) {
            HttpEngine webEngine = Java11HttpEngine.create();
            api = ApiClient.builder()
                    .httpEngine(webEngine)
                    .addUserAgentSegment("MineTogether-lib/" + MineTogetherLib.VERSION)
                    .addUserAgentSegment("BlockShot-mod/" + "123.45") // TODO: Fix to Blockshot version
                    .addUserAgentSegment("Minecraft/" + SharedConstants.getCurrentVersion().name())
                    .addUserAgentSegment("Modloader/" + Services.PLATFORM.getPlatformName())
                    .webAuth(AUTH)
                    .build();
        }
        return api;
    }

    public static void init() {
        if (Services.PLATFORM.isClient()) {
            LOGGER.info("Init");
//            AUTH.setHeader("Fingerprint", FINGERPRINT);
            configLocation = Services.PLATFORM.getConfigFolder().resolve(MOD_ID + ".json");
            Config.init(configLocation.toFile());
            PolyClientLifecycleEvents.CLIENT_STARTED.register(instance -> clientStart());
        }
    }

    private static void clientStart() {
        //Cant do this in init anymore because init now occurs before Minecraft.instance is initialised.
        ModPackInfo.init();
        MineTogetherSession.getDefault().setProvider(new MTSessionProvider());
        tokenFuture = MineTogetherSession.getDefault().getTokenAsync();
        try {
            JWebToken token = tokenFuture.get();
            if (token != null) {
                Auth.init(token);
                BlockShotClient.init();
                active = true;
            }
        } catch (InterruptedException | ExecutionException ignored) {}
        if (!active) {
            LOGGER.error("BlockShot will not run in offline mode.");
        }
        active |= Services.PLATFORM.isDevelopmentEnvironment();
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

    public static Path gameFolder() {
        return Minecraft.getInstance().gameDirectory.toPath();
    }
}
