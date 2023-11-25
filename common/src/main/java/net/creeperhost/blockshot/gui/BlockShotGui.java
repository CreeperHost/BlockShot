package net.creeperhost.blockshot.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.creeperhost.blockshot.Auth;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.lib.Capture;
import net.creeperhost.blockshot.lib.HistoryManager;
import net.creeperhost.blockshot.lib.TextureCache;
import net.creeperhost.polylib.client.PolyPalette.Flat;
import net.creeperhost.polylib.client.modulargui.ModularGui;
import net.creeperhost.polylib.client.modulargui.elements.GuiButton;
import net.creeperhost.polylib.client.modulargui.elements.GuiElement;
import net.creeperhost.polylib.client.modulargui.elements.GuiScrolling;
import net.creeperhost.polylib.client.modulargui.elements.GuiText;
import net.creeperhost.polylib.client.modulargui.lib.BackgroundRender;
import net.creeperhost.polylib.client.modulargui.lib.Constraints;
import net.creeperhost.polylib.client.modulargui.lib.GuiProvider;
import net.creeperhost.polylib.client.modulargui.lib.GuiRender;
import net.creeperhost.polylib.client.modulargui.lib.geometry.Align;
import net.creeperhost.polylib.client.modulargui.lib.geometry.Axis;
import net.creeperhost.polylib.client.modulargui.lib.geometry.Constraint;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static net.creeperhost.polylib.client.modulargui.lib.geometry.Constraint.*;
import static net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam.*;

/**
 * Created by brandon3055 on 14/09/2023
 */
public class BlockShotGui implements GuiProvider {
    private static final SimpleDateFormat DAY_OF_MONTH_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy");
    private static final SimpleDateFormat HH_MM_FORMAT = new SimpleDateFormat("HH:mm");

    private ModularGui gui;
    private int entryWidth = 42;
    private int entryHeight = 42;

    private int historyState = -1;
    private GuiScrolling scrollElement;

