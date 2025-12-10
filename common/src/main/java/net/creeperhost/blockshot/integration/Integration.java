package net.creeperhost.blockshot.integration;

import dev.architectury.platform.Platform;

import java.util.function.Supplier;

public class Integration {
    public static void runOptional(String modid, Supplier<Runnable> runnable) {
        if (Platform.isModLoaded(modid)) {
            runnable.get().run();
        }
    }
}
