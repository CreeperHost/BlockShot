package net.creeperhost.blockshot.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.creeperhost.blockshot.BlockShot;
import net.creeperhost.blockshot.Config;
import net.creeperhost.blockshot.WebUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.GuiScrollingList;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BlockShotHistoryScreen extends GuiScreen {
    private static final int ID_COPY_URL = 8008135;
    private static final int ID_DELETE = 8008136;
    private static final int ID_VIEW = 8008137;
    private static final int ID_BACK = 8008138;
    private static final int ID_OWNER = 8008139;
    private static final int ID_UPLOAD_MODE = 8008140;
    private static final int ID_COPY_CREATED = 8008141;
    private static final int ID_BUTTON_POS = 8008142;
    private static final int ID_REFRESH = 8008143;

    private final List<ScreencapListItem> caps = Collections.synchronizedList(new ArrayList<ScreencapListItem>());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    private GuiButton deleteButton;
    private GuiButton viewButton;
    private GuiButton copyButton;
    private GuiButton ownerButton;
    private GuiButton uploadModeButton;
    private GuiButton copyCreatedButton;
    private GuiButton buttonPosButton;
    private BlockShotHistoryList list;
    public GuiScreen parent;

    private boolean isLoading = true;
    private boolean hasRequested = false;
    private boolean downloadError = false;
    private int ticks = 0;
    private long whenClick;
    private ScreencapListItem lastSelected;

    public BlockShotHistoryScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        int listWidth = Math.max(220, (int) (this.width * 0.66F));
        if (listWidth > this.width - 130) {
            listWidth = this.width - 130;
        }
        int left = 10;
        list = new BlockShotHistoryList(Minecraft.getMinecraft(), this, listWidth, this.height, 56, this.height - 36, left, 36);
        loadRemote(false);

        int buttonY = this.height - 28;
        this.copyButton = (GuiButton) this.addButton(new GuiButton(ID_COPY_URL, left, buttonY, 72, 20, "Copy URL"));
        this.viewButton = (GuiButton) this.addButton(new GuiButton(ID_VIEW, left + 76, buttonY, 72, 20, "View"));
        this.deleteButton = (GuiButton) this.addButton(new GuiButton(ID_DELETE, left + listWidth - 72, buttonY, 72, 20, "Delete"));

        int settingsX = Math.min(left + listWidth + 14, this.width - 112);
        int settingsY = 56;
        this.ownerButton = (GuiButton) this.addButton(new GuiButton(ID_OWNER, settingsX, settingsY, 102, 20, ""));
        settingsY += 24;
        this.uploadModeButton = (GuiButton) this.addButton(new GuiButton(ID_UPLOAD_MODE, settingsX, settingsY, 102, 20, ""));
        settingsY += 24;
        this.copyCreatedButton = (GuiButton) this.addButton(new GuiButton(ID_COPY_CREATED, settingsX, settingsY, 102, 20, ""));
        settingsY += 24;
        this.buttonPosButton = (GuiButton) this.addButton(new GuiButton(ID_BUTTON_POS, settingsX, settingsY, 102, 20, ""));
        settingsY += 34;
        this.addButton(new GuiButton(ID_REFRESH, settingsX, settingsY, 102, 20, "Refresh"));
        this.addButton(new GuiButton(ID_BACK, settingsX, this.height - 28, 102, 20, "Done"));

        this.copyButton.enabled = false;
        this.deleteButton.enabled = false;
        this.viewButton.enabled = false;
        updateSettingsButtons();
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        list.drawScreen(mouseX, mouseY, partialTicks);
        drawCenteredString(this.fontRenderer, "BlockShot", width / 2, 16, 0xFFFFFF);
        drawString(this.fontRenderer, "Upload History", list.listLeft, 42, 0xFFFFFF);
        drawString(this.fontRenderer, "Settings", Math.min(list.listLeft + list.listWidth + 14, this.width - 112), 42, 0xFFFFFF);

        ScreencapListItem selected = list.getCurrSelected();
        if (selected != lastSelected) {
            boolean enabled = selected != null && !selected.isDeleting;
            this.copyButton.enabled = enabled;
            this.deleteButton.enabled = enabled;
            this.viewButton.enabled = enabled;
            lastSelected = selected;
        }

        if (downloadError) {
            drawCenteredString(this.fontRenderer, "Unable to download BlockShot history.", list.listLeft + list.listWidth / 2, 72, 0xFF5555);
        } else if (!isLoading && caps.isEmpty()) {
            drawCenteredString(this.fontRenderer, "No BlockShot uploads found.", list.listLeft + list.listWidth / 2, 72, 0xAAAAAA);
        }

        if (isLoading) {
            ticks++;
            LoadingSpinner.render(partialTicks, ticks, width, height - 20, new ItemStack(Items.COOKED_BEEF));
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void openWebLink(URI url) {
        try {
            Class<?> oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop").invoke((Object) null);
            oclass.getMethod("browse", URI.class).invoke(object, url);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        long now = System.currentTimeMillis();
        if (whenClick + 200 > now) return;
        whenClick = now;

        ScreencapListItem selected = list.getCurrSelected();
        switch (button.id) {
            case ID_COPY_URL:
                if (selected != null) selected.copyUrl();
                break;
            case ID_DELETE:
                if (selected != null) delete(selected);
                break;
            case ID_VIEW:
                if (selected != null) selected.openUrl(this);
                break;
            case ID_BACK:
                Minecraft.getMinecraft().displayGuiScreen(parent);
                break;
            case ID_OWNER:
                Config.INSTANCE.anonymous = !Config.INSTANCE.anonymous;
                saveConfig();
                updateSettingsButtons();
                refreshHistory();
                break;
            case ID_UPLOAD_MODE:
                Config.INSTANCE.cycleUploadMode();
                saveConfig();
                updateSettingsButtons();
                break;
            case ID_COPY_CREATED:
                Config.INSTANCE.copyToClipboard = !Config.INSTANCE.copyToClipboard;
                saveConfig();
                updateSettingsButtons();
                break;
            case ID_BUTTON_POS:
                Config.INSTANCE.cycleButtonPos();
                saveConfig();
                updateSettingsButtons();
                break;
            case ID_REFRESH:
                refreshHistory();
                break;
        }
    }

    private void saveConfig() {
        Config.saveConfigToFile(BlockShot.configLocation.toFile());
    }

    private void updateSettingsButtons() {
        String owner = Config.INSTANCE.anonymous ? "Anonymous" : Minecraft.getMinecraft().getSession().getUsername();
        this.ownerButton.displayString = "Owner: " + owner;
        this.uploadModeButton.displayString = "Upload: " + Config.INSTANCE.uploadModeName();
        this.copyCreatedButton.displayString = "Copy Link: " + (Config.INSTANCE.copyToClipboard ? "On" : "Off");
        this.buttonPosButton.displayString = "Button: " + Config.INSTANCE.buttonPosName();
    }

    private void refreshHistory() {
        synchronized (caps) {
            caps.clear();
        }
        lastSelected = null;
        hasRequested = false;
        copyButton.enabled = false;
        deleteButton.enabled = false;
        viewButton.enabled = false;
        loadRemote(true);
    }

    private void delete(final ScreencapListItem selected) {
        selected.isDeleting = true;
        copyButton.enabled = false;
        deleteButton.enabled = false;
        viewButton.enabled = false;
        isLoading = true;
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                WebUtils.delete("https://blocks.hot/api/v1/shares/" + selected.id);
                synchronized (caps) {
                    caps.remove(selected);
                }
                selected.isDeleting = false;
                isLoading = false;
            }
        });
    }

    private void loadRemote(final boolean force) {
        if (!force && hasRequested) {
            return;
        }
        isLoading = true;
        hasRequested = true;
        downloadError = false;
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                List<ScreencapListItem> loaded = new ArrayList<ScreencapListItem>();
                try {
                    String rsp = WebUtils.get("https://blocks.hot/api/v1/list/1");
                    if (!rsp.equals("error") && rsp.startsWith("{")) {
                        JsonObject asJsonObject = new JsonParser().parse(rsp).getAsJsonObject();
                        JsonArray images = asJsonObject.get("results").getAsJsonArray();
                        for (JsonElement obj : images) {
                            loaded.add(ScreencapListItem.fromJson(obj.getAsJsonObject()));
                        }
                        Collections.sort(loaded, new Comparator<ScreencapListItem>() {
                            @Override
                            public int compare(ScreencapListItem a, ScreencapListItem b) {
                                return Long.compare(b.created, a.created);
                            }
                        });
                        synchronized (caps) {
                            caps.clear();
                            caps.addAll(loaded);
                        }
                    } else {
                        downloadError = true;
                    }
                } catch (Throwable throwable) {
                    downloadError = true;
                    throwable.printStackTrace();
                }
                isLoading = false;
            }
        });
    }

    static class ScreencapListItem {
        String id;
        String format;
        long created;
        boolean isDeleting;
        boolean selected;
        DynamicTexture icon;
        ResourceLocation resource;
        BufferedImage previewImage;

        static ScreencapListItem fromJson(JsonObject obj) {
            ScreencapListItem item = new ScreencapListItem();
            item.id = obj.get("code").getAsString();
            JsonObject fileMeta = obj.get("fileMeta").getAsJsonObject();
            item.format = fileMeta.has("type") ? fileMeta.get("type").getAsString() : "image";
            item.created = parseCreated(obj.get("created").getAsString());
            item.previewImage = WebUtils.getImageFromUrl("https://blocks.hot/api/v1/shares/" + item.id + "/preview/smol");
            return item;
        }

        private static long parseCreated(String created) {
            try {
                return OffsetDateTime.parse(created).toEpochSecond();
            } catch (Throwable ignored) {
            }
            try {
                return Long.parseLong(created);
            } catch (Throwable ignored) {
            }
            return 0;
        }

        public void openUrl(BlockShotHistoryScreen screen) {
            URL url;
            try {
                url = new URL(publicUrl());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return;
            }
            try {
                screen.openWebLink(url.toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        public void copyUrl() {
            GuiScreen.setClipboardString(publicUrl());
        }

        public String publicUrl() {
            return "https://blocks.hot/" + this.id;
        }
    }

    class BlockShotHistoryList extends GuiScrollingList {
        BlockShotHistoryScreen parent;
        int listWidth;
        int listLeft;

        public BlockShotHistoryList(Minecraft client, BlockShotHistoryScreen parent, int width, int height, int top, int bottom, int left, int entryHeight) {
            super(client, width, height, top, bottom, left, entryHeight);
            this.parent = parent;
            this.listWidth = width;
            this.listLeft = left;
        }

        @Override
        protected int getSize() {
            synchronized (parent.caps) {
                return parent.caps.size();
            }
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            ScreencapListItem wanted = getItem(index);
            synchronized (parent.caps) {
                for (ScreencapListItem item : parent.caps) {
                    item.selected = item.id.equals(wanted.id);
                }
            }
            if (doubleClick) {
                wanted.openUrl(parent);
            }
        }

        @Override
        protected boolean isSelected(int index) {
            return getItem(index).selected;
        }

        @Override
        protected void drawBackground() {
        }

        @Override
        protected void drawSlot(int slotIdx, int entryRight, int slotTop, int slotBuffer, Tessellator tess) {
            ScreencapListItem item = getItem(slotIdx);
            drawIcon(item, this.listLeft + 5, slotTop);
            Date date = item.created > 0 ? new Date(item.created * 1000L) : new Date();
            drawString(Minecraft.getMinecraft().fontRenderer, dateFormat.format(date), this.listLeft + 42, slotTop, 0xFFFFFF);
            drawString(Minecraft.getMinecraft().fontRenderer, item.publicUrl(), this.listLeft + 42, slotTop + 10, 0xFFFFFF);
            drawString(Minecraft.getMinecraft().fontRenderer, item.format, this.listLeft + 42, slotTop + 20, 0xAAAAAA);
        }

        public ScreencapListItem getCurrSelected() {
            synchronized (parent.caps) {
                for (ScreencapListItem item : parent.caps) {
                    if (item.selected) return item;
                }
            }
            return null;
        }

        private ScreencapListItem getItem(int index) {
            synchronized (parent.caps) {
                return parent.caps.get(index);
            }
        }

        private void drawIcon(ScreencapListItem item, int slotX, int slotY) {
            if (item.resource == null) {
                BufferedImage bufferedimage = item.previewImage;
                if (bufferedimage != null) {
                    item.icon = new DynamicTexture(bufferedimage.getWidth(), bufferedimage.getHeight());
                    item.resource = new ResourceLocation("blockshot/" + item.id);
                    Minecraft.getMinecraft().getTextureManager().loadTexture(item.resource, item.icon);
                    bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), item.icon.getTextureData(), 0, bufferedimage.getWidth());
                    item.icon.updateDynamicTexture();
                }
            }
            if (item.resource == null) {
                item.resource = new ResourceLocation("textures/misc/unknown_server.png");
            }
            GlStateManager.pushMatrix();
            Minecraft.getMinecraft().getTextureManager().bindTexture(item.resource);
            GlStateManager.enableBlend();
            Gui.drawModalRectWithCustomSizedTexture(slotX, slotY, 0.0F, 0.0F, 32, 32, 32.0F, 32.0F);
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }
}
