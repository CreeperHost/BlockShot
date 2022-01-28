package net.creeperhost.blockshot.gui;

import net.minecraft.util.text.event.ClickEvent;

public class BlockShotClickEvent extends ClickEvent {
    public BlockShotClickEvent(Action theAction, String theValue) {
        super(theAction, theValue);
    }
}
