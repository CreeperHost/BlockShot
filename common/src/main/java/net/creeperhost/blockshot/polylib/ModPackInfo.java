package net.creeperhost.blockshot.polylib;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import net.covers1624.quack.gson.JsonUtils;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.minetogether.lib.web.requests.GetCurseForgeVersionRequest;
import net.creeperhost.minetogether.lib.web.requests.GetModpacksCHVersionRequest;
import net.creeperhost.polylib.blue.endless.jankson.Jankson;
import net.creeperhost.polylib.blue.endless.jankson.JsonObject;
import net.creeperhost.polylib.blue.endless.jankson.api.SyntaxError;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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

    private static class LazyMTConfig {
        String curseProjectID;
    }

    public static class VersionInfo {
        public String curseID = "";
        public String websiteID = "";
        public String base64FTBID = "";
        public String ftbPackID = "";
        public String realName = "{\"p\": \"-1\"}";

        public VersionInfo() {}

        public VersionInfo init() {
            Path versionJson = Platform.getGameFolder().resolve("version.json");
            Path versionJsonNew = Platform.getGameFolder().resolve("instance.json");
            curseID = checkMTConfig();

            if (!readVersionJson(versionJson)) {
                if (!readNewFTB(versionJsonNew)) {
                    if (curseID.isEmpty()) {
                        tryParseLauncherFiles();
                    }
                    fetchWebsiteIDCurse();
                }
            }

            Map<String, String> json = new HashMap<>();
            if (ftbPackID.isEmpty()) {
                json.put("p", NumberUtils.isParsable(curseID) ? curseID : "-1");
            } else {
                json.put("p", ftbPackID);
                json.put("b", base64FTBID);
            }

            realName = GSON.toJson(json);
            return this;
        }

        private boolean readNewFTB(Path path) {
            if (Files.exists(path)) {
                try {
                    FTBInstanceNew manifest = JsonUtils.parse(GSON, path, FTBInstanceNew.class);
                    //FTB pack
                    if (manifest.packType == 0) {
                        ftbPackID = "m" + manifest.id;
                        base64FTBID = Base64.getEncoder().encodeToString((String.valueOf(manifest.id) + manifest.versionId).getBytes(StandardCharsets.UTF_8));
                        GetModpacksCHVersionRequest.Response response = BlockShot.API.execute(new GetModpacksCHVersionRequest(base64FTBID)).apiResponse();
                        if (response.getStatus().equals("error") || response.id.isEmpty()) {
                            return false;
                        }
                        websiteID = response.id;
                        return true;
                    } else if (manifest.packType == 1) {
                        //CurseForge pack
                        curseID = String.valueOf(manifest.id);
                        LOGGER.info("Extracted CurseID {} from instance.json", curseID);
                        GetCurseForgeVersionRequest.Response response = BlockShot.API.execute(new GetCurseForgeVersionRequest(curseID)).apiResponse();
                        if (response.getStatus().equals("error") || response.id.isEmpty()) {
                            return false;
                        }
                        websiteID = response.id;
                        return true;
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load version manifest.", e);
                    return false;
                }
            }
            return false;
        }

        private Jankson JANKSON = Jankson.builder().build();

        private String checkMTConfig() {
            List<Path> paths = new ArrayList<>();
            paths.add(Platform.getConfigFolder().resolve("minetogethercommunity.json"));
            paths.add(Platform.getConfigFolder().resolve("minetogether.json"));

            for (Path path : paths) {
                if (!path.toFile().exists()) continue;

                try (InputStream is = Files.newInputStream(path)) {
                    JsonObject json = JANKSON.load(is);
                    LazyMTConfig lazyMTConfig = JANKSON.fromJson(json, LazyMTConfig.class);
                    if (lazyMTConfig.curseProjectID != null && !lazyMTConfig.curseProjectID.isEmpty()) {
                        BlockShot.LOGGER.info("Found CurseForge ID of {}", lazyMTConfig.curseProjectID);
                        return lazyMTConfig.curseProjectID;
                    }
                } catch (IOException | SyntaxError e) {
                    e.printStackTrace();
                }
            }
            return "";
        }

        private boolean readVersionJson(Path versionJson) {
            if (Files.exists(versionJson)) {
                try {
                    ModpackVersionManifest manifest = JsonUtils.parse(GSON, versionJson, ModpackVersionManifest.class);
                    ftbPackID = "m" + manifest.parent;
                    base64FTBID = Base64.getEncoder().encodeToString((String.valueOf(manifest.parent) + manifest.id).getBytes(StandardCharsets.UTF_8));
                    GetModpacksCHVersionRequest.Response response = BlockShot.API.execute(new GetModpacksCHVersionRequest(base64FTBID)).apiResponse();
                    if (response.getStatus().equals("error") || response.id.isEmpty()) {
                        return false;
                    }
                    websiteID = response.id;
                    return true;
                } catch (Exception ex) {
                    LOGGER.error("Failed to load version manifest.", ex);
                    return false;
                }
            }
            return false;
        }

        private boolean fetchWebsiteIDCurse() {
            try {
                if (!NumberUtils.isParsable(curseID)) return false;
                String resolvedID = BlockShot.API.execute(new GetCurseForgeVersionRequest(curseID)).apiResponse().id;
                if (resolvedID.isEmpty()) {
                    return false;
                }
                websiteID = resolvedID;
                return true;
            } catch (IOException ex) {
                LOGGER.error("Failed to load version manifest.", ex);
            }
            return false;
        }

        private void tryParseLauncherFiles() {
            Path auxilium = Platform.getConfigFolder().resolve("metadata.json");
            if (Files.exists(auxilium)) {
                try {
                    Auxilium aux = JsonUtils.parse(GSON, auxilium, Auxilium.class);
                    if (aux.id > 0 && aux.version != null) {
                        LOGGER.info("Found auxilium id: {} version: {}", aux.id, aux.version.id);
                        ftbPackID = "m" + aux.id;
                        base64FTBID = Base64.getEncoder().encodeToString((String.valueOf(aux.id) + aux.version.id).getBytes(StandardCharsets.UTF_8));
                        GetModpacksCHVersionRequest.Response response = BlockShot.API.execute(new GetModpacksCHVersionRequest(base64FTBID)).apiResponse();
                        if (response.getStatus().equals("error") || response.id.isEmpty()) {
                            return;
                        }
                        websiteID = response.id;
                        return;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load pack id from metadata.json", e);
                }
            }

            //Curse App
            Path instanceJson = Platform.getGameFolder().resolve("instance.json");
            if (Files.exists(instanceJson)) {
                try {
                    FTBInstance instance = JsonUtils.parse(GSON, instanceJson, FTBInstance.class);
                    if (instance.packType == 1 && instance.id > 0) {
                        curseID = String.valueOf(instance.id);
                        LOGGER.info("Extracted CurseID {} from instance.json", curseID);
                        GetCurseForgeVersionRequest.Response response = BlockShot.API.execute(new GetCurseForgeVersionRequest(curseID)).apiResponse();
                        if (response.getStatus().equals("error") || response.id.isEmpty()) {
                            return;
                        }
                        websiteID = response.id;
                        return;
                    }
                } catch (IOException ex) {
                    LOGGER.warn("Failed to load pack id from instance.json", ex);
                }
            }

            //Curse Launcher
            Path versionJson = Platform.getGameFolder().resolve("minecraftinstance.json");
            if (Files.exists(versionJson)) {
                try {
                    CurseInstance instance = JsonUtils.parse(GSON, versionJson, CurseInstance.class);
                    if (instance.projectID > 0) {
                        curseID = String.valueOf(instance.projectID);
                        LOGGER.info("Extracted CurseID {} from minecraftinstance.json", curseID);
                        GetCurseForgeVersionRequest.Response response = BlockShot.API.execute(new GetCurseForgeVersionRequest(curseID)).apiResponse();
                        if (response.getStatus().equals("error") || response.id.isEmpty()) {
                            return;
                        }
                        websiteID = response.id;
                        return;
                    }
                } catch (IOException ex) {
                    LOGGER.warn("Failed to load pack id from minecraftinstance.json", ex);
                }
            }

            //Prism
            Path instanceCfg = Platform.getGameFolder().getParent().resolve("instance.cfg");
            if (Files.exists(instanceCfg)) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(instanceCfg)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("ManagedPackID=")) {
                            if (line.length() > 14) {
                                line = line.substring(14);
                                long id = Long.parseLong(line);
                                if (id > 0) {
                                    curseID = String.valueOf(id);
                                    LOGGER.info("Extracted CurseID {} from instance.cfg", curseID);
                                    GetCurseForgeVersionRequest.Response response = BlockShot.API.execute(new GetCurseForgeVersionRequest(curseID)).apiResponse();
                                    if (response.getStatus().equals("error") || response.id.isEmpty()) {
                                        return;
                                    }
                                    websiteID = response.id;
                                }
                            }
                            return;
                        }
                    }
                } catch (Throwable ex) {
                    LOGGER.warn("Failed to load pack id from instance.cfg", ex);
                }
            }

            LOGGER.info("Could not find curse pack id, Not a curse modpack, or unsupported launcher.");
        }
    }
}
