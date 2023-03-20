package net.creeperhost.blockshot.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class BlockShotSettingsScreen extends Screen {
    private final Screen parent;

    public BlockShotSettingsScreen(Screen parent) {
        super(Component.translatable("gui.blockshot.settings.title"));
        this.parent = parent;
    }

    protected void init() {
        int xPos = width / 2 - 102;
        int yPos = height / 4 + 24 - 16;

        //Owner Button
        String value2 = I18n.get("gui.blockshot.upload.owner") + " " + (Config.INSTANCE.anonymous ? I18n.get("gui.blockshot.upload.anonymous") : minecraft.getUser().getName());
        addRenderableWidget(new Button(xPos, yPos, 204, 20, Component.literal(value2), button -> {
            Config.INSTANCE.anonymous = Config.INSTANCE.anonymous ? false : true;
            Config.saveConfigToFile(BlockShot.configLocation.toFile());
            Minecraft.getInstance().setScreen(this);
        }));
        yPos+= 24;

        //Upload Mode
        addRenderableWidget(new Button(xPos, yPos, 204, 20, Component.translatable(Config.INSTANCE.uploadMode.translatableName()), button -> {
            Config.INSTANCE.uploadMode = Config.INSTANCE.uploadMode.next();
            Config.saveConfigToFile(BlockShot.configLocation.toFile());
            Minecraft.getInstance().setScreen(this);
        }));
        yPos+= 24;

        //Button Position
        addRenderableWidget(new Button(xPos, yPos, 204, 20, Component.translatable(Config.INSTANCE.buttonPos.translatableName()), button -> {
            Config.INSTANCE.buttonPos = Config.INSTANCE.buttonPos.next();
            Config.saveConfigToFile(BlockShot.configLocation.toFile());
            Minecraft.getInstance().setScreen(this);
        }));
        yPos+= 24;

        //Back Button
        addRenderableWidget(new Button(xPos, yPos, 204, 20, CommonComponents.GUI_BACK, e -> {
            minecraft.setScreen(parent);
        }));
        yPos+= 24;
    }

    public void render(PoseStack poseStack, int i, int j, float f) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, font, title, width / 2, 40, 16777215);
        super.render(poseStack, i, j, f);
    }
}