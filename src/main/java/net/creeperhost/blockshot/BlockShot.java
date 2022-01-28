package net.creeperhost.blockshot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import net.creeperhost.blockshot.gui.BlockShotClickEvent;
import net.creeperhost.blockshot.gui.BlockShotHistoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Mod(modid = BlockShot.MODID, name = BlockShot.NAME, version = BlockShot.VERSION, clientSideOnly = true)
public class BlockShot
{
    public static final String MODID = "blockshot";
    public static final String NAME = "BlockShot";
    public static final String VERSION = "1.2.4";
    public static Path configLocation = null;
    public static final int CHAT_UPLOAD_ID = 360360;
    public static final int CHAT_ENCODING_ID = 420420;
    public static byte[] latest;
    private static boolean _active = false;

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        configLocation = (new File(event.getModConfigurationDirectory().getAbsolutePath() + "/blockshot.json")).toPath();
        Config.init(configLocation.toFile());
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        if (getServerIDAndVerify() != null || ((boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment"))) {
            _active = true;
            MinecraftForge.EVENT_BUS.register(this);
        } else {
            logger.error("BlockShot will not run in offline mode.");
        }
    }
    @SubscribeEvent
    public void onMouseClick(GuiScreenEvent.MouseInputEvent event)
    {
        if(event.getGui() instanceof GuiChat) {
            if (Minecraft.getMinecraft() != null) {
                if (Mouse.isButtonDown(0)) {
                    ITextComponent component = Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());
                    if (component != null) {
                        if (component.getStyle() != null && component.getStyle().getClickEvent() != null) {
                            if (component.getStyle().getClickEvent() instanceof BlockShotClickEvent) {
                                BlockShot.uploadAndAddToChat(BlockShot.latest);
                                event.setCanceled(true);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
    @SubscribeEvent
    public void onScreenShot(ScreenshotEvent event)
    {
        if (BlockShot.isActive()) {
            if (GuiScreen.isCtrlKeyDown()) {
                event.setResultMessage(new TextComponentString(" "));
                event.setCanceled(true);
                if(GifEncoder.isRecording)
                {
                    GifEncoder.isRecording = false;
                } else {
                    GifEncoder.begin();
                }
                return;
            }
            if (Config.INSTANCE.uploadMode != 0) {
                BufferedImage nativeImage = event.getImage();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ImageIO.write(nativeImage, "JPEG", baos);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BlockShot.latest = baos.toByteArray();
                if (Config.INSTANCE.uploadMode == 2) {
                    CompletableFuture.runAsync(() ->
                    {
                        if (BlockShot.latest == null || BlockShot.latest.length == 0) return;
                        BlockShot.uploadAndAddToChat(BlockShot.latest);
                        BlockShot.latest = null;
                    });
                } else {
                    if (BlockShot.latest != null && BlockShot.latest.length > 0) {
                        ITextComponent confirmMessage = new TextComponentString("[BlockShot] Click here to upload this screenshot to BlockShot");
                        if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                            confirmMessage.setStyle(confirmMessage.getStyle().setClickEvent(new BlockShotClickEvent(ClickEvent.Action.RUN_COMMAND, "/blockshot upload")));
                            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(confirmMessage, BlockShot.CHAT_UPLOAD_ID);
                        }
                    }
                }
                event.setResultMessage(new TextComponentString(" "));
                event.setCanceled(true);
            }
        }
    }
    private long whenClick = 0;
    @SubscribeEvent
    public void onGuiEvent(GuiScreenEvent.ActionPerformedEvent event)
    {
        if(event != null) {
            GuiScreen screen = event.getGui();
            if (screen != null) {
                if (screen instanceof GuiOptions) {
                    GuiButton button = event.getButton();
                    if(whenClick == (System.currentTimeMillis() / 1000)) return;
                    whenClick = (System.currentTimeMillis() / 1000);
                    switch(button.id)
                    {
                        case 8008135:
                            if (Config.INSTANCE.uploadMode == 2) {
                                Config.INSTANCE.uploadMode = 0;
                            } else if (Config.INSTANCE.uploadMode == 1) {
                                Config.INSTANCE.uploadMode = 2;
                            } else if (Config.INSTANCE.uploadMode == 0) {
                                Config.INSTANCE.uploadMode = 1;
                            }
                            Config.saveConfigToFile(BlockShot.configLocation.toFile());
                            Minecraft.getMinecraft().displayGuiScreen(screen);
                            break;
                        case 8008136:
                            Config.INSTANCE.anonymous = Config.INSTANCE.anonymous ? false : true;
                            Config.saveConfigToFile(BlockShot.configLocation.toFile());
                            Minecraft.getMinecraft().displayGuiScreen(screen);
                            break;
                        case 8008137:
                            GuiScreen history = new BlockShotHistoryScreen(screen);
                            Minecraft.getMinecraft().displayGuiScreen(history);
                            break;
                    }
                }
            }
        }
    }
    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent event)
    {
        if(event != null) {
            GuiScreen screen = event.getGui();
            if (screen != null) {
                if (screen instanceof GuiOptions) {
                    List<GuiButton> buttons = event.getButtonList();
                    int i = (screen.width / 2 - 155) + 160;
                    int k = (screen.height / 6 - 12) + 30;
                    String value = "Auto";
                    if (Config.INSTANCE.uploadMode == 0) value = "Off";
                    if (Config.INSTANCE.uploadMode == 1) value = "Prompt";
                    String name = "BlockShot Upload: " + value;
                    buttons.add(new GuiButton(8008135, i, k, 150, 20, name));
                    String value2 = "Anonymous";
                    if (!Config.INSTANCE.anonymous) value2 = Minecraft.getMinecraft().getSession().getUsername();
                    String name2 = "BlockShot Owner: " + value2;
                    i -= 160;
                    buttons.add(new GuiButton(8008136, i, k, 150, 20, name2));
                    String name3 = "View BlockShot History";
                    k += 120;
                    GuiButton historyBtn = new GuiButton(8008137, i, k, 150, 20, name3);
                    historyBtn.enabled = (!Config.INSTANCE.anonymous);
                    buttons.add(historyBtn);
                    event.setButtonList(buttons);
                }
            }
        }
    }
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if (GifEncoder.isRecording) {
            if (BlockShot.isActive()) {
                int skipFrames = 12;
                if (BlockShot.getFPS() > 20) {
                    skipFrames = (BlockShot.getFPS() / 10);
                }
                if (GifEncoder.frames > skipFrames || (GifEncoder.lastTimestamp != (System.currentTimeMillis() / 1000))) {
                    GifEncoder.frames = 0;
                    if (GifEncoder.lastTimestamp != (System.currentTimeMillis() / 1000)) {

                        GifEncoder.lastTimestamp = (System.currentTimeMillis() / 1000);
                        GifEncoder.totalSeconds++;
                    }
                    Framebuffer framebufferIn = Minecraft.getMinecraft().getFramebuffer();
                    IntBuffer pixelBuffer = null;
                    int width = 0;
                    int height = 0;
                    if (OpenGlHelper.isFramebufferEnabled()) {
                        //TODO: Investigate performance implications, as seems considerably worse than 1.16 and 1.18
                        width = framebufferIn.framebufferTextureWidth;
                        height = framebufferIn.framebufferTextureHeight;

                        int i = width * height;
                        if (pixelBuffer == null || pixelBuffer.capacity() < i) {
                            pixelBuffer = BufferUtils.createIntBuffer(i);
                        }
                        GlStateManager.glPixelStorei(3333, 1);
                        GlStateManager.glPixelStorei(3317, 1);
                        pixelBuffer.clear();
                        if (OpenGlHelper.isFramebufferEnabled()) {
                            GlStateManager.bindTexture(framebufferIn.framebufferTexture);
                            GlStateManager.glGetTexImage(3553, 0, 32993, 33639, pixelBuffer);
                        } else {
                            GlStateManager.glReadPixels(0, 0, width, height, 32993, 33639, pixelBuffer);
                        }
                        GifEncoder.addFrameAndClose(width, height, pixelBuffer);
                    }
                } else {
                    GifEncoder.frames++;
                }
            }
        }

    }
    public static boolean isActive() {
        return _active;
    }
    public static int getFPS() {
        return Minecraft.getMinecraft().debugFPS;
    }
    public static void uploadAndAddToChat(byte[] imageBytes) {
        if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
            ITextComponent finished = new TextComponentString("[BlockShot] Uploading to BlockShot...");
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(finished, BlockShot.CHAT_UPLOAD_ID);
        }
        String result = BlockShot.uploadImage(imageBytes);
        if (result == null) {
            if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                ITextComponent finished = new TextComponentString("[BlockShot] An error occurred uploading your content to BlockShot.");
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(finished, BlockShot.CHAT_UPLOAD_ID);
            }
        } else if (result.startsWith("http")) {
            ITextComponent link = (new TextComponentString(result));
            link.setStyle(link.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, result)).setUnderlined(true).setColor(TextFormatting.LIGHT_PURPLE));
            ITextComponent finished = new TextComponentString("[BlockShot] Your content is now available on BlockShot! ").appendSibling(link);
            Minecraft.getMinecraft().ingameGUI.getChatGUI().deleteChatLine(BlockShot.CHAT_UPLOAD_ID);
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(finished);
        }
    }

    public static String uploadImage(byte[] imageBytes) {
        try {
            String rsp = WebUtils.putWebResponse("https://blockshot.ch/upload", Base64.getEncoder().encodeToString(imageBytes), false, false, true);
            if (!rsp.equals("error")) {
                JsonElement jsonElement = new JsonParser().parse(rsp);
                String status = jsonElement.getAsJsonObject().get("status").getAsString();
                if (!status.equals("error")) {
                    String url = jsonElement.getAsJsonObject().get("url").getAsString();
                    return url;
                } else {
                    BlockShot.logger.error(jsonElement.getAsJsonObject().get("message").getAsString());
                    return null;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
        return null;
    }

    public static String getServerIDAndVerify() {
        Minecraft mc = Minecraft.getMinecraft();
        String serverId = DigestUtils.sha1Hex(String.valueOf(new Random().nextInt()));
        try {
            mc.getSessionService().joinServer(mc.getSession().getProfile(), mc.getSession().getToken(), serverId);
        } catch (AuthenticationException e) {
            logger.error("Failed to validate with Mojang: " + e.getMessage());
            return null;
        }
        return serverId;
    }
}
