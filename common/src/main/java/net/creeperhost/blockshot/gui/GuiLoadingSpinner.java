package net.creeperhost.blockshot.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.polylib.client.modulargui.elements.GuiElement;
import net.creeperhost.polylib.client.modulargui.lib.BackgroundRender;
import net.creeperhost.polylib.client.modulargui.lib.GuiRender;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.creeperhost.polylib.client.modulargui.lib.geometry.Position;
import net.creeperhost.polylib.helpers.MathUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
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
                        int abgr = image.getPixel(x, y);
                        int a = ABGR32.alpha(abgr);
                        if (a == 0) continue;
                        int r = ABGR32.red(abgr);
                        int g = ABGR32.green(abgr);
                        int b = ABGR32.blue(abgr);
                        pixels.add(new Pxl(x, y, ARGB.color(a, r, g, b)));//
                    }
                }
            }
        } catch (IOException ignored) {
            BlockShot.LOGGER.error(ignored);
        }
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
        render.pose().pushMatrix();
        render.pose().translate((int) xCenter(), (int) yCenter());
        render.pose().rotate((float) Math.toRadians((time * 90) + 40));
        render.pose().translate((int) -xCenter(), (int) -yCenter());
        pixels.forEach(pxl -> pxl.draw(render, (int) xCenter() - (texWidth / 2), (int) yCenter() - (texHeight / 2), partialTicks));
        render.pose().popMatrix();
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
            int r = Mth.lerpInt((float) MathUtil.clamp(anim - 0.5F, 0F, 1F), ARGB.red(colour), 0xFF);
            int g = Mth.lerpInt((float) MathUtil.clamp(anim - 0.5F, 0F, 1F), ARGB.green(colour), 0xFF);
            int b = Mth.lerpInt((float) MathUtil.clamp(anim - 0.5F, 0F, 1F), ARGB.blue(colour), 0xFF);
            render.rect(x + pos.x() + xAnim, y + pos.y() + yAnim, 1, 1, ARGB.color(fadeOut, r, g, b));
        }
    }

    public static class ABGR32 {
        public static int alpha(int p_267257_) {
            return p_267257_ >>> 24;
        }

        public static int red(int p_267160_) {
            return p_267160_ & 0xFF;
        }

        public static int green(int p_266784_) {
            return p_266784_ >> 8 & 0xFF;
        }

        public static int blue(int p_267087_) {
            return p_267087_ >> 16 & 0xFF;
        }

        public static int transparent(int p_267248_) {
            return p_267248_ & 16777215;
        }

        public static int opaque(int p_268288_) {
            return p_268288_ | 0xFF000000;
        }

        public static int color(int p_267196_, int p_266895_, int p_266779_, int p_267206_) {
            return p_267196_ << 24 | p_266895_ << 16 | p_266779_ << 8 | p_267206_;
        }

        public static int color(int p_267230_, int p_266708_) {
            return p_267230_ << 24 | p_266708_ & 16777215;
        }
    }
}
