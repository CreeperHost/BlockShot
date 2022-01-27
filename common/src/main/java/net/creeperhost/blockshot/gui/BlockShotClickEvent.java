package net.creeperhost.blockshot.gui;

import net.minecraft.network.chat.ClickEvent;

public class BlockShotClickEvent extends ClickEvent {
    public BlockShotClickEvent(Action action, String value) {
        super(action, value);
    }
}
