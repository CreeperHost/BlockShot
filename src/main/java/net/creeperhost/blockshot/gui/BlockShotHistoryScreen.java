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
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.GuiScrollingList;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class BlockShotHistoryScreen extends GuiScreen {
    private GuiButton deleteButton;
    private GuiButton viewButton;
    private GuiButton copyButton;
    private BlockShotHistoryList list;
    public GuiScreen parent;

    public BlockShotHistoryScreen(GuiScreen parent) {
        this.parent = parent;
        if (caps.get() == null) caps.getAndSet(new ArrayList<ScreencapListItem>());
    }

    boolean isLoading = true;

    @Override
    public void initGui() {
        int pad = (this.width/3);
        list = new BlockShotHistoryList(Minecraft.getMinecraft(), this, this.width-pad, this.height, 56, this.height - 36, pad/2, 36);
        this.loadRemote().thenRun(() -> isLoading = false);
        this.copyButton = (GuiButton) this.addButton(new GuiButton(8008135, this.width / 2 - (76 * 2), this.height - 28, 72, 20, "Copy URL"));
        this.deleteButton = (GuiButton) this.addButton(new GuiButton(8008136, this.width / 2 - 76, this.height - 28, 72, 20, "Delete"));
        this.viewButton = (GuiButton) this.addButton(new GuiButton(8008137,this.width / 2, this.height - 28, 72, 20, "View"));
        this.addButton(new GuiButton(8008138,this.width / 2 + 76, this.height - 28, 72, 20, "Cancel"));
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
            LoadingSpinner.render(partialTicks, ticks, width, height-20, new ItemStack(Items.COOKED_BEEF));
        }
        super.drawScreen(mouseX,mouseY,partialTicks);
    }
    private long whenClick;
    private ScreencapListItem lastSelected;
    private void openWebLink(URI url)
    {
        try
        {
            Class<?> oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop").invoke((Object)null);
            oclass.getMethod("browse", URI.class).invoke(object, url);
        }
        catch (Throwable throwable)
        {
            throwable.printStackTrace();
        }
    }
    @Override
    protected void actionPerformed(GuiButton button)
    {
        if(whenClick == (System.currentTimeMillis() / 1000)) return;
        whenClick = (System.currentTimeMillis() / 1000);
        switch(button.id)
        {
            case 8008135:
                list.getCurrSelected().copyUrl();
                break;
            case 8008136:
                try {
                    this.copyButton.enabled = false;
                    this.deleteButton.enabled = false;
                    this.viewButton.enabled = false;
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
                break;
            case 8008137:
                list.getCurrSelected().openUrl(this);
                break;
            case 8008138:
                Minecraft.getMinecraft().displayGuiScreen(parent);
                break;
        }
    }

    int ticks = 0;

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
        });
    }
    class ScreencapListItem {
        String id;
        String preview;
        long created;
        boolean isDeleting;
        boolean selected;
        DynamicTexture icon;
        ResourceLocation resource;
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
        public void openUrl(BlockShotHistoryScreen screen) {
            URL url = null;
            try {
                url = new URL("https://blockshot.ch/" + this.id);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return;
            }
            if (url != null)
            {
                try {
                    screen.openWebLink(url.toURI());
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }

        }
        public void copyUrl() {
            GuiScreen.setClipboardString("https://blockshot.ch/" + this.id);
        }
    }
    class BlockShotHistoryList extends GuiScrollingList {
        BlockShotHistoryScreen parent;
        public BlockShotHistoryList(Minecraft client, BlockShotHistoryScreen parent, int width, int height, int top, int bottom, int left, int entryHeight) {
            super(client, width, height, top, bottom, left, entryHeight);
            this.parent = parent;
        }

        @Override
        protected int getSize() {
            return parent.caps.get().size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            parent.caps.getAndUpdate((a) -> {
                ScreencapListItem wanted = a.get(index);
                for(ScreencapListItem i : a)
                {
                    if(i.id != wanted.id)
                    {
                        i.selected = false;
                    }
                }
                wanted.selected = true;
                return a;
            });
        }

        @Override
        protected boolean isSelected(int index) {
            return parent.caps.get().get(index).selected;
        }

        @Override
        protected void drawBackground() {
        }

        @Override
        protected void drawSlot(int slotIdx, int entryRight, int slotTop, int slotBuffer, Tessellator tess) {


            drawIcon(parent.caps.get().get(slotIdx), this.left+5, slotTop);
            Date date = parent.caps.get().get(slotIdx).created > 0 ? new java.util.Date(parent.caps.get().get(slotIdx).created * 1000L) : new java.util.Date();
            SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            drawString(Minecraft.getMinecraft().fontRenderer, sdf.format(date), this.left+42, slotTop,  0xFFFFFF);
            drawString(Minecraft.getMinecraft().fontRenderer, "https://blockshot.ch/"+parent.caps.get().get(slotIdx).id, this.left+42, slotTop+10,  0xFFFFFF);
        }

        public ScreencapListItem getCurrSelected()
        {
            List<ScreencapListItem> l = new ArrayList<ScreencapListItem>(parent.caps.get());
            for(ScreencapListItem i : l)
            {
                if(i.selected == true) return i;
            }
            return null;
        }

        private void drawIcon(ScreencapListItem item, int slotX, int slotY) {
            if(item.resource == null) {
                try {
                    ByteBuf bytebuf = Unpooled.copiedBuffer((CharSequence) item.preview, StandardCharsets.UTF_8);
                    ByteBuf bytebuf1 = null;
                    BufferedImage bufferedimage;
                    bytebuf1 = Base64.decode(bytebuf);

                    bufferedimage = TextureUtil.readBufferedImage(new ByteBufInputStream(bytebuf1));

                    bytebuf.release();

                    if (bytebuf1 != null) {
                        bytebuf1.release();
                    }
                    item.icon = new DynamicTexture(bufferedimage.getWidth(), bufferedimage.getHeight());
                    item.resource = new ResourceLocation("blockshot/"+item.id);
                    Minecraft.getMinecraft().getTextureManager().loadTexture(item.resource, item.icon);
                    bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), item.icon.getTextureData(), 0, bufferedimage.getWidth());
                    item.icon.updateDynamicTexture();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(item.resource == null)
            {
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
