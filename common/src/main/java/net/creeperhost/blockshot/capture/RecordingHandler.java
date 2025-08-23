package net.creeperhost.blockshot.capture;

import net.creeperhost.blockshot.Auth;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Created by brandon3055 on 24/03/2023
 */
public class RecordingHandler {

    private static Encoder encoder;

    public static Encoder getEncoder() {
        if (Config.INSTANCE.getEncoderType().requiresPremium() && !Auth.hasPremium()) {
            Config.INSTANCE.setEncoderType(Config.EncoderType.GIF);
        }

        if (encoder == null) {
            setEncoder(Config.INSTANCE.getEncoderType().createEncoder());
        }
        return encoder;
    }

    public static boolean setEncoder(Encoder newEncoder) {
        if (encoder != null && encoder.isWorking()) return false;
        encoder = newEncoder;
        return true;
    }

    public static void handleScreenCapture() {
        if (!BlockShot.isActive() || !getEncoder().isWorking()) {
            return;
        }
        getEncoder().updateCapture();
    }

    public static void handleScreenCaptureOverlay(GuiGraphics graphics) {
        if (!BlockShot.isActive() || !getEncoder().isWorking()) {
            return;
        }
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().options.hideGui) return;
        drawRecordingIndicator(graphics, 5, 5);
    }

    private static void drawRecordingIndicator(GuiGraphics graphics, int x, int y) {
        List<Component> hudLines = getEncoder().getHudText();
        Font font = Minecraft.getInstance().font;

        int recordOffset = getEncoder().showRecordIcon() ? 10 : 0;

        int maxWidth = 0;
        for (Component line : hudLines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        int height = (hudLines.size() * 9) + 5;

        graphics.fill(x, y, x + maxWidth + 6 + recordOffset, y + height, 0xb0101010);

        int i = 0;
        for (Component line : hudLines) {
            graphics.drawString(font, line, x + 3 + recordOffset, y + 3 + i, 0xFFFFFFFF, true);
            i += 9;
        }

        if (System.currentTimeMillis() % 2000 > 1000 && getEncoder().showRecordIcon()) {
            graphics.fill(x + 3, y + 4, x + 3 + 7, y + 4 + 5, 0xFFFF0000);
            graphics.fill(x + 4, y + 3, x + 4 + 5, y + 3 + 7, 0xFFFF0000);
        }
    }
}
