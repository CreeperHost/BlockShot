package net.creeperhost.blockshot;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by brandon3055 on 23/03/2023
 */
public class Auth {

    private static final Gson GSON = new Gson();
    private static final String CH_API = "https://api.creeper.host/";
    private static final String AUTH_API = "https://connect.syd.minetogether.ch.tools:443/auth";
    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean mojangAuthenticated = false;
    private static String mojangServerId = null;
    private static UUID playerUUID;
    private static String uuidHash = null;

    private static boolean chAuthenticated = false;
    private static String chAuth = null;
    private static boolean hasPremium = false;

    public static void init() {
        try {
            doCreeperHostAuth();
            checkMTPremiumStatus();
        } catch (Throwable ignored) {
            chAuthenticated = mojangAuthenticated = false;
        }
    }

    public static boolean doMojangAuth() {
        mojangAuthenticated = false;
        Minecraft mc = Minecraft.getInstance();
        mojangServerId = DigestUtils.sha1Hex(String.valueOf(new Random().nextInt()));

        try {
            mc.getMinecraftSessionService().joinServer(mc.getUser().getGameProfile(), mc.getUser().getAccessToken(), mojangServerId);
            if (uuidHash == null) {
                playerUUID = UUIDUtil.getOrCreatePlayerUUID(mc.getUser().getGameProfile());
                uuidHash = Hashing.sha256().hashString(playerUUID.toString(), UTF_8).toString().toUpperCase(Locale.ROOT);
            }

            mojangAuthenticated = playerUUID.version() == 4;
        } catch (Throwable e) {
            LOGGER.error("Failed to validate with Mojang", e);
        }
        return mojangAuthenticated;
    }

    public static String getMojangServerId() {
        if (doMojangAuth()) {
            return mojangServerId;
        }
        return "";
    }

    private static void doCreeperHostAuth() {
        chAuthenticated = false;
        if (!doMojangAuth()) {
            return;
        }

        HashMap<String, String> bodyArr = new HashMap<>();
        bodyArr.put("auth", playerUUID + ":" + mojangServerId);
        bodyArr.put("type", "minecraft");
        String bodyString = GSON.toJson(bodyArr);
        HttpPut httpput = new HttpPut(AUTH_API);
        httpput.setEntity(new ByteArrayEntity(bodyString.getBytes()));

        String response = WebUtils.executeWebRequest(httpput, WebUtils.MediaType.JSON, null, false);
        if ("error".equals(response)) {
            return;
        }

        JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
        if (obj.has("success") && obj.get("success").getAsBoolean() && obj.has("authSecret")) {
            chAuth = obj.get("authSecret").getAsString();
            chAuthenticated = true;
        }
    }

    public static boolean hasCreeperHostAuth() {
        return chAuthenticated;
    }

    public static String getCreeperHostAuth() {
        return chAuth;
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
