package net.creeperhost.blockshot.lib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ModPackInfo {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String CH = "https://www.creeperhost.net/";
    private static VersionInfo info;

    public static synchronized VersionInfo getInfo() {
        if (info == null) {
            info = new VersionInfo().init();
        }
        return info;
    }

    public static class VersionInfo {
        public String curseID = "";
        public String websiteID = "";
        public String base64FTBID = "";
        public String ftbPackID = "";
        public String ftbUploadID = "";
        public String realName = "{\"p\":\"-1\"}";

        private VersionInfo init() {
            File gameDir = Minecraft.getMinecraft().mcDataDir;
            File configDir = new File(gameDir, "config");

            if (!applyManualOverride(gameDir)
                    && !readAuxiliumMetadata(configDir)
                    && !readVersionJson(new File(gameDir, "version.json"))
                    && !readNewFTB(new File(gameDir, "instance.json"))) {
                tryParseLauncherFiles(gameDir);
            }

            if (!curseID.isEmpty()) {
                fetchWebsiteIDCurse();
            }

            Map<String, String> json = new HashMap<String, String>();
            if (ftbPackID.isEmpty()) {
                json.put("p", NumberUtils.isParsable(curseID) ? curseID : "-1");
            } else {
                json.put("p", ftbPackID);
                if (!base64FTBID.isEmpty()) {
                    json.put("b", base64FTBID);
                }
            }
            realName = GSON.toJson(json);
            return this;
        }

        public String getUploadModpackPlatform() {
            if (NumberUtils.isParsable(ftbUploadID)) {
                return "FTB";
            }
            if (NumberUtils.isParsable(curseID)) {
                return "Curseforge";
            }
            return "";
        }

        public String getUploadModpackId() {
            if (NumberUtils.isParsable(ftbUploadID)) {
                return ftbUploadID;
            }
            if (NumberUtils.isParsable(curseID)) {
                return curseID;
            }
            return "";
        }

        private boolean applyManualOverride(File gameDir) {
            JsonObject config = readJson(new File(gameDir, "local/minetogether/minetogethercommunity.json"));
            if (config == null) {
                config = readJson(new File(gameDir, "local/minetogether/minetogether.json"));
            }
            if (config == null) {
                return false;
            }
            if (booleanValue(config, "connectPackBypass")) {
                return true;
            }

            String key = stringValue(config, "connectPackKey");
            if (isBlank(key)) {
                return false;
            }

            String type = stringValue(config, "connectPackProjectType").toLowerCase(Locale.ROOT);
            if ("ftb".equals(type) || !NumberUtils.isParsable(key)) {
                base64FTBID = key;
                String projectId = stringValue(config, "connectPackProjectId");
                if (NumberUtils.isParsable(projectId)) {
                    setFTBUploadID(projectId);
                } else {
                    resolveFTBUploadID(base64FTBID);
                }
            } else {
                curseID = key;
            }

            int chId = intValue(config, "connectPackCreeperHostVersionId");
            if (chId > 0) {
                websiteID = String.valueOf(chId);
            }
            return hasConnectPackKey();
        }

        private boolean readAuxiliumMetadata(File configDir) {
            JsonObject auxilium = readJson(new File(configDir, "metadata.json"));
            if (auxilium == null || !auxilium.has("id") || !auxilium.has("version")) {
                return false;
            }
            JsonObject version = auxilium.getAsJsonObject("version");
            if (version == null || !version.has("id")) {
                return false;
            }

            long packId = auxilium.get("id").getAsLong();
            long versionId = version.get("id").getAsLong();
            if (packId <= 0 || versionId <= 0) {
                return false;
            }

            setFTB(packId, versionId);
            return true;
        }

        private boolean readVersionJson(File path) {
            JsonObject json = readJson(path);
            if (json == null || !json.has("parent") || !json.has("id")) {
                return false;
            }

            String parent = json.get("parent").getAsString();
            String version = json.get("id").getAsString();
            if (!NumberUtils.isParsable(parent) || !NumberUtils.isParsable(version)) {
                return false;
            }

            setFTB(Long.parseLong(parent), Long.parseLong(version));
            return true;
        }

        private boolean readNewFTB(File path) {
            JsonObject json = readJson(path);
            if (json == null || !json.has("packType") || !json.has("id")) {
                return false;
            }

            int packType = json.get("packType").getAsInt();
            if (packType == 0 && json.has("versionId")) {
                long packId = json.get("id").getAsLong();
                long versionId = json.get("versionId").getAsLong();
                if (packId > 0 && versionId > 0) {
                    setFTB(packId, versionId);
                    return true;
                }
            }
            if (packType == 1) {
                curseID = json.get("id").getAsString();
                return true;
            }
            return false;
        }

        private void tryParseLauncherFiles(File gameDir) {
            JsonObject instance = readJson(new File(gameDir, "instance.json"));
            if (instance != null && instance.has("packType") && instance.has("id") && instance.get("packType").getAsInt() == 1) {
                curseID = instance.get("id").getAsString();
                return;
            }

            JsonObject minecraftInstance = readJson(new File(gameDir, "minecraftinstance.json"));
            if (minecraftInstance != null && minecraftInstance.has("projectID")) {
                curseID = minecraftInstance.get("projectID").getAsString();
                return;
            }

            readMultiMc(gameDir);
        }

        private void readMultiMc(File gameDir) {
            File parent = gameDir.getParentFile();
            if (parent == null) return;
            File prismCfg = new File(parent, "instance.cfg");
            if (!prismCfg.exists()) return;

            Map<String, String> values = new LinkedHashMap<String, String>();
            try (BufferedReader reader = new BufferedReader(new FileReader(prismCfg))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int equals = line.indexOf('=');
                    if (equals <= 0) continue;
                    values.put(line.substring(0, equals).trim(), line.substring(equals + 1).trim());
                }
            } catch (Throwable ex) {
                LOGGER.warn("Failed to load pack id from instance.cfg", ex);
                return;
            }

            String packType = strip(values.get("ManagedPackType")).toLowerCase(Locale.ROOT);
            String packId = strip(values.get("ManagedPackID"));
            String versionId = strip(values.get("ManagedPackVersionID"));

            if ((packType.isEmpty() || packId.isEmpty()) && !isBlank(values.get("iconKey"))) {
                String[] split = values.get("iconKey").split("_");
                if (split.length >= 2) {
                    packType = strip(split[0]).toLowerCase(Locale.ROOT);
                    packId = strip(split[1]);
                }
            }

            if (packType.isEmpty() || !NumberUtils.isParsable(packId)) return;
            if ("flame".equals(packType) || "curseforge".equals(packType) || "curse".equals(packType)) {
                curseID = packId;
                return;
            }
            if ("ftb".equals(packType)) {
                ftbPackID = "m" + packId;
                ftbUploadID = packId;
                if (NumberUtils.isParsable(versionId)) {
                    base64FTBID = encodeFTB(packId, versionId);
                    fetchWebsiteIDFTB();
                }
            }
        }

        public boolean hasConnectPackKey() {
            return !base64FTBID.isEmpty() || !curseID.isEmpty();
        }

        private void setFTB(long packId, long versionId) {
            ftbPackID = "m" + packId;
            ftbUploadID = String.valueOf(packId);
            base64FTBID = encodeFTB(packId, versionId);
            fetchWebsiteIDFTB();
        }

        private void setFTBUploadID(String packId) {
            ftbUploadID = packId;
            ftbPackID = "m" + packId;
        }

        private boolean fetchWebsiteIDCurse() {
            try {
                if (!NumberUtils.isParsable(curseID)) return false;
                JsonObject response = getJson(CH + "json/modpacks/curseforge/" + encodePath(curseID));
                String resolvedID = stringValue(response, "id");
                if (resolvedID.isEmpty()) return false;
                websiteID = resolvedID;
                return true;
            } catch (Throwable ex) {
                LOGGER.warn("Failed to resolve CurseForge pack id {}", curseID, ex);
            }
            return false;
        }

        private boolean fetchWebsiteIDFTB() {
            if (base64FTBID.isEmpty()) return false;
            try {
                JsonObject response = getJson(CH + "json/modpacks/modpacksch/" + encodePath(base64FTBID));
                String resolvedID = stringValue(response, "id");
                if (!resolvedID.isEmpty()) {
                    websiteID = resolvedID;
                }
                ModpackResolution resolution = resolutionFromMeta(response, base64FTBID);
                if (resolution == null && !resolvedID.isEmpty()) {
                    JsonObject detail = getJson(CH + "json/modpacks/name/" + encodePath(resolvedID) + "?shape=humanVersion");
                    resolution = resolutionFromMeta(detail, base64FTBID);
                }
                if (resolution != null) {
                    applyResolution(base64FTBID, resolution);
                }
                return !websiteID.isEmpty();
            } catch (Throwable ex) {
                LOGGER.warn("Failed to resolve FTB pack id {}", base64FTBID, ex);
            }
            return false;
        }

        private boolean resolveFTBUploadID(String base64Key) {
            if (isBlank(base64Key)) return false;
            ModpackResolution cached = cachedResolution(base64Key);
            if (cached != null) {
                applyResolution(base64Key, cached);
                return true;
            }
            return fetchWebsiteIDFTB();
        }

        private void applyResolution(String base64Key, ModpackResolution resolution) {
            if (!"FTB".equals(resolution.platform) || !NumberUtils.isParsable(resolution.projectId)) return;
            ftbUploadID = resolution.projectId;
            if (ftbPackID.isEmpty()) {
                ftbPackID = "m" + resolution.projectId;
            }
            cacheResolution(base64Key, resolution);
        }

        private ModpackResolution cachedResolution(String base64Key) {
            if (Config.INSTANCE == null || Config.INSTANCE.modpackResolverCache == null) return null;
            Config.ModpackResolverCacheEntry cached = Config.INSTANCE.modpackResolverCache.get(base64Key);
            if (cached == null || !"FTB".equals(cached.platform) || !NumberUtils.isParsable(cached.projectId)) {
                return null;
            }
            return new ModpackResolution(cached.platform, cached.projectId, cached.projectVersion, cached.minecraftVersion, cached.displayName);
        }

        private void cacheResolution(String base64Key, ModpackResolution resolution) {
            if (Config.INSTANCE == null) return;
            Config.validate();
            Config.ModpackResolverCacheEntry entry = new Config.ModpackResolverCacheEntry();
            entry.platform = resolution.platform;
            entry.projectId = resolution.projectId;
            entry.projectVersion = resolution.projectVersion;
            entry.minecraftVersion = resolution.minecraftVersion;
            entry.displayName = resolution.displayName;
            entry.timestamp = System.currentTimeMillis();
            Config.INSTANCE.modpackResolverCache.put(base64Key, entry);
            if (BlockShot.configLocation != null) {
                Config.saveConfigToFile(BlockShot.configLocation.toFile());
            }
        }

        private JsonObject readJson(File file) {
            if (!file.exists()) {
                return null;
            }

            try (FileReader reader = new FileReader(file)) {
                return new JsonParser().parse(reader).getAsJsonObject();
            } catch (Throwable ex) {
                LOGGER.warn("Failed to load pack metadata from {}", file.getName(), ex);
                return null;
            }
        }
    }

    private static ModpackResolution resolutionFromMeta(JsonObject root, String base64Key) {
        if (root == null || !root.has("meta") || !root.get("meta").isJsonObject()) return null;
        JsonObject meta = root.getAsJsonObject("meta");
        String type = stringValue(meta, "projectType").toLowerCase(Locale.ROOT);
        String projectId = stringValue(meta, "projectId");
        if (!"ftb".equals(type) || !NumberUtils.isParsable(projectId)) return null;
        String displayName = displayName(root);
        return new ModpackResolution(
                "FTB",
                projectId,
                stringValue(meta, "projectVersion"),
                stringValue(meta, "versionFor"),
                displayName.isEmpty() ? base64Key : displayName
        );
    }

    private static String displayName(JsonObject root) {
        if (root == null) return "";
        if (root.has("pack") && root.get("pack").isJsonObject()) {
            JsonObject pack = root.getAsJsonObject("pack");
            String name = stringValue(pack, "displayName");
            if (!name.isEmpty()) return name;
        }
        return stringValue(root, "name");
    }

    private static JsonObject getJson(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "BlockShot/1.5");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            JsonElement element = new JsonParser().parse(reader);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        } finally {
            connection.disconnect();
        }
    }

    private static String stringValue(JsonObject object, String key) {
        if (object == null || !object.has(key)) return "";
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return "";
        try {
            return element.getAsString();
        } catch (UnsupportedOperationException ex) {
            return "";
        }
    }

    private static int intValue(JsonObject object, String key) {
        String value = stringValue(object, key);
        if (!NumberUtils.isParsable(value)) return -1;
        return Integer.parseInt(value);
    }

    private static boolean booleanValue(JsonObject object, String key) {
        if (object == null || !object.has(key)) return false;
        try {
            return object.get(key).getAsBoolean();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String encodeFTB(Object packId, Object versionId) {
        return Base64.getEncoder().encodeToString((String.valueOf(packId) + versionId).getBytes(StandardCharsets.UTF_8));
    }

    private static String encodePath(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
    }

    private static String strip(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return strip(value).isEmpty();
    }

    private static class ModpackResolution {
        final String platform;
        final String projectId;
        final String projectVersion;
        final String minecraftVersion;
        final String displayName;

        ModpackResolution(String platform, String projectId, String projectVersion, String minecraftVersion, String displayName) {
            this.platform = platform == null ? "" : platform;
            this.projectId = projectId == null ? "" : projectId;
            this.projectVersion = projectVersion == null ? "" : projectVersion;
            this.minecraftVersion = minecraftVersion == null ? "" : minecraftVersion;
            this.displayName = displayName == null ? "" : displayName;
        }
    }
}
