package net.creeperhost.blockshot;

import com.mojang.blaze3d.platform.InputConstants;
import net.creeperhost.blockshot.gui.BlockShotGui;
import net.creeperhost.blockshot.lib.HistoryManager;
import net.creeperhost.polylib.client.modulargui.ModularGuiScreen;
import net.creeperhost.polylib.event.events.client.PolyClientTickEvents;
import net.creeperhost.polylib.event.events.client.PolyInputEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Created by brandon3055 on 14/09/2023
 */
public class BlockShotClient {

    public static final KeyMapping OPEN_GUI = new KeyMapping("key.blockshot.open_blockshot", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC);

    public static void init() {
        ClientUtil.init();

        PolyClientTickEvents.CLIENT_TICK_START.register(mc -> HistoryManager.instance.tick());

        PolyInputEvents.INPUT_KEY.register((key, scanCode, action, modifiers) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (OPEN_GUI.isDown() && minecraft.screen == null) {
                Minecraft.getInstance().setScreen(new ModularGuiScreen(new BlockShotGui()));
            }
        });
    }

}
