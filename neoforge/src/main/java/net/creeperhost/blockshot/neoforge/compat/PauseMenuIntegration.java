package net.creeperhost.blockshot.neoforge.compat;

import dev.ftb.mods.pmapi.api.PauseMenuApi;
import dev.ftb.mods.pmapi.api.menu.MenuLocation;
import dev.ftb.mods.pmapi.api.menu.PauseItemProvider;
import dev.ftb.mods.pmapi.api.menu.ScreenHolder;
import dev.ftb.mods.pmapi.api.menu.ScreenWidgetCollection;
import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.gui.BlockShotGui;
import net.creeperhost.blockshot.gui.IconButton;
import net.creeperhost.blockshot.gui.ModTextures;
import net.creeperhost.polylib.client.modulargui.ModularGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class PauseMenuIntegration {
    public static void init() {
        PauseMenuApi.get().registerPauseItem(MenuLocation.BOTTOM_LEFT, new MenuButtons());
    }

    public static class MenuButtons implements PauseItemProvider
    {
        @Override
        public @Nullable ScreenWidgetCollection init(MenuLocation target, ScreenHolder screen, int x, int y)
        {
            var collection = ScreenWidgetCollection.create();
            Config.ButtonPos pos = Config.INSTANCE.buttonPos;

            IconButton button = new IconButton(pos.getX(screen.getWidth(), 100), pos.getY(screen.getHeight(), 20), 100, 20, Component.translatable("gui.blockshot.blockshot_button"), e -> Minecraft.getInstance().setScreen(new ModularGuiScreen(new BlockShotGui())))
                    .setIcon(ModTextures.get("blockshot_icon"), 16, 16);

            collection.addRenderableWidget(button);
            return collection;
        }
    }
}
