package net.creeperhost.blockshot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.creeperhost.minetogether.session.JWebToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by brandon3055 on 23/03/2023
 */
public class Auth {

    private static final String CH_API = "https://api.creeper.host/";
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean hasPremium = false;

    public static void init(JWebToken auth) {
        String profile = WebUtils.post(CH_API + "minetogether/profile", "{\"target\":\"" + auth.getUuidHash() + "\"}", WebUtils.MediaType.JSON, null);
        if ("error".equals(profile)) {
            return;
        }

        try {
            JsonObject response = JsonParser.parseString(profile).getAsJsonObject();
            if (!"success".equals(response.get("status").getAsString())) {
                return;
            }

            JsonObject data = response.getAsJsonObject("profileData").getAsJsonObject(auth.getUuidHash());
            hasPremium = data.get("premium").getAsBoolean();
        } catch (Throwable e) {
            LOGGER.error("An error occurred while retrieving MineTogether profile", e);
        }
    }

    public static boolean hasPremium() {
        return hasPremium;
    }
}
