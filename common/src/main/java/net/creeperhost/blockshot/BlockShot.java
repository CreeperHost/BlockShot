package net.creeperhost.blockshot;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.injectables.targets.ArchitecturyTarget;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.covers1624.quack.net.httpapi.HttpEngine;
import net.covers1624.quack.net.httpapi.java11.Java11HttpEngine;
import net.creeperhost.blockshot.gui.GuiEvents;
import net.creeperhost.blockshot.lib.MTSessionProvider;
import net.creeperhost.blockshot.mixin.MixinMinecraft;
import net.creeperhost.blockshot.polylib.ModPackInfo;
import net.creeperhost.minetogether.MineTogetherPlatform;
import net.creeperhost.minetogether.lib.MineTogetherLib;
import net.creeperhost.minetogether.lib.web.ApiClient;
import net.creeperhost.minetogether.lib.web.DynamicWebAuth;
import net.creeperhost.minetogether.session.JWebToken;
import net.creeperhost.minetogether.session.MineTogetherSession;
import net.creeperhost.minetogether.util.SignatureVerifier;
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
    public static Path configLocation = Platform.getGameFolder().resolve(MOD_ID + ".json");
    private static boolean active = false;
    private static CompletableFuture<@Nullable JWebToken> tokenFuture;
//    public static final String FINGERPRINT = SignatureVerifier.generateSignature();
    public static final DynamicWebAuth AUTH = new DynamicWebAuth();
    public static final HttpEngine WEB_ENGINE = Java11HttpEngine.create();
    public static final ApiClient API = ApiClient.builder()
            .httpEngine(WEB_ENGINE)
            .addUserAgentSegment("MineTogether-lib/" + MineTogetherLib.VERSION)
            .addUserAgentSegment("BlockShot-mod/" + Platform.getMod(MOD_ID).getVersion())
            .addUserAgentSegment("Minecraft/" + Platform.getMinecraftVersion())
            .addUserAgentSegment("Modloader/" + ArchitecturyTarget.getCurrentTarget())
            .webAuth(AUTH)
            .build();

    public static void init() {
        if (Platform.getEnvironment().equals(Env.CLIENT)) {
            LOGGER.info("Init");
//            AUTH.setHeader("Fingerprint", FINGERPRINT);
            Config.init(configLocation.toFile());
            ClientLifecycleEvent.CLIENT_SETUP.register(instance -> clientStart());
            ModPackInfo.init();
        }
    }

    private static void clientStart() {
        //Cant do this in init anymore because init now occurs before Minecraft.instance is initialised.
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
