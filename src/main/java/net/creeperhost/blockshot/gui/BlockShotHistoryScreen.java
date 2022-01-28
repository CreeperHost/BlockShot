package net.creeperhost.blockshot.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import net.creeperhost.blockshot.WebUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.apache.commons.lang3.Validate;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class BlockShotHistoryScreen extends GuiScreen {
    private GuiButton deleteButton;
    private GuiButton viewButton;
    private GuiButton copyButton;
    private GuiButton cancelButton;
    private BlockShotHistoryList list;
    public GuiScreen parent;

    public BlockShotHistoryScreen(GuiScreen parent) {
        this.parent = parent;
        if (caps.get() == null) caps.getAndSet(new ArrayList<ScreencapListItem>());
    }

    boolean isLoading = true;

    @Override
    public void initGui() {
        list = new BlockShotHistoryList(Minecraft.getMinecraft(), this.width, this.height, 56, this.height - 36, 36);
        this.loadRemote().thenRun(() -> isLoading = false);
        this.copyButton = (GuiButton) this.addButton(new GuiButton(8008135, this.width / 2 - (76 * 2), this.height - 28, 72, 20, "Copy URL"));
        this.deleteButton = (GuiButton) this.addButton(new GuiButton(8008136, this.width / 2 - 76, this.height - 28, 72, 20, "Delete"));
        this.viewButton = (GuiButton) this.addButton(new GuiButton(8008137,this.width / 2, this.height - 28, 72, 20, "View"));
        this.cancelButton = (GuiButton) this.addButton(new GuiButton(8008138,this.width / 2 + 76, this.height - 28, 72, 20, "Cancel"));
        this.copyButton.enabled = false;
        this.deleteButton.enabled = false;
        this.viewButton.enabled = false;
        super.initGui();
    }
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        list.drawScreen(mouseX,mouseY,partialTicks);
        drawCenteredString(this.fontRenderer, "BlockShot Upload History", width / 2, 16, 0xFFFFFF);
        if (list.getCurrSelected() != null && list.getCurrSelected() != lastSelected) {
            this.copyButton.enabled = !list.getCurrSelected().isDeleting;
            this.deleteButton.enabled = !list.getCurrSelected().isDeleting;
            this.viewButton.enabled = !list.getCurrSelected().isDeleting;
            lastSelected = list.getCurrSelected();
        }
        if (isLoading) {
            ticks++;
            //LoadingSpinner.render(f, ticks, width / 2, height / 2, new ItemStack(Items.COOKED_BEEF));
        }
        super.drawScreen(mouseX,mouseY,partialTicks);
    }
    private long whenClick;
    public void handleButton(GuiScreenEvent.ActionPerformedEvent event)
    {
        GuiButton button = event.getButton();
        if(whenClick == (System.currentTimeMillis() / 1000)) return;
        whenClick = (System.currentTimeMillis() / 1000);
        switch(button.id)
        {
            case 8008135:
                //TODO: Copy URL to clipboard
                break;
            case 8008136:
                //TODO: Delete image via API
                break;
            case 8008137:
                //TODO: Open url
                break;
            case 8008138:
                Minecraft.getMinecraft().displayGuiScreen(parent);
                break;
        }
    }

    int ticks = 0;
    BlockShotHistoryEntry lastSelected;


    AtomicReference<List<ScreencapListItem>> caps = new AtomicReference<>();
    private boolean hasRequested = false;

    private CompletableFuture<?> loadRemote() {
        return CompletableFuture.runAsync(() -> {
            if (caps.get().size() == 0 && !hasRequested) {
                isLoading = true;
                hasRequested = true;
                String rsp = WebUtils.getWebResponse("https://blockshot.ch/list");
                if (!rsp.equals("error")) {
                    JsonElement jsonElement = new JsonParser().parse(rsp);
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
            List<ScreencapListItem> localCaps = new ArrayList<ScreencapListItem>(caps.get());
            for (ScreencapListItem c : localCaps) {
                BlockShotHistoryEntry entry = new BlockShotHistoryEntry(list, c.id, c.preview, c.created, c.isDeleting);
                list.add(entry);
            }
        });
    }
    class ScreencapListItem {
        String id;
        String preview;
        long created;
        boolean isDeleting;
    }
    class BlockShotHistoryList extends GuiListExtended {
        List<BlockShotHistoryEntry> l = new ArrayList<BlockShotHistoryEntry>();
        public BlockShotHistoryList(Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn) {
            super(mcIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
        }

        @Override
        public BlockShotHistoryEntry getListEntry(int index) {
            return l.get(index);
        }

        public List<BlockShotHistoryEntry> children()
        {
            return l;
        }

        public boolean add(BlockShotHistoryEntry e)
        {
            l.add(e);
            return true;
        }

        public boolean remove(int index)
        {
            l.remove(index);
            return true;
        }
        public boolean remove(BlockShotHistoryEntry e)
        {
            return l.remove(e);
        }

        public BlockShotHistoryEntry getCurrSelected()
        {
            for(BlockShotHistoryEntry i : l)
            {
                if(i.selected) return i;
            }
            return null;
        }
        @Override
        protected int getSize() {
            return l.size();
        }
    }
    class BlockShotHistoryEntry implements GuiListExtended.IGuiListEntry
    {
        public boolean selected = false;
        String id;
        String preview;
        long created;
        boolean isDeleting = false;
        DynamicTexture icon;
        ResourceLocation rl;

        private BlockShotHistoryList parent;
        public BlockShotHistoryEntry(BlockShotHistoryList parent, String id, String preview, long created, boolean isDeleting)
        {
            super();
            this.parent = parent;
            this.id = id;
            this.isDeleting = isDeleting;
            this.preview = preview;
            this.created = created;
        }
        @Override
        public void updatePosition(int slotIndex, int x, int y, float partialTicks) {

        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
            //Minecraft.getMinecraft().ingameGUI.drawTexturedModalRect(x, y, loadPreview());
        }
        private ResourceLocation loadPreview() {
            if(this.rl == null) {
                ByteBuf bytebuf = Unpooled.copiedBuffer((CharSequence) this.preview, StandardCharsets.UTF_8);
                ByteBuf bytebuf1 = null;
                BufferedImage bufferedimage = null;
                try {
                    bytebuf1 = Base64.decode(bytebuf);
                    bufferedimage = TextureUtil.readBufferedImage(new ByteBufInputStream(bytebuf1));
                    Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide");
                    Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high");
                } catch (Throwable throwable) {
                } finally {
                    bytebuf.release();

                    if (bytebuf1 != null) {
                        bytebuf1.release();
                    }
                }
                if (this.icon == null) {
                    this.icon = new DynamicTexture(bufferedimage.getWidth(), bufferedimage.getHeight());
                    this.rl = new ResourceLocation(this.id);
                    Minecraft.getMinecraft().getTextureManager().loadTexture(rl, this.icon);
                }

                bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), this.icon.getTextureData(), 0, bufferedimage.getWidth());
                this.icon.updateDynamicTexture();
            } else {
                return this.rl;
            }
            return null;
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            return false;
        }


        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            for(BlockShotHistoryEntry n : parent.l) {
                if(n != this) {
                    n.selected = false;
                }
                selected = true;
            }
        }
    }
}
