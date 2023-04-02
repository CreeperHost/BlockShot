package net.creeperhost.blockshot.gui;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.WebUtils;
import net.creeperhost.polylib.client.screen.widget.LoadingSpinner;
import net.creeperhost.polylib.client.screen.widget.ScreenList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BlockShotHistoryScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger();
    private ScreenList<BlockShotListEntry> screenList;
    private List<ScreenCapInfo> captureList = new ArrayList<>();
    private BlockShotListEntry lastSelected;
    private Button deleteButton;
    private Button viewButton;
    private Button copyButton;
    private Screen parent;
    private int ticks = 0;

    //True if currently downloading captures
    private boolean isLoading = true;
    //True if captures have been downloaded and do not need to be updated.
    private boolean capturesValid = false;
    private boolean downloadError = false;
    private AtomicDouble downloadProgress = new AtomicDouble(-1);

    private List<CompletableFuture<FutureTask>> activeTasks = new ArrayList<>();

    public BlockShotHistoryScreen(Screen parent) {
        super(Component.translatable("gui.blockshot.history.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        screenList = addRenderableWidget(new ScreenList<>(this, minecraft, width, height, 30, height - 36, 36));

        copyButton = addRenderableWidget(new Button(width / 2 - (76 * 2), height - 28, 72, 20, Component.translatable("gui.blockshot.history.copy_url"), e -> screenList.getCurrSelected().copyUrl()));
        deleteButton = addRenderableWidget(new Button(width / 2 - 76, height - 28, 72, 20, Component.translatable("gui.blockshot.history.delete"), e -> deleteSelected()));
        viewButton = addRenderableWidget(new Button(width / 2, height - 28, 72, 20, Component.translatable("gui.blockshot.history.view"), (arg) -> screenList.getCurrSelected().openUrl()));
        addRenderableWidget(new Button(this.width / 2 + 76, this.height - 28, 72, 20, CommonComponents.GUI_BACK, (arg) -> this.minecraft.setScreen(this.parent)));

        setButtons(false);

        IconButton gearButton = addRenderableWidget(new IconButton(width - 28, height - 28, 20, 20, null, e -> minecraft.setScreen(new BlockShotSettingsScreen(this)), (button, poseStack, i, j) -> renderTooltip(poseStack, Component.translatable("gui.blockshot.settings.open.info"), i, j)));
        gearButton.setIcon(new ResourceLocation(BlockShot.MOD_ID, "textures/gui/gear_icon.png"), 16, 16);

        loadRemote(false);
    }

    private void loadRemote(boolean forceUpdate) {
        if (Config.INSTANCE.anonymous) {
            capturesValid = false;
            isLoading = false;
            return;
        }
        if (capturesValid && !forceUpdate) {
            if (!isLoading && !captureList.isEmpty()) {
                loadCaptures();
            }
            return;
        }
        isLoading = true;
        capturesValid = true;
        downloadError = false;
        activeTasks.add(CompletableFuture.supplyAsync(() -> new DownloadTask().runOffThread()));
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_DELETE && deleteButton.active) {
            deleteSelected();
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    private void deleteSelected() {
        setButtons(false);
        ScreenCapInfo capInfo = screenList.getCurrSelected().capInfo;
        capInfo.deleting = true;
        activeTasks.add(CompletableFuture.supplyAsync(() -> new DeleteTask(capInfo).runOffThread()));
    }

    private void loadCaptures() {
        screenList.clearList();
        for (ScreenCapInfo capInfo : captureList) {
            screenList.add(new BlockShotListEntry(screenList, capInfo));
        }
        screenList.setScrollAmount(0);
    }

    @Override
    public void tick() {
        super.tick();
        if (activeTasks.isEmpty()) return;

        List<CompletableFuture<FutureTask>> completed = new ArrayList<>();
        for (CompletableFuture<FutureTask> task : activeTasks) {
            if (!task.isDone()) continue;
            completed.add(task);
        }

        for (CompletableFuture<FutureTask> task : completed) {
            if (!task.isCompletedExceptionally()) {
                try {
                    task.get().completeOnThread();
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("An error occurred while processing task", e);
                }
            }
        }
        activeTasks.removeAll(completed);
    }

    private void setButtons(boolean active) {
        this.copyButton.active = active;
        this.deleteButton.active = active;
        this.viewButton.active = active;
    }

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        super.render(poseStack, i, j, f);
        if (screenList.getCurrSelected() != null && screenList.getCurrSelected() != lastSelected) {
            setButtons(!screenList.getCurrSelected().capInfo.deleting);
            lastSelected = screenList.getCurrSelected();
        }

        if (isLoading) {
            ticks++;
            LoadingSpinner.render(poseStack, f, ticks, width / 2, height / 2, new ItemStack(Items.COOKED_BEEF));

            if (downloadProgress.get() != -1) {
                if (downloadProgress.get() > 1) {
                    drawCenteredString(poseStack, font, Component.literal(Math.round(downloadProgress.get() / 1000) + "KB"), width / 2, (height / 2) + 50, 0xFFFFFF);
                } else {
                    drawCenteredString(poseStack, font, Component.literal(Math.round(downloadProgress.get() * 100) + "%"), width / 2, (height / 2) + 50, 0xFFFFFF);
                }
            }
        }
        drawCenteredString(poseStack, font, this.getTitle(), width / 2, 15 - 4, 0xFFFFFF);

        if (downloadError) {
            drawCenteredString(poseStack, font, Component.translatable("gui.blockshot.history.download_error"), width / 2, 11 + 10, 0xFF0000);
        } else {
            poseStack.pushPose();
            poseStack.translate(5, 15, 0);
            poseStack.scale(0.75F, 0.75F, 0.75F);
            drawString(poseStack, font, Component.translatable("gui.blockshot.history.how_to_screenshot", minecraft.options.keyScreenshot.getTranslatedKeyMessage()), 0, 0, 0xFFFFFF);
            drawString(poseStack, font, Component.translatable("gui.blockshot.history.how_to_record", minecraft.options.keyScreenshot.getTranslatedKeyMessage()), 0, 10, 0xFFFFFF);
            poseStack.popPose();
        }

        if (Config.INSTANCE.anonymous) {
            drawCenteredString(poseStack, font, Component.translatable("gui.blockshot.history.not_in_anon_mode"), width / 2, 11 + 50, 0xFF0000);
        }
    }

    private abstract static class FutureTask {
        public abstract FutureTask runOffThread();

        public abstract void completeOnThread();
    }

    private class DownloadTask extends FutureTask {
        private List<ScreenCapInfo> captures = new ArrayList<>();
        private boolean errored = false;

        @Override
        public DownloadTask runOffThread() {
            captures = new ArrayList<>();
            downloadProgress.set(-1);
            String rsp = WebUtils.get("https://blockshot.ch/list", downloadProgress);
            if (!rsp.equals("error")) {
                JsonElement jsonElement = JsonParser.parseString(rsp);
                JsonArray images = jsonElement.getAsJsonArray();
                for (JsonElement obj : images) {
                    captures.add(new ScreenCapInfo(obj.getAsJsonObject()));
                }
            } else {
                errored = true;
            }
            downloadProgress.set(-1);
            return this;
        }

        @Override
        public void completeOnThread() {
            downloadError = errored;
            isLoading = false;
            captureList.clear();
            captureList.addAll(captures);
            loadCaptures();
        }
    }

    private class DeleteTask extends FutureTask {
        private final ScreenCapInfo capInfo;

        public DeleteTask(ScreenCapInfo capInfo) {
            this.capInfo = capInfo;
        }

        @Override
        public DeleteTask runOffThread() {
            WebUtils.get("https://blockshot.ch/delete/" + capInfo.id, null);
            return this;
        }

        @Override
        public void completeOnThread() {
            captureList.remove(capInfo);
            for (ScreenCapInfo screenCapInfo : captureList) {
                if (screenCapInfo.deleting) return;
            }
            loadRemote(true);
        }
    }
}
