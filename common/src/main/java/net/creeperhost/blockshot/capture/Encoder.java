package net.creeperhost.blockshot.capture;

import com.mojang.blaze3d.platform.NativeImage;
import net.creeperhost.blockshot.BlockShot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Created by brandon3055 on 25/03/2023
 */
public interface Encoder {


    void startOrStopRecording();

    void updateCapture();

    void cancelRecording();

    /** Returns true if currently recording, encoding or uploading. */
    boolean isWorking();

    boolean showRecordIcon();

    List<Component> getHudText();

    default Component getStopText() {
        String screenshotKey = Minecraft.getInstance().options.keyScreenshot.getTranslatedKeyMessage().getString();
        return Component.translatable("overlay.blockshot.finish", screenshotKey).withStyle(ChatFormatting.GRAY);
    }

    default Component getCancelText() {
        String screenshotKey = Minecraft.getInstance().options.keyScreenshot.getTranslatedKeyMessage().getString();
        return Component.translatable("overlay.blockshot.cancel", screenshotKey).withStyle(ChatFormatting.GRAY);
    }

    default BufferedImage toBufferedImage(NativeImage image, int targetWidth, int targetHeight) {
        try (image) {
            int width = image.getWidth();
            int height = image.getHeight();
            image.flipY();

            if (width > height) {
                double ratio = (double) height / width;
                targetHeight = (int) Math.round(targetWidth * ratio);
                if ((targetHeight & 1) != 0) targetHeight++;
            } else {
                double ratio = (double) width / height;
                targetWidth = (int) Math.round(targetHeight * ratio);
                if ((targetHeight & 1) != 0) targetHeight++;
            }

            try (NativeImage nativeImage = new NativeImage(targetWidth, targetHeight, false)) {
                image.resizeSubRectTo(0, 0, width, height, nativeImage);
                InputStream is = new ByteArrayInputStream(nativeImage.asByteArray());
                BufferedImage finalFrame = new BufferedImage(targetWidth, targetHeight, 1);
                finalFrame.getGraphics().drawImage(ImageIO.read(is), 0, 0, null);
                return finalFrame;
            }
        } catch (Throwable t) {
            BlockShot.LOGGER.error("An error occurred while capturing frame", t);
            return null;
        }
    }

}
