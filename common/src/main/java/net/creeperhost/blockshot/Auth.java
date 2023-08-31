package net.creeperhost.blockshot;

import com.google.gson.Gson;
import net.creeperhost.minetogether.session.JWebToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Created by brandon3055 on 23/03/2023
 */
public class Auth {

    private static final Gson GSON = new Gson();
    private static final String CH_API = "https://api.creeper.host/";
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean hasPremium = false;

    public static void init(JWebToken auth) {
        String profile = WebUtils.post(CH_API + "minetogether/profile", "{\"target\":\"" + auth.getUuidHash() + "\"}", WebUtils.MediaType.JSON, null);
        if ("error".equals(profile)) {
            return;
        }

        try {
            ProfileResponse response = GSON.fromJson(profile, ProfileResponse.class);
            if (!"success".equals(response.status)) {
                return;
            }

            ProfileResponse.Data data = response.profileData.get(auth.getUuidHash());
            hasPremium = data != null && data.premium;
        }catch (Throwable e) {
            LOGGER.error("An error occurred while retrieving MineTogether profile", e);
        }
    }

    public static boolean hasPremium() {
        return hasPremium;
    }

    private static class ProfileResponse {
        public String status;
        public Map<String, Data> profileData;
        public static class Data {
            boolean premium;
        }
    }
}
