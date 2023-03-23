package net.creeperhost.blockshot;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * Created by brandon3055 on 23/03/2023
 */
public class Auth {
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean verified = false;
    private static String serverId = null;

    public static boolean checkVerification() {
        if (serverId == null) {
            join();
        } else {
            check();
        }
        return verified;
    }

    public static String checkAndGetServerID() {
        checkVerification();
        return serverId;
    }

    private static void join() {
        verified = false;
        Minecraft mc = Minecraft.getInstance();
        serverId = DigestUtils.sha1Hex(String.valueOf(new Random().nextInt()));

        try {
            mc.getMinecraftSessionService().joinServer(mc.getUser().getGameProfile(), mc.getUser().getAccessToken(), serverId);
            verified = true;
        } catch (Throwable e) {
            LOGGER.error("Failed to validate with Mojang", e);
        }
    }

    private static void check() {
        verified = false;
        Minecraft mc = Minecraft.getInstance();

        try {
            GameProfile profile = mc.getMinecraftSessionService().hasJoinedServer(mc.getUser().getGameProfile(), serverId, null);
            if (profile != null) {
                verified = true;
                return;
            }
        } catch (Throwable ignored) {}

        join();
    }
}
