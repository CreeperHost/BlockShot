package net.creeperhost.blockshot.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.creeperhost.blockshot.WebUtils;
import net.creeperhost.polylib.client.screen.widget.LoadingSpinner;
import net.creeperhost.polylib.client.screen.widget.ScreenList;
import net.creeperhost.polylib.client.screen.widget.ScreenListEntry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class BlockShotHistoryScreen extends Screen {
    private Button deleteButton;
    private Button viewButton;
    private Button copyButton;
    private ScreenList<BlockShotHistoryEntry> list;
    public Screen parent;

    public BlockShotHistoryScreen(Screen parent) {
        super(Component.literal("BlockShot Upload History"));
        this.parent = parent;
        if (caps.get() == null) caps.getAndSet(new ArrayList<ScreencapListItem>());
    }

    boolean isLoading = true;

    @Override
    protected void init() {
        list = new ScreenList(this, this.minecraft, this.width, this.height, 56, this.height - 36, 36);
        this.loadRemote().thenRun(() -> isLoading = false);
        this.addRenderableWidget(list);
        this.copyButton = (Button) this.addRenderableWidget(new Button(this.width / 2 - (76 * 2), this.height - 28, 72, 20, Component.translatable("Copy URL"), (arg) -> {
            list.getCurrSelected().copyUrl();
        }));
        this.deleteButton = (Button) this.addRenderableWidget(new Button(this.width / 2 - 76, this.height - 28, 72, 20, Component.translatable("selectWorld.delete"), (arg) -> {
            try {
                this.copyButton.active = false;
                this.deleteButton.active = false;
                this.viewButton.active = false;
                CompletableFuture.runAsync(() -> {
                    isLoading = true;
                    list.getCurrSelected().delete();
                    caps.getAndUpdate((a) -> {
                        a.clear();
                        return a;
                    });
                    hasRequested = false;
                    this.loadRemote().thenRun(() -> isLoading = false);
                }).thenRun(() -> {
                });
            } catch (Exception ignored) {
            }
        }));
        this.viewButton = (Button) this.addRenderableWidget(new Button(this.width / 2, this.height - 28, 72, 20, Component.literal("View"), (arg) -> {
            list.getCurrSelected().openUrl();
        }));
        this.addRenderableWidget(new Button(this.width / 2 + 76, this.height - 28, 72, 20, CommonComponents.GUI_CANCEL, (arg) -> {
            this.minecraft.setScreen(this.parent);
        }));
        this.copyButton.active = false;
        this.deleteButton.active = false;
        this.viewButton.active = false;
    }

    int ticks = 0;
    BlockShotHistoryEntry lastSelected;

    @Override
    public void render(PoseStack poseStack, int i, int j, float f) {
        this.renderBackground(poseStack);
        super.render(poseStack, i, j, f);
        if (list.getCurrSelected() != null && list.getCurrSelected() != lastSelected) {
            this.copyButton.active = !list.getCurrSelected().isDeleting;
            this.deleteButton.active = !list.getCurrSelected().isDeleting;
            this.viewButton.active = !list.getCurrSelected().isDeleting;
            lastSelected = list.getCurrSelected();
        }
        if (isLoading) {
            ticks++;
            LoadingSpinner.render(poseStack, f, ticks, width / 2, height / 2, new ItemStack(Items.COOKED_BEEF));
        }
        drawCenteredString(poseStack, font, this.getTitle(), width / 2, 18, 0xFFFFFF);
    }

    AtomicReference<List<ScreencapListItem>> caps = new AtomicReference<>();
    private boolean hasRequested = false;

    private CompletableFuture<?> loadRemote() {
        return CompletableFuture.runAsync(() -> {
            if (caps.get().size() == 0 && !hasRequested) {
                isLoading = true;
                hasRequested = true;
                String rsp = WebUtils.getWebResponse("https://blockshot.ch/list");
                if (!rsp.equals("error")) {
                    JsonElement jsonElement = JsonParser.parseString(rsp);
                    JsonArray images = jsonElement.getAsJsonArray();
                    for (JsonElement obj : images) {
                        ScreencapListItem item = new ScreencapListItem();
                        item.id = obj.getAsJsonObject().get("id").getAsString();
                        item.preview = obj.getAsJsonObject().get("preview").getAsString();
                        item.created = obj.getAsJsonObject().get("created").getAsLong();
                        caps.getAndUpdate((a) -> {
                            a.add(item);
                            return a;
                        });
                    }
                } else {
                    //Used only in dev to help, as the list should stay 0 in prod otherwise it'll break for strange reasons.
                    if (caps.get().size() == 0) {
                        ScreencapListItem item = new ScreencapListItem();
                        item.id = "BlockShot not available in offline mode.";
                        item.preview = "";
                        item.isDeleting = true;
                        item.created = 0;
                        caps.getAndUpdate((a) -> {
                            a.add(item);
                            return a;
                        });
                    }
                }
            }
            list.children().clear();
            List<ScreencapListItem> localCaps = new ArrayList<>(caps.get());
            for (ScreencapListItem c : localCaps) {
                BlockShotHistoryEntry entry = new BlockShotHistoryEntry(list, c.id, c.preview, c.created, c.isDeleting);
                list.children().add(entry);
            }
        });
    }

    class ScreencapListItem {
        String id;
        String preview;
        long created;
        boolean isDeleting;
    }

    public class BlockShotHistoryEntry extends ScreenListEntry
    {
        String id;
        String preview;
        long created;
        ScreenList parent;
        boolean isDeleting = false;

        public BlockShotHistoryEntry(ScreenList list, String id, String preview, long created, boolean isDeleting) {
            super(list);
            parent = list;
            this.id = id;
            this.isDeleting = isDeleting;
            this.preview = preview;
            this.created = created;
        }

        @Override
        public void render(PoseStack poseStack, int slotIndex, int y, int x, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float p_render_9_) {
            Date date = this.created > 0 ? new java.util.Date(this.created * 1000L) : new java.util.Date();
            SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            this.mc.font.draw(poseStack, sdf.format(date), x + 35, y, 16777215);
            if (this.isDeleting) {
                if (this.id.length() < 10) {
                    this.mc.font.draw(poseStack, "Pending deletion...", x + 35, y + 10, 8421504);
                } else {
                    this.mc.font.draw(poseStack, this.id, x + 35, y + 10, 8421504);
                }
            } else {
                if (this.id.length() < 10) {
                    this.mc.font.draw(poseStack, "https://blockshot.ch/" + this.id, x + 35, y + 10, 8421504);
                }
            }
            this.drawIcon(poseStack, x, y, getPreview());
        }

        public void delete() {
            isDeleting = true;
            caps.getAndUpdate((a) -> {
                for (ScreencapListItem c : a) {
                    if (c.id == this.id) {
                        c.isDeleting = true;
                        break;
                    }
                }
                return a;
            });
            WebUtils.getWebResponse("https://blockshot.ch/delete/" + this.id);
        }

        public void openUrl() {
            URL url = null;
            try {
                url = new URL("https://blockshot.ch/" + this.id);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return;
            }
            if (url != null) Util.getPlatform().openUrl(url);
        }

        public void copyUrl() {
            Minecraft.getInstance().keyboardHandler.setClipboard("https://blockshot.ch/" + this.id);
        }

        private boolean previewLoading = false;
        private boolean previewLoaded = false;
        private ResourceLocation _resource;

        public ResourceLocation getPreview() {
            if (this.preview == null || this.preview.length() == 0 || this.created == 0)
                return new ResourceLocation("textures/misc/unknown_server.png");
            try {
                if (previewLoading) return new ResourceLocation("textures/misc/unknown_server.png");
                if (!previewLoaded) {
                    previewLoading = true;
                    NativeImage image = NativeImage.fromBase64(this.preview);
                    DynamicTexture i = (new DynamicTexture(image));
                    _resource = Minecraft.getInstance().getTextureManager().register("blockshot/", i);
                    //i.close();
                    previewLoaded = true;
                    previewLoading = false;
                } else {
                    return _resource;
                }
            } catch (Throwable t) {
                t.printStackTrace();
                previewLoading = false;
                previewLoaded = true;//Let's not retry...
            }
            return new ResourceLocation("textures/misc/unknown_server.png");
        }

        protected void drawIcon(PoseStack poseStack, int i, int j, ResourceLocation resourceLocation) {
            if (resourceLocation == null) resourceLocation = new ResourceLocation("textures/misc/unknown_server.png");
            RenderSystem.setShaderTexture(0, resourceLocation);
            RenderSystem.enableBlend();
            GuiComponent.blit(poseStack, i, j, 0.0F, 0.0F, 32, 32, 32, 32);
            RenderSystem.disableBlend();
        }
    }

}
