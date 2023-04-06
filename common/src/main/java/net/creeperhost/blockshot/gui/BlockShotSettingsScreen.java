package net.creeperhost.blockshot.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.creeperhost.blockshot.Auth;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class BlockShotSettingsScreen extends Screen {
    private final BlockShotHistoryScreen parent;
    private boolean prevAnon;

    public BlockShotSettingsScreen(BlockShotHistoryScreen parent) {
        super(new TranslatableComponent("gui.blockshot.settings.title"));
        this.parent = parent;
    }

    protected void init() {
        int xPos = width / 2 - 102;
        int yPos = height / 4 + 24 - 16;

        //Owner Button
        String value2 = I18n.get("gui.blockshot.upload.owner") + " " + (Config.INSTANCE.anonymous ? I18n.get("gui.blockshot.upload.anonymous") : minecraft.getUser().getName());
        addButton(new Button(xPos, yPos, 204, 20, new TextComponent(value2), button -> {
            Config.INSTANCE.anonymous = !Config.INSTANCE.anonymous;
            Config.saveConfigToFile(BlockShot.configLocation.toFile());
            Minecraft.getInstance().setScreen(this);
        }));
        prevAnon = Config.INSTANCE.anonymous;
        yPos += 24;

        //Upload Mode
        addButton(new Button(xPos, yPos, 204, 20, new TranslatableComponent(Config.INSTANCE.uploadMode.translatableName()), button -> {
            Config.INSTANCE.uploadMode = Config.INSTANCE.uploadMode.next();
            Config.saveConfigToFile(BlockShot.configLocation.toFile());
            Minecraft.getInstance().setScreen(this);
        }));
        yPos += 24;

        //Button Position
        addButton(new Button(xPos, yPos, 204, 20, new TranslatableComponent(Config.INSTANCE.buttonPos.translatableName()), button -> {
            Config.INSTANCE.buttonPos = Config.INSTANCE.buttonPos.next();
            Config.saveConfigToFile(BlockShot.configLocation.toFile());
            Minecraft.getInstance().setScreen(this);
        }));
        yPos += 24;

        yPos += 10;

        RadioButton setGif = addButton(new RadioButton(xPos, yPos, 101, 20, new TranslatableComponent("gui.blockshot.settings.encoder.gif"), button -> {
            Config.INSTANCE.setEncoderType(Config.EncoderType.GIF);
            Config.saveConfigToFile(BlockShot.configLocation.toFile());
            Minecraft.getInstance().setScreen(this);
        }));
        setGif.setSelected(() -> Config.INSTANCE.getEncoderType() == Config.EncoderType.GIF);

        Button.OnTooltip premiumTooltip = Auth.hasPremium() ? Button.NO_TOOLTIP : (button, poseStack, x, y) -> {
            List<FormattedCharSequence> list = font.split(new TranslatableComponent("gui.blockshot.settings.encoder.mov.experimental"), (int) (width / 2.1));
            renderTooltip(poseStack, list, x, y);
        };

        RadioButton setMOV = addButton(new RadioButton(xPos + 104, yPos, 101, 20, new TranslatableComponent("gui.blockshot.settings.encoder.mov"), button -> {
            Config.INSTANCE.setEncoderType(Config.EncoderType.MOV);
            Config.saveConfigToFile(BlockShot.configLocation.toFile());
            Minecraft.getInstance().setScreen(this);
        }, premiumTooltip));
        setMOV.setSelected(() -> Config.INSTANCE.getEncoderType() == Config.EncoderType.MOV);
        setMOV.active = Auth.hasPremium();

        yPos += 24;


        //Back Button
        addButton(new Button(xPos, yPos, 204, 20, CommonComponents.GUI_BACK, e -> {
            minecraft.setScreen(parent);
        }));
        yPos += 24;
    }

    public void render(PoseStack poseStack, int i, int j, float f) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, font, title, width / 2, 40, 16777215);
        drawCenteredString(poseStack, font, new TranslatableComponent("gui.blockshot.settings.encoder"), width / 2, height / 4 + 80, 16777215);
        super.render(poseStack, i, j, f);
    }
}