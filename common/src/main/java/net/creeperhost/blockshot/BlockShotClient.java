package net.creeperhost.blockshot;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.creeperhost.blockshot.gui.BlockShotGui;
import net.creeperhost.blockshot.lib.HistoryManager;
import net.creeperhost.polylib.client.modulargui.ModularGuiScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Created by brandon3055 on 14/09/2023
 */
public class BlockShotClient {

    public static final KeyMapping OPEN_GUI = new KeyMapping("key.blockshot.open_blockshot", InputConstants.UNKNOWN.getValue(), "key.categories.misc");

    public static void init() {
        ClientTickEvent.CLIENT_PRE.register(mc -> HistoryManager.instance.tick());

        ClientRawInputEvent.KEY_PRESSED.register((client, keyCode, scanCode, action, modifiers) -> {
            if (OPEN_GUI.isDown() && client.screen == null) {
                Minecraft.getInstance().setScreen(new ModularGuiScreen(new BlockShotGui()));
            }
            return EventResult.pass();
        });
    }

}
