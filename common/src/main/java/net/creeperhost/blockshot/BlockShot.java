package net.creeperhost.blockshot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.squareup.gifencoder.*;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.platform.Platform;
import net.creeperhost.blockshot.mixin.MixinMinecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BlockShot
{
    public static final String MOD_ID = "blockshot";
    public static Logger logger = LogManager.getLogger();
    public static Path configLocation = Platform.getGameFolder().resolve(MOD_ID + ".json");
    public static final int BLOCKSHOT_UPLOAD_ID = 360360;
    public static byte[] latest;

    public static void init()
    {
        Config.init(configLocation.toFile());
        ClientRawInputEvent.KEY_PRESSED.register(BlockShot::onRawInput);
        ClientGuiEvent.INIT_POST.register((screen, access) ->
        {
            if(screen instanceof ControlsScreen)
            {
                int i = (screen.width / 2 - 155) + 160;
                int k = (screen.height / 6 - 12) + 48;
                String value = "Auto";
                if(Config.INSTANCE.uploadMode == 0) value = "Off";
                if(Config.INSTANCE.uploadMode == 1) value = "Prompt";
                String name = "BlockShot Upload: " + value;

                access.addRenderableWidget(new Button(i, k, 150, 20, new TextComponent(name), button ->
                {
                    if(Config.INSTANCE.uploadMode == 2)
                    {
                        Config.INSTANCE.uploadMode = 0;
                    } else if(Config.INSTANCE.uploadMode == 1)
                    {
                        Config.INSTANCE.uploadMode = 2;
                    } else if(Config.INSTANCE.uploadMode == 0) {
                        Config.INSTANCE.uploadMode = 1;
                    }
                    Config.saveConfigToFile(BlockShot.configLocation.toFile());
                    Minecraft.getInstance().setScreen(screen);
                }));
                String value2 = "Anonymous";
                if(!Config.INSTANCE.anonymous) value2 = Minecraft.getInstance().getUser().getName();
                String name2 = "BlockShot Owner: " + value2;
                i -= 160;
                k += 24;
                access.addRenderableWidget(new Button(i, k, 150, 20, new TextComponent(name2), button ->
                {
                    Config.INSTANCE.anonymous = Config.INSTANCE.anonymous ? false : true;
                    Config.saveConfigToFile(BlockShot.configLocation.toFile());
                    Minecraft.getInstance().setScreen(screen);
                }));
                String name3 = "View BlockShot History";
                i += 160;
                Button historyBtn = new Button(i, k, 150, 20, new TextComponent(name3), button ->
                {
                    Minecraft.getInstance().setScreen(new BlockShotHistoryScreen(screen));
                });

                historyBtn.active = (!Config.INSTANCE.anonymous);
                access.addRenderableWidget(historyBtn);
            }
        });
    }
    private static long keybindLast = 0;
    private static EventResult onRawInput(Minecraft minecraft, int keyCode, int scanCode, int action, int modifiers)
    {
        if(Screen.hasControlDown())
        {
            if(Minecraft.getInstance().options.keyScreenshot.matches(keyCode, scanCode))
            {
                if(keybindLast+5 > Instant.now().getEpochSecond()) return EventResult.pass();
                keybindLast = Instant.now().getEpochSecond();
                if (isRecording) {
                    isRecording = false;
                } else if (processedFrames.get() == 0 && addedFrames.get() == 0) {
                    recordGif();
                }
                return EventResult.interrupt(true);
            }
        }
        return EventResult.pass();
    }
    public static int getFPS()
    {
        return ((MixinMinecraft) Minecraft.getInstance()).getfps();
    }
    private static ExecutorService rendering = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("blockshot-framerenderer-%d").build());
    private static ExecutorService encoding = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("blockshot-imageencoder-%d").build());
    public static AtomicInteger addedFrames = new AtomicInteger();
    public static AtomicInteger processedFrames = new AtomicInteger();
    public static void addFrameAndClose(NativeImage screenImage)
    {
        CompletableFuture.runAsync(() -> {
            addedFrames.incrementAndGet();
            try {
                int i = screenImage.getWidth();
                int j = screenImage.getHeight();
                int k = 0;
                int l = 0;
                screenImage.flipY();
                int newx = 856;
                int newy = 482;
                int scaleFactor = 2;
                newx = newx - (((newx/2)/3)*scaleFactor);
                newy = newy - (((newy/2)/3)*scaleFactor);
                NativeImage nativeImage = new NativeImage(newx,newy, false);
                screenImage.resizeSubRectTo(k, l, i, j, nativeImage);
                screenImage.close();
                int w = nativeImage.getWidth();
                int h = nativeImage.getHeight();
                Color[][] colours = new Color[h][w];
                for (int y = 0; y < h; ++y) {
                    for (int x = 0; x < w; ++x) {
                        colours[y][x] = fromRgbMc(nativeImage.getPixelRGBA(x, y));
                    }
                }
                nativeImage.close();
                Image frame = Image.fromColors(colours);
                BlockShot._frames.getAndUpdate((a) -> {
                    a.add(frame);
                    return a;
                });
            } catch (Throwable t)
            {
                t.printStackTrace();
            } finally {
                screenImage.close();
            }
            processedFrames.incrementAndGet();
        }, rendering);
    }
    public static void loadingSpin(PoseStack poseStack, float partialTicks, int ticks, int x, int y, ItemStack stack)
    {
        int rotateTickMax = 30;
        int throbTickMax = 20;
        int rotateTicks = ticks % rotateTickMax;
        int throbTicks = ticks % throbTickMax;
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        poseStack.pushPose();
        poseStack.translate(0, 0, rotateTicks);
        float scale = 1F + ((throbTicks >= (throbTickMax / 2) ? (throbTickMax - (throbTicks + partialTicks)) : (throbTicks + partialTicks)) * (2F / throbTickMax));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderGuiItem(stack, x-8, y-8);
        poseStack.popPose();
    }
    //Minecraft's r and b channels work inverted-ly...
    private static Color fromRgbMc(int rgb) {
        int redComponent = rgb & 0xFF;
        int greenComponent = rgb >>> 8 & 0xFF;
        int blueComponent = rgb >>> 16 & 0xFF;
        return new Color(redComponent / 255.0, greenComponent / 255.0, blueComponent / 255.0);
    }
    public static boolean isRecording = false;
    public static long lastTimestamp;
    public static long frames;
    public static long totalSeconds;
    private static AtomicReference<List<Image>> _frames = new AtomicReference<>();
    public static void recordGif()
    {
        if(_frames == null || _frames.get() == null)
        {
            _frames.set(new ArrayList<Image>());
        }
        if(isRecording == true) return;
        lastTimestamp = 0;
        frames = 0;
        totalSeconds = 0;
        isRecording = true;
        CompletableFuture.runAsync(() -> {
            Component message = new TextComponent("You are now recording gameplay! ");
            if(Minecraft.getInstance() != null && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendMessage(message, Util.NIL_UUID);
            }
            while(isRecording) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {}
            }
            message = new TextComponent("Gameplay recording complete, encoding... ");
            if(Minecraft.getInstance() != null && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendMessage(message, Util.NIL_UUID);
            }
            while(addedFrames.get() > processedFrames.get()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {}
            }
            GifEncoder encoder = null;
            ImageOptions imageOptions = new ImageOptions();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            if(BlockShot._frames.get() != null) {
                List<Image> frames = BlockShot._frames.get();
                Image firstFrame = frames.get(0);
                try {
                    encoder = new GifEncoder(os, firstFrame.getWidth(), firstFrame.getHeight(), 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int duration = (int) (BlockShot.totalSeconds / frames.size());
                int f = 0;
                for (Image frame : frames) {
                    try {
                        f++;
                        imageOptions.setDelay(duration, TimeUnit.MILLISECONDS);
                        encoder.addImage(frame, imageOptions);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    encoder.finishEncoding();
                    frames.clear();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BlockShot._frames.set(new ArrayList<Image>());
                message = new TextComponent("Encoding complete... Uploading... ");
                if(Minecraft.getInstance() != null && Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendMessage(message, Util.NIL_UUID);
                }
                try {
                    byte[] bytes = os.toByteArray();
                    String rsp = WebUtils.putWebResponse("https://blockshot.ch/upload", Base64.getEncoder().encodeToString(bytes), false, false, true);
                    if(!rsp.equals("error")) {
                        JsonElement jsonElement = new JsonParser().parse(rsp);
                        String status = jsonElement.getAsJsonObject().get("status").getAsString();
                        if (!status.equals("error")) {
                            String url = jsonElement.getAsJsonObject().get("url").getAsString();
                            Component link = (new TextComponent(url)).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
                            if (Minecraft.getInstance().player != null) {
                                Component finished = new TextComponent("Your content is now available on BlockShot! ").append(link);
                                Minecraft.getInstance().player.sendMessage(finished, Util.NIL_UUID);
                            }
                        } else {
                            String mssage = jsonElement.getAsJsonObject().get("message").getAsString();
                            Component failMessage = new TextComponent(mssage);
                            if (Minecraft.getInstance().player != null) {
                                Minecraft.getInstance().player.sendMessage(failMessage, Util.NIL_UUID);
                            }
                        }
                    } else {
                        if (Minecraft.getInstance().player != null) {
                            Component finished = new TextComponent("An error occurred uploading your content to BlockShot.");
                            Minecraft.getInstance().player.sendMessage(finished, Util.NIL_UUID);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            addedFrames.set(0);
            processedFrames.set(0);
            isRecording = false;
        }, encoding);
    }
    public static String getServerIDAndVerify()
    {
        Minecraft mc = Minecraft.getInstance();
        String serverId = DigestUtils.sha1Hex(String.valueOf(new Random().nextInt()));
        try
        {
            mc.getMinecraftSessionService().joinServer(mc.getUser().getGameProfile(), mc.getUser().getAccessToken(), serverId);
        } catch (AuthenticationException e)
        {
            logger.error("Failed to validate with Mojang: " + e.getMessage());
            return null;
        }
        return serverId;
    }
}
