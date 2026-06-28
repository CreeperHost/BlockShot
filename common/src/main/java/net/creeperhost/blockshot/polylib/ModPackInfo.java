package net.creeperhost.blockshot.polylib;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.covers1624.quack.gson.JsonUtils;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.creeperhost.polylib.blue.endless.jankson.Jankson;
import net.creeperhost.polylib.blue.endless.jankson.api.SyntaxError;
import net.creeperhost.polylib.platform.Services;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ModPackInfo {
    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("mt-packinfo-request")
                    .setDaemon(true)
                    .build()
    );

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CH = "https://www.creeperhost.net/";

    private static CompletableFuture<VersionInfo> initTask;

    public static void init() {
        initTask = CompletableFuture.supplyAsync(() -> new VersionInfo().init(), EXECUTOR);
    }

    public static VersionInfo getInfo() {
        try {
            return initTask.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.warn("Failed to retrieve version data", e);
            return new VersionInfo();
        }
    }

    //Note Callback may be called from a different thread.
    public static void waitForInfo(Consumer<VersionInfo> callback) {
        initTask.thenAccept(callback);
    }

    public static class ModpackVersionManifest {

        public long id;
        public long parent;
    }

    public static class FTBInstanceNew {
        public long id;
        public long packType;
        public long versionId;
    }

    public static class CurseInstance {
        public long projectID = -1;
    }

    public static class FTBInstance {
        public long id = -1;
        public int packType = -1;
    }

    public static class Auxilium {
        public int id = -1;
        public AuxiliumVersion version = null;
    }

    public static class AuxiliumVersion {
        int id = -1;
        String name = "";
        String type = "";
    }

    private static class LazyMTLocalConfig {
        boolean connectPackBypass;
        String connectPackKey = "";
        String connectPackProjectType = "";
        String connectPackProjectId = "";
        String connectPackProjectVersion = "";
        String connectPackDisplayName = "";
        String connectPackMinecraftVersion = "";
        int connectPackCreeperHostVersionId = -1;
    }

    public static class VersionInfo {
        public String curseID = "";
        public String websiteID = "";
        public String base64FTBID = "";
        public String ftbPackID = "";
        public String ftbUploadID = "";
        public String realName = "{\"p\": \"-1\"}";

        private final Jankson jankson = Jankson.builder().build();

        public VersionInfo() {}

        public VersionInfo init() {
            Path versionJson = BlockShot.gameFolder().resolve("version.json");
            Path versionJsonNew = BlockShot.gameFolder().resolve("instance.json");

            if (!applyManualOverride()
                    && !readAuxiliumMetadata()
                    && !readVersionJson(versionJson)
                    && !readNewFTB(versionJsonNew)) {
                tryParseLauncherFiles();
            }

            if (!curseID.isEmpty()) {
                fetchWebsiteIDCurse();
            }

            Map<String, String> json = new HashMap<>();
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

        private boolean applyManualOverride() {
            LazyMTLocalConfig config = readMTLocalConfig();
            if (config == null) {
                return false;
            }
            if (config.connectPackBypass) {
                return true;
            }
            if (isBlank(config.connectPackKey)) {
                return false;
            }

            String type = strip(config.connectPackProjectType).toLowerCase(Locale.ROOT);
            if ("ftb".equals(type) || !NumberUtils.isParsable(config.connectPackKey)) {
                base64FTBID = config.connectPackKey;
                if (NumberUtils.isParsable(config.connectPackProjectId)) {
                    setFTBUploadID(config.connectPackProjectId);
                } else {
                    resolveFTBUploadID(base64FTBID);
                }
            } else {
                curseID = config.connectPackKey;
            }

            if (config.connectPackCreeperHostVersionId > 0) {
                websiteID = String.valueOf(config.connectPackCreeperHostVersionId);
            }
            return hasConnectPackKey();
        }

        private LazyMTLocalConfig readMTLocalConfig() {
            Path game = BlockShot.gameFolder();
            Path[] paths = new Path[]{
                    game.resolve("local/minetogether/minetogethercommunity.json"),
                    game.resolve("local/minetogether/minetogether.json")
            };
            for (Path path : paths) {
                if (!Files.exists(path)) continue;
                try (InputStream is = Files.newInputStream(path)) {
                    return jankson.fromJson(jankson.load(is), LazyMTLocalConfig.class);
                } catch (IOException | SyntaxError ex) {
                    LOGGER.warn("Failed to read MineTogether local pack config {}", path, ex);
                }
            }
            return null;
        }

        private boolean readAuxiliumMetadata() {
            Path auxilium = Services.PLATFORM.getConfigFolder().resolve("metadata.json");
            if (!Files.exists(auxilium)) return false;
            try {
                Auxilium aux = JsonUtils.parse(GSON, auxilium, Auxilium.class);
                if (aux.id > 0 && aux.version != null && aux.version.id > 0) {
                    LOGGER.info("Found auxilium id: {} version: {}", aux.id, aux.version.id);
                    setFTB(aux.id, aux.version.id);
                    return true;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to load pack id from metadata.json", e);
            }
            return false;
        }

        private boolean readVersionJson(Path versionJson) {
            if (!Files.exists(versionJson)) return false;
            try {
                ModpackVersionManifest manifest = JsonUtils.parse(GSON, versionJson, ModpackVersionManifest.class);
                if (manifest.parent <= 0 || manifest.id <= 0) return false;
                setFTB(manifest.parent, manifest.id);
                return true;
            } catch (Exception ex) {
                LOGGER.error("Failed to load version manifest.", ex);
                return false;
            }
        }

        private boolean readNewFTB(Path path) {
            if (!Files.exists(path)) return false;
            try {
                FTBInstanceNew manifest = JsonUtils.parse(GSON, path, FTBInstanceNew.class);
                if (manifest.id <= 0) return false;
                if (manifest.packType == 0 && manifest.versionId > 0) {
                    setFTB(manifest.id, manifest.versionId);
                    return true;
                } else if (manifest.packType == 1) {
                    curseID = String.valueOf(manifest.id);
                    LOGGER.info("Extracted CurseID {} from instance.json", curseID);
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load version manifest.", e);
            }
            return false;
        }

        private void tryParseLauncherFiles() {
            Path instanceJson = BlockShot.gameFolder().resolve("instance.json");
            if (Files.exists(instanceJson)) {
                try {
                    FTBInstance instance = JsonUtils.parse(GSON, instanceJson, FTBInstance.class);
                    if (instance.packType == 1 && instance.id > 0) {
                        curseID = String.valueOf(instance.id);
                        LOGGER.info("Extracted CurseID {} from instance.json", curseID);
                        return;
                    }
                } catch (IOException ex) {
                    LOGGER.warn("Failed to load pack id from instance.json", ex);
                }
            }

            Path versionJson = BlockShot.gameFolder().resolve("minecraftinstance.json");
            if (Files.exists(versionJson)) {
                try {
                    CurseInstance instance = JsonUtils.parse(GSON, versionJson, CurseInstance.class);
                    if (instance.projectID > 0) {
                        curseID = String.valueOf(instance.projectID);
                        LOGGER.info("Extracted CurseID {} from minecraftinstance.json", curseID);
                        return;
                    }
                } catch (IOException ex) {
                    LOGGER.warn("Failed to load pack id from minecraftinstance.json", ex);
                }
            }

            readMultiMc();
            if (curseID.isEmpty() && ftbPackID.isEmpty()) {
                LOGGER.info("Could not find a supported launcher modpack identity.");
            }
        }

        private void readMultiMc() {
            Path parent = BlockShot.gameFolder().getParent();
            if (parent == null) return;
            Path instanceCfg = parent.resolve("instance.cfg");
            if (!Files.exists(instanceCfg)) return;

            Map<String, String> values = new LinkedHashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(instanceCfg)))) {
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
                LOGGER.info("Extracted CurseID {} from instance.cfg", curseID);
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

        public String getConnectPackKey() {
            if (!base64FTBID.isEmpty()) return base64FTBID;
            if (!curseID.isEmpty()) return curseID;
            return null;
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
            } catch (IOException ex) {
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
            } catch (IOException ex) {
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

    private static JsonObject getJson(String url) throws IOException {
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

    private static String encodeFTB(Object packId, Object versionId) {
        return Base64.getEncoder().encodeToString((String.valueOf(packId) + versionId).getBytes(StandardCharsets.UTF_8));
    }

    private static String encodePath(String value) throws IOException {
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
