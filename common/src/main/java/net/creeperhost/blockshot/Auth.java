package net.creeperhost.blockshot;

import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by brandon3055 on 23/03/2023
 */
public class Auth {

    private static final String CH_API = "https://api.creeper.host/";
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean verified = false;
    private static String serverId = null;
    private static String uuidHash = null;
    private static boolean hasPremium = false;

    public static void init() {
        checkMojangAuth();
        checkMTPremiumStatus();
    }

    public static boolean checkMojangAuth() {
        if (serverId == null) {
            doMojangAuth();
        } else {
            check();
        }
        return verified;
    }

    public static String checkAndGetServerID() {
        checkMojangAuth();
        return serverId;
    }

    private static void doMojangAuth() {
        verified = false;
        Minecraft mc = Minecraft.getInstance();
        serverId = DigestUtils.sha1Hex(String.valueOf(new Random().nextInt()));

        try {
            mc.getMinecraftSessionService().joinServer(mc.getUser().getGameProfile(), mc.getUser().getAccessToken(), serverId);
            verified = true;
            if (uuidHash == null) {
                GameProfile profile = mc.getUser().getGameProfile();
                UUID uuid = profile.getId();
                if (uuid != null){
                    uuidHash = Hashing.sha256().hashString(uuid.toString(), UTF_8).toString().toUpperCase(Locale.ROOT);
                }
            }
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

        doMojangAuth();
    }

    public static void checkMTPremiumStatus() {
        hasPremium = false;
        if (uuidHash == null) {
            return;
        }

        String profile = WebUtils.post(CH_API + "minetogether/profile", "{\"target\":\""+uuidHash+"\"}", WebUtils.MediaType.JSON, null);
        if ("error".equals(profile)) {
            return;
        }

        try {
            JsonObject response = JsonParser.parseString(profile).getAsJsonObject();
            if (!"success".equals(response.get("status").getAsString())) {
                return;
            }

            JsonObject data = response.getAsJsonObject("profileData").getAsJsonObject(uuidHash);
            hasPremium = data.get("premium").getAsBoolean();
        }catch (Throwable e) {
            LOGGER.error("An error occurred while retrieving MineTogether profile", e);
        }
    }

    public static boolean hasPremium() {
        return hasPremium;
    }
}
