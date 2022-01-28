package net.creeperhost.blockshot.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

public class LoadingSpinner {
    public static void render(float partialTicks, int ticks, int width, int height, ItemStack stack)
    {
        int rotateTickMax = 30;
        int throbTickMax = 20;
        int rotateTicks = ticks % rotateTickMax;
        int throbTicks = ticks % throbTickMax;
        GlStateManager.translate(width / 2, height / 2 + 20 + 10, 0);
        GlStateManager.pushMatrix();
        float scale = 1F + ((throbTicks >= (throbTickMax / 2) ? (throbTickMax - (throbTicks + partialTicks)) : (throbTicks + partialTicks)) * (2F / throbTickMax));
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.rotate((rotateTicks + partialTicks) * (360F / rotateTickMax), 0, 0, 1);
        GlStateManager.pushMatrix();

        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, -8, -8);

        GlStateManager.popMatrix();
        GlStateManager.popMatrix();
    }
}
