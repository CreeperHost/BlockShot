package net.creeperhost.blockshot;

import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.architectury.platform.Platform;
import net.creeperhost.minetogether.session.MineTogetherSession;
import net.creeperhost.minetogether.session.SessionValidationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.core.UUIDUtil;
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

    private static User user;
    private static GameProfile profile;
    private static String uuidHash;

    private static boolean chAuthenticated = false;
    private static boolean hasPremium = false;

    public static MineTogetherSession mtSession;

    public static void init() {
        Minecraft mc = Minecraft.getInstance();
        user = mc.getUser();
        profile = user.getGameProfile();

        UUID playerUUID = UUIDUtil.getOrCreatePlayerUUID(profile);
        String playerName = profile.getName();
        uuidHash = Hashing.sha256().hashString(playerUUID.toString(), UTF_8).toString().toUpperCase(Locale.ROOT);

        mtSession = new MineTogetherSession(Platform.getGameFolder(), playerUUID, playerName, Auth::doMojangAuth);

        try {
            mtSession.validate();
            chAuthenticated = true;
            checkMTPremiumStatus();
        } catch (SessionValidationException e) {
            LOGGER.error("Failed to validate session", e);
        }
    }

    public static String doMojangAuth() {
        String mojangServerId = DigestUtils.sha1Hex(String.valueOf(new Random().nextInt()));
        try {
            Minecraft.getInstance().getMinecraftSessionService().joinServer(profile, user.getAccessToken(), mojangServerId);
            return mojangServerId;
        } catch (Throwable e) {
            LOGGER.error("Failed to validate with Mojang", e);
        }
        return "";
    }

    public static boolean hasCreeperHostAuth() {
        return chAuthenticated;
    }

    private static void checkMTPremiumStatus() {
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
