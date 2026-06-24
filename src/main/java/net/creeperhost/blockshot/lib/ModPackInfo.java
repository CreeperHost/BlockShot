package net.creeperhost.blockshot.lib;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ModPackInfo {
    private static final Logger LOGGER = LogManager.getLogger();
    private static VersionInfo info;

    public static synchronized VersionInfo getInfo() {
        if (info == null) {
            info = new VersionInfo().init();
        }
        return info;
    }

    public static class VersionInfo {
        public String curseID = "";
        public String ftbPackID = "";

        private VersionInfo init() {
            File gameDir = Minecraft.getMinecraft().mcDataDir;
            File configDir = new File(gameDir, "config");

            curseID = checkMTConfig(configDir);

            if (!readVersionJson(new File(gameDir, "version.json"))) {
                if (!readNewFTB(new File(gameDir, "instance.json"))) {
                    if (curseID.isEmpty()) {
                        tryParseLauncherFiles(gameDir, configDir);
                    }
                }
            }
            return this;
        }

        private boolean readVersionJson(File path) {
            JsonObject json = readJson(path);
            if (json == null || !json.has("parent")) {
                return false;
            }

            String parent = json.get("parent").getAsString();
            if (!NumberUtils.isParsable(parent)) {
                return false;
            }

            ftbPackID = "m" + parent;
            return true;
        }

        private boolean readNewFTB(File path) {
            JsonObject json = readJson(path);
            if (json == null || !json.has("packType") || !json.has("id")) {
                return false;
            }

            int packType = json.get("packType").getAsInt();
            if (packType == 0 && json.has("versionId")) {
                ftbPackID = "m" + json.get("versionId").getAsString();
                return true;
            }
            if (packType == 1) {
                curseID = json.get("id").getAsString();
                return true;
            }
            return false;
        }

        private String checkMTConfig(File configDir) {
            String curse = readCurseProjectID(new File(configDir, "minetogethercommunity.json"));
            if (curse.isEmpty()) {
                curse = readCurseProjectID(new File(configDir, "minetogether.json"));
            }
            return curse;
        }

        private String readCurseProjectID(File file) {
            JsonObject json = readJson(file);
            if (json == null || !json.has("curseProjectID")) {
                return "";
            }

            String id = json.get("curseProjectID").getAsString();
            if (!id.isEmpty()) {
                LOGGER.info("Found CurseForge ID of {}", id);
            }
            return id;
        }

        private void tryParseLauncherFiles(File gameDir, File configDir) {
            JsonObject auxilium = readJson(new File(configDir, "metadata.json"));
            if (auxilium != null && auxilium.has("id") && auxilium.has("version")) {
                JsonObject version = auxilium.getAsJsonObject("version");
                if (version != null && auxilium.get("id").getAsInt() > 0 && version.has("id")) {
                    ftbPackID = "m" + auxilium.get("id").getAsString();
                    return;
                }
            }

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

            File prismCfg = new File(gameDir.getParentFile(), "instance.cfg");
            if (prismCfg.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(prismCfg))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("ManagedPackID=") && line.length() > 14) {
                            curseID = line.substring(14);
                            return;
                        }
                    }
                } catch (Throwable ex) {
                    LOGGER.warn("Failed to load pack id from instance.cfg", ex);
                }
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
}
