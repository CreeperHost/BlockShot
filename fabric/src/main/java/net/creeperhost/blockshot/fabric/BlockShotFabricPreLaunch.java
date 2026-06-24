package net.creeperhost.blockshot.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class BlockShotFabricPreLaunch implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LogManager.getLogger("BlockShot");
    private static final String DEFAULT_POLYLIB_CONFIG = """
            {
              "serverOnlySupport": true,
              "configPanelKeybinds": {}
            }
            """;

    @Override
    public void onPreLaunch() {
        ensurePolylibConfig();
    }

    private static void ensurePolylibConfig() {
        Path config = FabricLoader.getInstance().getConfigDir().resolve("polylib.json");
        try {
            if (Files.exists(config)) {
                String existing = Files.readString(config, StandardCharsets.UTF_8).trim();
                if (!existing.isEmpty() && !"null".equals(existing)) {
                    return;
                }
            } else if (config.getParent() != null) {
                Files.createDirectories(config.getParent());
            }
            Files.writeString(config, DEFAULT_POLYLIB_CONFIG, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Unable to prepare PolyLib config fallback", e);
        }
    }
}
