package net.creeperhost.blockshot.gui;

import net.minecraft.network.chat.ClickEvent;

public class BlockShotUploadEvent implements ClickEvent {
    @Override
    public Action action() {
        return Action.RUN_COMMAND;
    }
}
