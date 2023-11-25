package net.creeperhost.blockshot.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Vector3f;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.polylib.client.modulargui.elements.GuiElement;
import net.creeperhost.polylib.client.modulargui.lib.BackgroundRender;
import net.creeperhost.polylib.client.modulargui.lib.GuiRender;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.creeperhost.polylib.client.modulargui.lib.geometry.Position;
import net.creeperhost.polylib.helpers.MathUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by brandon3055 on 15/09/2023
 */
public class GuiLoadingSpinner extends GuiElement<GuiLoadingSpinner> implements BackgroundRender {
    private final List<Pxl> pixels = new ArrayList<>();
    private double animation = 0;
    private int fadeOut = 0;
    private double animSpeed = 0.03;
    private int texWidth;
    private int texHeight;
    private Supplier<Boolean> doSpin = () -> true;

    public GuiLoadingSpinner(@NotNull GuiParent<?> parent, ResourceLocation texture) {
        super(parent);
        try {
            Resource resource = mc().getResourceManager().getResourceOrThrow(texture);
            try (InputStream inputStream = resource.open()) {
                NativeImage image = NativeImage.read(inputStream);
                texWidth = image.getWidth();
                texHeight = image.getHeight();
                for (int x = 0; x < texWidth; x++) {
                    for (int y = 0; y < texHeight; y++) {
                        int abgr = image.getPixelRGBA(x, y);
                        int a = alphaAGBR(abgr);
                        if (a == 0) continue;
                        int r = redAGBR(abgr);
                        int g = greenAGBR(abgr);
                        int b = blueAGBR(abgr);
                        pixels.add(new Pxl(x, y, FastColor.ARGB32.color(a, r, g, b)));//
                    }
                }
            }
        } catch (IOException ignored) {
            BlockShot.LOGGER.error(ignored);
        }
    }

    public static int alphaAGBR(int i) {
        return i >>> 24;
    }

    public static int redAGBR(int i) {
        return i & 255;
    }

    public static int greenAGBR(int i) {
        return i >> 8 & 255;
    }

    public static int blueAGBR(int i) {
        return i >> 16 & 255;
    }

    public GuiLoadingSpinner setDoSpin(Supplier<Boolean> doSpin) {
        this.doSpin = doSpin;
        return this;
    }

    @Override
    public void tick(double mouseX, double mouseY) {
        super.tick(mouseX, mouseY);
        if (doSpin.get()) {
            fadeOut = Math.min(0xFF, fadeOut + 10);
        } else {
            fadeOut = Math.max(0, fadeOut - 10);
        }
        if (fadeOut == 0) {
            animation = 0;
        } else {
            animation += animSpeed;
        }
    }

    @Override
    public void renderBehind(GuiRender render, double mouseX, double mouseY, float partialTicks) {
        if (fadeOut == 0) return;
        double time = Mth.lerp(partialTicks, animation, animation + animSpeed);
        render.pose().pushPose();
        render.pose().translate(xCenter(), yCenter(), 0);
        render.pose().mulPose(Vector3f.ZP.rotationDegrees((float) (time * 90) + 40));
        render.pose().translate(-xCenter(), -yCenter(), 0);
        render.batchDraw(() -> pixels.forEach(pxl -> pxl.draw(render, (int) xCenter() - (texWidth / 2), (int) yCenter() - (texHeight / 2), partialTicks)));
        render.pose().popPose();
    }

    private class Pxl {
        public final Position.Mutable origin;
        public final Position.Mutable pos;
        public final int colour;
        public final double random;

        public Pxl(int x, int y, int colour) {
            this.colour = colour;
            this.origin = new Position.Mutable(x, y);
            this.pos = new Position.Mutable(x, y);
            this.random = Math.random();
        }

        public void draw(GuiRender render, int x, int y, float partialTicks) {
            double time = Mth.lerp(partialTicks, animation, animation + animSpeed);
            double anim = 1 + Math.sin(time * Math.PI);
            anim = Math.max(0, (anim - 0.1) * 1.1);
            double xAnim = anim * Math.sin(time + (random * pos.x())) * 20 * random;
            double yAnim = anim * Math.cos(time + (random * pos.y())) * 20 * random;
            int r = lerpInt((float) MathUtil.clamp(anim - 0.5F, 0F, 1F), FastColor.ARGB32.red(colour), 0xFF);
            int g = lerpInt((float) MathUtil.clamp(anim - 0.5F, 0F, 1F), FastColor.ARGB32.green(colour), 0xFF);
            int b = lerpInt((float) MathUtil.clamp(anim - 0.5F, 0F, 1F), FastColor.ARGB32.blue(colour), 0xFF);
            render.rect(x + pos.x() + xAnim, y + pos.y() + yAnim, 1, 1, FastColor.ARGB32.color(fadeOut, r, g, b));
        }
    }

    public static int lerpInt(float f, int i, int j) {
        return i + Mth.floor(f * (float)(j - i));
    }
}
