package net.creeperhost.blockshot.gui;

import net.creeperhost.blockshot.Auth;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class BlockShotSettingsScreen extends Screen {
    private final BlockShotHistoryScreen parent;
    private boolean prevAnon;

    public BlockShotSettingsScreen(BlockShotHistoryScreen parent) {
        super(Component.translatable("gui.blockshot.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int xPos = width / 2 - 102;
        int yPos = height / 4 + 24 - 16;

        //Owner Button
        String value2 = I18n.get("gui.blockshot.upload.owner") + " " + (Config.INSTANCE.anonymous ? I18n.get("gui.blockshot.upload.anonymous") : minecraft.getUser().getName());
        addRenderableWidget(Button.builder(Component.literal(value2), button -> {
                    Config.INSTANCE.anonymous = !Config.INSTANCE.anonymous;
                    Config.saveConfigToFile(BlockShot.configLocation.toFile());
                    Minecraft.getInstance().setScreen(this);
                })
                .bounds(xPos, yPos, 204, 20).build());
        prevAnon = Config.INSTANCE.anonymous;
        yPos += 24;

        //Upload Mode
        addRenderableWidget(Button.builder(Component.translatable(Config.INSTANCE.uploadMode.translatableName()), button -> {
                    Config.INSTANCE.uploadMode = Config.INSTANCE.uploadMode.next();
                    Config.saveConfigToFile(BlockShot.configLocation.toFile());
                    Minecraft.getInstance().setScreen(this);
                })
                .bounds(xPos, yPos, 204, 20).build());
        yPos += 24;

        //Button Position
        addRenderableWidget(Button.builder(Component.translatable(Config.INSTANCE.buttonPos.translatableName()), button -> {
                    Config.INSTANCE.buttonPos = Config.INSTANCE.buttonPos.next();
                    Config.saveConfigToFile(BlockShot.configLocation.toFile());
                    Minecraft.getInstance().setScreen(this);
                })
                .bounds(xPos, yPos, 204, 20).build());
        yPos += 24;

        yPos += 10;

        RadioButton setGif = addRenderableWidget(new RadioButton(xPos, yPos, 101, 20, Component.translatable("gui.blockshot.settings.encoder.gif"), button -> {
            Config.INSTANCE.setEncoderType(Config.EncoderType.GIF);
            Config.saveConfigToFile(BlockShot.configLocation.toFile());
            Minecraft.getInstance().setScreen(this);
        }));
        setGif.setSelected(() -> Config.INSTANCE.getEncoderType() == Config.EncoderType.GIF);

        RadioButton setMOV = addRenderableWidget(new RadioButton(xPos + 104, yPos, 101, 20, Component.translatable("gui.blockshot.settings.encoder.mov"), button -> {
            Config.INSTANCE.setEncoderType(Config.EncoderType.MOV);
            Config.saveConfigToFile(BlockShot.configLocation.toFile());
            Minecraft.getInstance().setScreen(this);
        }));
        if (!Auth.hasPremium()) {
            setMOV.setTooltip(Tooltip.create(Component.translatable("gui.blockshot.settings.encoder.mov.experimental")));
        }

        setMOV.setSelected(() -> Config.INSTANCE.getEncoderType() == Config.EncoderType.MOV);
        setMOV.active = Auth.hasPremium();

        yPos += 24;


        //Back Button
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, e -> minecraft.setScreen(parent))
                .bounds(xPos, yPos, 204, 20).build());
        yPos += 24;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, title, width / 2, 40, 0xffffff);
        guiGraphics.drawCenteredString(font, Component.translatable("gui.blockshot.settings.encoder"), width / 2, height / 4 + 80, 0xffffff);
        super.render(guiGraphics, i, j, f);
    }
}