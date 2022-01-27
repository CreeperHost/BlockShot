package net.creeperhost.blockshot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import com.squareup.gifencoder.*;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.platform.Platform;
import net.creeperhost.blockshot.mixin.MixinChatComponent;
import net.creeperhost.blockshot.mixin.MixinMinecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
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
        int rotateTickMax = 60;
        int throbTickMax = 20;
        int rotateTicks = ticks % rotateTickMax;
        int throbTicks = ticks % throbTickMax;
        float rotationDegrees = (rotateTicks + partialTicks) * (360F / rotateTickMax);

        float scale = 1F + ((throbTicks >= (throbTickMax / 2) ? (throbTickMax - (throbTicks + partialTicks)) : (throbTicks + partialTicks)) * (2F / throbTickMax));
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1F);
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(rotationDegrees));
        drawItem(poseStack, stack, 0, true, null);
        poseStack.popPose();
    }
    public static void drawItem(PoseStack poseStack, ItemStack stack, int hash, boolean renderOverlay, @Nullable String text) {
        if (stack.isEmpty()) {
            return;
        }

        var mc = Minecraft.getInstance();
        var itemRenderer = mc.getItemRenderer();
        var bakedModel = itemRenderer.getModel(stack, null, mc.player, hash);

        Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.mulPoseMatrix(poseStack.last().pose());
        // modelViewStack.translate(x, y, 100.0D + this.blitOffset);
        modelViewStack.scale(1F, -1F, 1F);
        modelViewStack.scale(16F, 16F, 16F);
        RenderSystem.applyModelViewMatrix();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        var flatLight = !bakedModel.usesBlockLight();

        if (flatLight) {
            Lighting.setupForFlatItems();
        }

        itemRenderer.render(stack, ItemTransforms.TransformType.GUI, false, new PoseStack(), bufferSource, 0xF000F0, OverlayTexture.NO_OVERLAY, bakedModel);
        bufferSource.endBatch();
        RenderSystem.enableDepthTest();

        if (flatLight) {
            Lighting.setupFor3DItems();
        }

        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();

        if (renderOverlay) {
            var t = Tesselator.getInstance();
            var font = mc.font;

            if (stack.getCount() != 1 || text != null) {
                var s = text == null ? String.valueOf(stack.getCount()) : text;
                poseStack.pushPose();
                poseStack.translate(9D - font.width(s), 1D, 20D);
                font.drawInBatch(s, 0F, 0F, 0xFFFFFF, true, poseStack.last().pose(), bufferSource, false, 0, 0xF000F0);
                bufferSource.endBatch();
                poseStack.popPose();
            }

            if (stack.isBarVisible()) {
                RenderSystem.disableDepthTest();
                RenderSystem.disableTexture();
                RenderSystem.disableBlend();
                var barWidth = stack.getBarWidth();
                var barColor = stack.getBarColor();
                draw(poseStack, t, -6, 5, 13, 2, 0, 0, 0, 255);
                draw(poseStack, t, -6, 5, barWidth, 1, barColor >> 16 & 255, barColor >> 8 & 255, barColor & 255, 255);
                RenderSystem.enableBlend();
                RenderSystem.enableTexture();
                RenderSystem.enableDepthTest();
            }

            var cooldown = mc.player == null ? 0F : mc.player.getCooldowns().getCooldownPercent(stack.getItem(), mc.getFrameTime());

            if (cooldown > 0F) {
                RenderSystem.disableDepthTest();
                RenderSystem.disableTexture();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                draw(poseStack, t, -8, Mth.floor(16F * (1F - cooldown)) - 8, 16, Mth.ceil(16F * cooldown), 255, 255, 255, 127);
                RenderSystem.enableTexture();
                RenderSystem.enableDepthTest();
            }
        }
    }

    private static void draw(PoseStack matrixStack, Tesselator t, int x, int y, int width, int height, int red, int green, int blue, int alpha) {
        if (width <= 0 || height <= 0) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var m = matrixStack.last().pose();
        var renderer = t.getBuilder();
        renderer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        renderer.vertex(m, x, y, 0).color(red, green, blue, alpha).endVertex();
        renderer.vertex(m, x, y + height, 0).color(red, green, blue, alpha).endVertex();
        renderer.vertex(m, x + width, y + height, 0).color(red, green, blue, alpha).endVertex();
        renderer.vertex(m, x + width, y, 0).color(red, green, blue, alpha).endVertex();
        t.end();
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
            if(Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                ((MixinChatComponent)Minecraft.getInstance().gui.getChat()).invokeaddMessage(message, BlockShot.BLOCKSHOT_UPLOAD_ID);
            }
            while(isRecording) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {}
            }
            message = new TextComponent("Gameplay recording complete, encoding... ");
            if(Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                ((MixinChatComponent)Minecraft.getInstance().gui.getChat()).invokeaddMessage(message, BlockShot.BLOCKSHOT_UPLOAD_ID);
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
                if(Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                    ((MixinChatComponent)Minecraft.getInstance().gui.getChat()).invokeaddMessage(message, BlockShot.BLOCKSHOT_UPLOAD_ID);
                }
                try {
                    byte[] bytes = os.toByteArray();
                    String result = BlockShot.uploadImage(bytes);
                    if(result == null)
                    {
                        if(Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                            Component finished = new TextComponent("An error occurred uploading your content to BlockShot.");
                            ((MixinChatComponent)Minecraft.getInstance().gui.getChat()).invokeaddMessage(finished, BlockShot.BLOCKSHOT_UPLOAD_ID);
                        }
                    } else if(result.startsWith("http"))
                    {
                        Component link = (new TextComponent(result)).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, result)));
                        Component finished = new TextComponent("Your content is now available on BlockShot! ").append(link);
                        ((MixinChatComponent)Minecraft.getInstance().gui.getChat()).invokeremoveById(BlockShot.BLOCKSHOT_UPLOAD_ID);
                        Minecraft.getInstance().gui.getChat().addMessage(finished);
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
    public static String uploadImage(byte[] imageBytes)
    {
        try {
            String rsp = WebUtils.putWebResponse("https://blockshot.ch/upload", Base64.getEncoder().encodeToString(imageBytes), false, false, true);
            if (!rsp.equals("error")) {
                JsonElement jsonElement = JsonParser.parseString(rsp);
                String status = jsonElement.getAsJsonObject().get("status").getAsString();
                if (!status.equals("error")) {
                    String url = jsonElement.getAsJsonObject().get("url").getAsString();
                    if (Minecraft.getInstance() != null && Minecraft.getInstance().gui.getChat() != null) {
                        return url;
                    }
                } else {
                    System.out.println(jsonElement.getAsJsonObject().get("message").getAsString());
                    return null;
                }
            }
        } catch(Throwable t)
        {
            t.printStackTrace();
            return null;
        }
        return null;
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
