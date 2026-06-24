package net.creeperhost.blockshot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.common.util.concurrent.AtomicDouble;
import com.mojang.authlib.exceptions.AuthenticationException;
import net.creeperhost.blockshot.lib.MTSessionProvider;
import net.creeperhost.blockshot.gui.BlockShotClickEvent;
import net.creeperhost.blockshot.gui.BlockShotHistoryScreen;
import net.creeperhost.minetogether.session.JWebToken;
import net.creeperhost.minetogether.session.MineTogetherSession;
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
import net.minecraftforge.client.event.RenderGameOverlayEvent;
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
import org.lwjgl.input.Mouse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Mod(modid = BlockShot.MODID, name = BlockShot.NAME, version = BlockShot.VERSION, clientSideOnly = true)
public class BlockShot
{
    public static final String MODID = "blockshot";
    public static final String NAME = "BlockShot";
    public static final String VERSION = "1.5.0";
    public static Path configLocation = null;
    public static final int CHAT_UPLOAD_ID = 360360;
    public static final int CHAT_ENCODING_ID = 420420;
    public static byte[] latest;
    private static boolean _active = false;
    private static CompletableFuture<JWebToken> tokenFuture;

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
        MineTogetherSession.getDefault().setProvider(new MTSessionProvider());
        tokenFuture = MineTogetherSession.getDefault().getTokenAsync();
        try {
            JWebToken token = tokenFuture.get();
            if (token != null) {
                Auth.init(token);
                _active = true;
            }
        } catch (InterruptedException | ExecutionException ignored) {
        }
        _active |= ((boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment"));

        if (_active) {
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
                                BlockShot.uploadAndAddToChat(BlockShot.latest, false, "png", WebUtils.MediaType.PNG, null);
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
                VideoEncoder.startOrStopRecording();
                return;
            }
            if (GuiScreen.isShiftKeyDown() && VideoEncoder.isWorking()) {
                event.setResultMessage(new TextComponentString(" "));
                event.setCanceled(true);
                VideoEncoder.cancelRecording();
                return;
            }
            if (VideoEncoder.isWorking()) {
                event.setResultMessage(new TextComponentString(" "));
                event.setCanceled(true);
                return;
            }
            if (Config.INSTANCE.uploadMode != 0) {
                BufferedImage nativeImage = event.getImage();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ImageIO.write(nativeImage, "PNG", baos);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BlockShot.latest = baos.toByteArray();
                if (Config.INSTANCE.uploadMode == 2) {
                    CompletableFuture.runAsync(() ->
                    {
                        if (BlockShot.latest == null || BlockShot.latest.length == 0) return;
                        BlockShot.uploadAndAddToChat(BlockShot.latest, true, "png", WebUtils.MediaType.PNG, null);
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
                if (screen instanceof GuiIngameMenu && event.getButton().id == 8008137) {
                    Minecraft.getMinecraft().displayGuiScreen(new BlockShotHistoryScreen(screen));
                    return;
                }
                if (screen instanceof GuiOptions) {
                    GuiButton button = event.getButton();
                    if(whenClick == (System.currentTimeMillis() / 1000)) return;
                    whenClick = (System.currentTimeMillis() / 1000);
                    switch(button.id)
                    {
                        case 8008135:
                            Config.INSTANCE.cycleUploadMode();
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
                    String name = "BlockShot Upload: " + Config.INSTANCE.uploadModeName();
                    buttons.add(new GuiButton(8008135, i, k, 150, 20, name));
                    String value2 = "Anonymous";
                    if (!Config.INSTANCE.anonymous) value2 = Minecraft.getMinecraft().getSession().getUsername();
                    String name2 = "BlockShot Owner: " + value2;
                    i -= 160;
                    buttons.add(new GuiButton(8008136, i, k, 150, 20, name2));
                    String name3 = "View BlockShot History";
                    k += 120;
                    buttons.add(new GuiButton(8008137, i, k, 150, 20, name3));
                    event.setButtonList(buttons);
                }
                if (screen instanceof GuiIngameMenu) {
                    List<GuiButton> buttons = event.getButtonList();
                    int buttonWidth = 100;
                    int buttonHeight = 20;
                    buttons.add(new GuiButton(8008137, Config.INSTANCE.getButtonX(screen.width, buttonWidth), Config.INSTANCE.getButtonY(screen.height, buttonHeight), buttonWidth, buttonHeight, "BlockShot"));
                    event.setButtonList(buttons);
                }
            }
        }
    }
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && BlockShot.isActive()) {
            VideoEncoder.updateCapture();
        }

    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (!VideoEncoder.isWorking() || Minecraft.getMinecraft().gameSettings.hideGUI) {
            return;
        }
        event.getLeft().add("");
        event.getLeft().addAll(VideoEncoder.getHudText());
    }
    public static boolean isActive() {
        return _active;
    }
    public static int getFPS() {
        return Minecraft.getDebugFPS();
    }
    public static void uploadAndAddToChat(byte[] imageBytes) {
        WebUtils.MediaType type = mediaType(imageBytes);
        uploadAndAddToChat(imageBytes, false, extensionFor(type), type, null);
    }

    public static void uploadAndAddToChat(byte[] imageBytes, boolean writeOnFail, String fallbackExt, WebUtils.MediaType type, AtomicDouble progress) {
        if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
            ITextComponent finished = new TextComponentString("[BlockShot] Uploading to BlockShot...");
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(finished, BlockShot.CHAT_UPLOAD_ID);
        }
        String result = BlockShot.uploadImage(imageBytes, type, progress);
        if (result == null) {
            if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                ITextComponent finished = new TextComponentString("[BlockShot] An error occurred uploading your content to BlockShot.");
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(finished, BlockShot.CHAT_UPLOAD_ID);
            }
            if (writeOnFail) {
                saveLocalFallback(imageBytes, null, fallbackExt);
            }
        } else if (result.startsWith("http")) {
            if (Config.INSTANCE.copyToClipboard) {
                GuiScreen.setClipboardString(result);
            }
            ITextComponent link = (new TextComponentString(result));
            link.setStyle(link.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, result)).setUnderlined(true).setColor(TextFormatting.LIGHT_PURPLE));
            ITextComponent finished = new TextComponentString("[BlockShot] Your content is now available on BlockShot! ").appendSibling(link);
            if (Config.INSTANCE.copyToClipboard) {
                finished.appendText(" (Copied)");
            }
            Minecraft.getMinecraft().ingameGUI.getChatGUI().deleteChatLine(BlockShot.CHAT_UPLOAD_ID);
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(finished);
        } else if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
            ITextComponent finished = new TextComponentString("[BlockShot] An error occurred uploading your content to BlockShot: " + result);
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(finished, BlockShot.CHAT_UPLOAD_ID);
            if (writeOnFail) {
                saveLocalFallback(imageBytes, null, fallbackExt);
            }
        }
    }

    public static String uploadImage(byte[] imageBytes) {
        return uploadImage(imageBytes, mediaType(imageBytes), null);
    }

    public static String uploadImage(byte[] imageBytes, WebUtils.MediaType type, AtomicDouble progress) {
        try {
            String rsp = WebUtils.put("https://blocks.hot/api/v1/shares", imageBytes, type, progress);
            if (rsp != null && !rsp.equals("error")) {
                if (!rsp.startsWith("{")) {
                    return rsp;
                }
                JsonElement jsonElement = new JsonParser().parse(rsp);
                return "https://blocks.hot/" + jsonElement.getAsJsonObject().get("code").getAsString();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
        return null;
    }

    public static void saveLocalFallback(byte[] bytes, File sourceFile, String extension) {
        try {
            File directory = new File(Minecraft.getMinecraft().mcDataDir, "screenshots");
            directory.mkdirs();
            File outputFile = nextCaptureFile(directory, extension);
            if (sourceFile != null) {
                try (FileInputStream input = new FileInputStream(sourceFile); OutputStream output = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
            } else if (bytes != null) {
                try (OutputStream output = new FileOutputStream(outputFile)) {
                    output.write(bytes);
                }
            } else {
                return;
            }

            ITextComponent file = new TextComponentString(outputFile.getName());
            file.setStyle(file.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outputFile.getAbsolutePath())).setUnderlined(true).setColor(TextFormatting.LIGHT_PURPLE));
            ITextComponent message = new TextComponentString("[BlockShot] Saved capture locally: ").appendSibling(file);
            if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().ingameGUI.getChatGUI() != null) {
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(message);
                    }
                });
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static File nextCaptureFile(File directory, String extension) {
        String dateTime = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        int index = 1;
        while (true) {
            File file = new File(directory, dateTime + (index == 1 ? "" : "_" + index) + "." + extension);
            if (!file.exists()) {
                return file;
            }
            index++;
        }
    }

    private static WebUtils.MediaType mediaType(byte[] imageBytes) {
        if (imageBytes != null && imageBytes.length > 4 && (imageBytes[0] & 0xFF) == 0x89 && imageBytes[1] == 'P' && imageBytes[2] == 'N' && imageBytes[3] == 'G') {
            return WebUtils.MediaType.PNG;
        }
        return WebUtils.MediaType.JPEG;
    }

    private static String extensionFor(WebUtils.MediaType type) {
        if (type == WebUtils.MediaType.PNG) {
            return "png";
        }
        if (type == WebUtils.MediaType.WEBM) {
            return "webm";
        }
        return "jpg";
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