    private Capture selected = null;

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        return Flat.background(gui);
    }

    @Override
    public void buildGui(ModularGui gui) {
        this.gui = gui;
        gui.renderScreenBackground(false);
        gui.initFullscreenGui();
        gui.setGuiTitle(new TranslatableComponent("gui.blockshot.title"));

        GuiElement<?> root = gui.getRoot();

        GuiText title = new GuiText(root, gui.getGuiTitle())
                .constrain(TOP, relative(root.get(TOP), 5))
                .constrain(HEIGHT, Constraint.literal(8))
                .constrain(LEFT, match(root.get(LEFT)))
                .constrain(RIGHT, match(root.get(RIGHT)));

        if (!BlockShot.isActive()) {
            title.setText(new TranslatableComponent("gui.blockshot.history.not_in_offline").withStyle(ChatFormatting.RED));
            return;
        }

        new HeaderInfo(root) //Draws the keybind info
                .constrain(TOP, match(root.get(TOP)))
                .constrain(HEIGHT, Constraint.literal(20))
                .constrain(LEFT, match(root.get(LEFT)))
                .constrain(RIGHT, match(root.get(RIGHT)));

        //Note, scroll bar takes up 10 pixels to the right og this element.
        GuiElement<?> historyPanel = createHistoryPanel(root, entryWidth);

        //Selected Capture
        GuiButton copyUrl = Flat.button(root, new TranslatableComponent("gui.blockshot.history.copy_url"))
                .setEnabled(() -> selected != null)
                .onPress(() -> activateSelected(true))
                .constrain(LEFT, match(historyPanel.get(LEFT)))
                .constrain(BOTTOM, relative(root.get(BOTTOM), -4))
                .constrain(WIDTH, literal(80))
                .constrain(HEIGHT, literal(14));

        GuiButton view = Flat.button(root, new TranslatableComponent("gui.blockshot.history.view"))
                .setEnabled(() -> selected != null)
                .onPress(() -> activateSelected(false))
                .constrain(LEFT, relative(copyUrl.get(RIGHT), 2))
                .constrain(BOTTOM, relative(root.get(BOTTOM), -4))
                .constrain(WIDTH, literal(80))
                .constrain(HEIGHT, literal(14));

        GuiButton delete = Flat.buttonCaution(root, new TranslatableComponent("gui.blockshot.history.delete"))
                .setEnabled(() -> selected != null)
                .onPress(this::deleteSelected)
                .constrain(RIGHT, match(historyPanel.get(RIGHT)))
                .constrain(BOTTOM, relative(root.get(BOTTOM), -4))
                .constrain(WIDTH, literal(60))
                .constrain(HEIGHT, literal(14));

        GuiElement<?> previous;
        //Owner Setting
        previous = new GuiText(root, new TranslatableComponent("gui.blockshot.settings.upload.owner").withStyle(ChatFormatting.UNDERLINE))
                .constrain(RIGHT, relative(root.get(RIGHT), -4))
                .constrain(TOP, match(historyPanel.get(TOP)))
                .constrain(LEFT, relative(historyPanel.get(RIGHT), 12))
                .constrain(HEIGHT, literal(8));

        previous = Flat.button(root, TextComponent.EMPTY)
                .setTooltipSingle(new TranslatableComponent("gui.blockshot.settings.upload.info"))
                .onPress(() -> {
                    Config.INSTANCE.anonymous = !Config.INSTANCE.anonymous;
                    Config.saveConfigToFile(BlockShot.configLocation.toFile());
                })
                .constrain(RIGHT, relative(root.get(RIGHT), -4))
                .constrain(TOP, relative(previous.get(BOTTOM), 2))
                .constrain(LEFT, relative(historyPanel.get(RIGHT), 12))
                .constrain(HEIGHT, literal(14));
        ((GuiButton) previous).getLabel().setTextSupplier(() -> Config.INSTANCE.anonymous ? new TranslatableComponent("gui.blockshot.settings.upload.anonymous") : gui.mc().player.getName());

        //Upload Setting
        previous = new GuiText(root, new TranslatableComponent("gui.blockshot.settings.upload_mode").withStyle(ChatFormatting.UNDERLINE))
                .constrain(RIGHT, relative(root.get(RIGHT), -4))
                .constrain(TOP, relative(previous.get(BOTTOM), 8))
                .constrain(LEFT, relative(historyPanel.get(RIGHT), 12))
                .constrain(HEIGHT, literal(8));

        previous = Flat.button(root, TextComponent.EMPTY)
                .onPress(() -> {
                    Config.INSTANCE.uploadMode = Config.INSTANCE.uploadMode.next();
                    Config.saveConfigToFile(BlockShot.configLocation.toFile());
                })
                .constrain(RIGHT, relative(root.get(RIGHT), -4))
                .constrain(TOP, relative(previous.get(BOTTOM), 2))
                .constrain(LEFT, relative(historyPanel.get(RIGHT), 12))
                .constrain(HEIGHT, literal(14));
        ((GuiButton) previous).getLabel().setTextSupplier(() -> new TranslatableComponent(Config.INSTANCE.uploadMode.translatableName()));

        //Position Setting
        previous = new GuiText(root, new TranslatableComponent("gui.blockshot.settings.button_pos").withStyle(ChatFormatting.UNDERLINE))
                .setTooltipSingle(new TranslatableComponent("gui.blockshot.settings.button_pos.info"))
                .constrain(RIGHT, relative(root.get(RIGHT), -4))
                .constrain(TOP, relative(previous.get(BOTTOM), 8))
                .constrain(LEFT, relative(historyPanel.get(RIGHT), 12))
                .constrain(HEIGHT, literal(8));

        previous = Flat.button(root, TextComponent.EMPTY)
                .onPress(() -> {
                    Config.INSTANCE.buttonPos = Config.INSTANCE.buttonPos.next();
                    Config.saveConfigToFile(BlockShot.configLocation.toFile());
                })
                .setTooltipSingle(new TranslatableComponent("gui.blockshot.settings.button_pos.info"))
                .constrain(RIGHT, relative(root.get(RIGHT), -4))
                .constrain(TOP, relative(previous.get(BOTTOM), 2))
                .constrain(LEFT, relative(historyPanel.get(RIGHT), 12))
                .constrain(HEIGHT, literal(14));
        ((GuiButton) previous).getLabel().setTextSupplier(() -> new TranslatableComponent(Config.INSTANCE.buttonPos.translatableName()));

        //Encoder Setting
        previous = new GuiText(root, new TranslatableComponent("gui.blockshot.settings.encoder").withStyle(ChatFormatting.UNDERLINE))
                .constrain(RIGHT, relative(root.get(RIGHT), -4))
                .constrain(TOP, relative(previous.get(BOTTOM), 8))
                .constrain(LEFT, relative(historyPanel.get(RIGHT), 12))
                .constrain(HEIGHT, literal(8));

        previous = Flat.button(root, new TranslatableComponent("gui.blockshot.settings.encoder.gif"))
                .setToggleMode(() -> Config.INSTANCE.getEncoderType() == Config.EncoderType.GIF || !Auth.hasPremium())
                .onPress(() -> {
                    Config.INSTANCE.setEncoderType(Config.EncoderType.GIF);
                    Config.saveConfigToFile(BlockShot.configLocation.toFile());
                })
                .constrain(RIGHT, relative(root.get(RIGHT), -4))
                .constrain(TOP, relative(previous.get(BOTTOM), 2))
                .constrain(LEFT, relative(historyPanel.get(RIGHT), 12))
                .constrain(HEIGHT, literal(14));

        previous = Flat.button(root, new TranslatableComponent("gui.blockshot.settings.encoder.mov"))
                .setToggleMode(() -> Config.INSTANCE.getEncoderType() == Config.EncoderType.MOV && Auth.hasPremium())
                .setDisabled(() -> !Auth.hasPremium())
                .onPress(() -> {
                    Config.INSTANCE.setEncoderType(Config.EncoderType.MOV);
                    Config.saveConfigToFile(BlockShot.configLocation.toFile());
                })
                .setTooltipSingle(new TranslatableComponent("gui.blockshot.settings.encoder.mov.info"))
                .constrain(RIGHT, relative(root.get(RIGHT), -4))
                .constrain(TOP, relative(previous.get(BOTTOM), 2))
                .constrain(LEFT, relative(historyPanel.get(RIGHT), 12))
                .constrain(HEIGHT, literal(14));

        Flat.button(root, new TranslatableComponent("gui.blockshot.close"))
                .onPress(() -> gui.mc().setScreen(null))
                .constrain(RIGHT, relative(root.get(RIGHT), -4))
                .constrain(LEFT, relative(historyPanel.get(RIGHT), 12))
                .constrain(BOTTOM, match(historyPanel.get(BOTTOM)))
                .constrain(HEIGHT, literal(14));


        new GuiLoadingSpinner(root, new ResourceLocation(BlockShot.MOD_ID, "textures/gui/loading_spinner.png"))
//        new GuiLoadingSpinner(root, new ResourceLocation(BlockShot.MOD_ID, "textures/gui/blockshot.png"))
                .setDoSpin(() -> HistoryManager.instance.isDownloading())
                .constrain(TOP, match(historyPanel.get(TOP)))
                .constrain(LEFT, match(historyPanel.get(LEFT)))
                .constrain(BOTTOM, match(historyPanel.get(BOTTOM)))
                .constrain(RIGHT, match(historyPanel.get(RIGHT)));


        gui.onTick(() -> detectHistoryChanges(false));
        gui.onResize(() -> detectHistoryChanges(true));
        gui.onMouseClickPost((aDouble, aDouble2, integer) -> selected = null);
        gui.onKeyPressPost((key, code, modifier) -> {
            if (key == InputConstants.KEY_DELETE) deleteSelected();
        });
    }

    private void activateSelected(boolean copyUrl) {
        if (selected == null) return;
        if (copyUrl) {
            Minecraft.getInstance().keyboardHandler.setClipboard("https://blockshot.ch/" + selected.id());
        } else {
            Util.getPlatform().openUri("https://blockshot.ch/" + selected.id());
        }
    }

    private void deleteSelected() {
        if (selected == null) return;
        HistoryManager.instance.deleteCapture(selected);
        selected = null;
    }

    private GuiElement<?> createHistoryPanel(GuiElement<?> root, int entryWidth) {
        GuiElement<?> container = Flat.contentArea(root)
                .constrain(WIDTH, dynamic(() -> historyColumns() * (double) entryWidth))
                .constrain(LEFT, relative(root.get(LEFT), 10))
                .constrain(TOP, relative(root.get(TOP), 30))
                .constrain(BOTTOM, relative(root.get(BOTTOM), -22));

        Component title = new TranslatableComponent("gui.blockshot.history.title").withStyle(ChatFormatting.UNDERLINE);
        Component anonWarning = new TranslatableComponent("gui.blockshot.history.anon_warning").withStyle(ChatFormatting.RED);
        new GuiText(container, () -> title.copy().append(Config.INSTANCE.anonymous ? new TextComponent(" ").append(anonWarning) : TextComponent.EMPTY))
                .setTextColour(0xFFFFFF)
                .setShadow(false)
                .setAlignment(Align.LEFT)
                .constrain(BOTTOM, relative(container.get(TOP), -3))
                .constrain(HEIGHT, Constraint.literal(8))
                .constrain(LEFT, match(container.get(LEFT)))
                .constrain(RIGHT, match(root.get(RIGHT)));

        scrollElement = new GuiScrolling(container);
        Constraints.bind(scrollElement, container);
        scrollElement.getContentElement().setZStacking(false);

        var scrollBar = Flat.scrollBar(container, Axis.Y);
        scrollBar.container
                .setEnabled(() -> scrollElement.hiddenSize(Axis.Y) > 0)
                .constrain(TOP, match(container.get(TOP)))
                .constrain(LEFT, relative(container.get(RIGHT), 1))
                .constrain(BOTTOM, match(container.get(BOTTOM)))
                .constrain(WIDTH, Constraint.literal(8));

        scrollBar.primary
                .setSliderState(scrollElement.scrollState(Axis.Y))
                .setScrollableElement(scrollElement);

        GuiText noHistory = new GuiText(container, new TranslatableComponent("gui.blockshot.history.no_history"))
                .setEnabled(HistoryManager.instance::hasNoHistory)
                .setTextColour(0xFFFFFF)
                .setShadow(false);
        Constraints.bind(noHistory, container);

        return container;
    }

    private int historyColumns() {
        return (int) Math.floor((gui.xSize() * 0.667) / entryWidth);
    }

    private void detectHistoryChanges(boolean force) {
        HistoryManager manager = HistoryManager.instance;
        manager.updateHistory();
        if (historyState == manager.getState() && !force) return;
        historyState = manager.getState();

        GuiElement<?> content = scrollElement.getContentElement();
        content.getChildren().forEach(content::removeChild);

        int yOffset = 0;
        int column = 0;
        int columns = historyColumns();

        long currentDay = Long.MAX_VALUE;

        for (Capture capture : manager.getCaptureHistory()) {
            //Add Day Headings
            long day = TimeUnit.MILLISECONDS.toDays((capture.created() * 1000) + TimeZone.getDefault().getRawOffset());
            if (day != currentDay) {
                currentDay = day;
                MutableComponent dateText;
                if (day == TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() + TimeZone.getDefault().getRawOffset())) {
                    dateText = new TranslatableComponent("gui.blockshot.history.today");
                } else {
                    dateText = new TextComponent(DAY_OF_MONTH_FORMAT.format(capture.created() * 1000));
                }

                if (column != 0) yOffset += entryHeight;

                new GuiText(content, dateText.withStyle(ChatFormatting.UNDERLINE))
                        .setTextColour(0xFFFFFF)
                        .setShadow(false)
                        .constrain(TOP, relative(content.get(TOP), yOffset + 2))
                        .constrain(LEFT, match(scrollElement.get(LEFT)))
                        .constrain(RIGHT, match(scrollElement.get(RIGHT)))
                        .constrain(HEIGHT, literal(8));
                yOffset += 12;
                column = 0;
            }

            new CaptureElement(content, capture)
                    .constrain(TOP, relative(content.get(TOP), yOffset))
                    .constrain(LEFT, relative(content.get(LEFT), column * entryWidth))
                    .constrain(WIDTH, literal(entryWidth))
                    .constrain(HEIGHT, literal(entryHeight));

            column++;
            if (column == columns) {
                column = 0;
                yOffset += entryHeight;
            }
        }
    }

    private class CaptureElement extends GuiElement<CaptureElement> implements BackgroundRender {
        private final Capture capture;
        private long lastClick = 0;

        public CaptureElement(@NotNull GuiParent<?> parent, Capture capture) {
            super(parent);
            this.capture = capture;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isMouseOver()) {
                selected = capture;
                if (System.currentTimeMillis() - lastClick < 500) {
                    activateSelected(false);
                    mc().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
                    lastClick = 0;
                } else {
                    lastClick = System.currentTimeMillis();
                }
                return true;
            }
            return false;
        }

        @Override
        public void renderBehind(GuiRender render, double mouseX, double mouseY, float partialTicks) {
            if (isMouseOver() || capture.equals(selected)) {
                render.borderRect(getRectangle(), 1, 0x50808080, 0x80FFFFFF);
            }

            double size = (Math.min(xSize(), ySize()) / 2) - 1;
            drawTexture(render, xCenter() - size, yCenter() - size, xCenter() + size, yCenter() + size);

            render.pose().pushPose();
            render.pose().translate(xMin() + 2, yMax() - 8, 0);
            render.pose().scale(0.75F, 0.75F, 1F);
            render.drawString(HH_MM_FORMAT.format(capture.created() * 1000L), 0, 0, 0x8080FF, false);
            String format = capture.format();
            if (format.contains("/")) format = format.substring(format.indexOf("/") + 1);
            render.drawString(format, (entryWidth / 0.75) - 28, 0, 0xAAAAAA, false);
            render.pose().popPose();
        }

        private void drawTexture(GuiRender render, double xMin, double yMin, double xMax, double yMax) {
            RenderSystem.setShaderTexture(0, TextureCache.loadPreview(capture));
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            Matrix4f matrix4f = render.pose().last().pose();
            BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferbuilder.vertex(matrix4f, (float) xMin, (float) yMin, 0).uv(0, 0).endVertex();
            bufferbuilder.vertex(matrix4f, (float) xMin, (float) yMax, 0).uv(0, 1).endVertex();
            bufferbuilder.vertex(matrix4f, (float) xMax, (float) yMax, 0).uv(1, 1).endVertex();
            bufferbuilder.vertex(matrix4f, (float) xMax, (float) yMin, 0).uv(1, 0).endVertex();
            bufferbuilder.end();
            BufferUploader.end(bufferbuilder);
        }
    }

    //TODO Add ability to scale GuiText so i can remove this
    private static class HeaderInfo extends GuiElement<HeaderInfo> implements BackgroundRender {
        public HeaderInfo(@NotNull GuiParent<?> parent) {
            super(parent);
        }

        @Override
        public void renderBehind(GuiRender render, double mouseX, double mouseY, float partialTicks) {
            if (HistoryManager.instance.isDownloadError()) {
                render.drawCenteredString(new TranslatableComponent("gui.blockshot.history.download_error"), xSize() / 2, 15, 0xFF0000);
            } else {
                render.pose().pushPose();
                render.pose().translate(10, 2, 0);
                render.pose().scale(0.75F, 0.75F, 0.75F);
                render.drawString(new TranslatableComponent("gui.blockshot.history.how_to_screenshot", mc().options.keyScreenshot.getTranslatedKeyMessage()), 0, 0, 0x707070);
                render.drawString(new TranslatableComponent("gui.blockshot.history.how_to_record", mc().options.keyScreenshot.getTranslatedKeyMessage()), 0, 10, 0x707070);
                render.pose().popPose();
            }
        }
    }
}
