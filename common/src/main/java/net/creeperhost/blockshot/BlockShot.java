package net.creeperhost.blockshot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.creeperhost.blockshot.gui.BlockShotHistoryScreen;
import net.creeperhost.blockshot.mixin.MixinChatComponent;
import net.creeperhost.blockshot.mixin.MixinMinecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Random;

public class BlockShot {
    public static final String MOD_ID = "blockshot";
    public static Logger logger = LogManager.getLogger();
    public static Path configLocation = Platform.getGameFolder().resolve(MOD_ID + ".json");
    public static final MessageSignature CHAT_UPLOAD_ID = new MessageSignature(new byte[]{36, 03, 60});
    public static final MessageSignature CHAT_ENCODING_ID = new MessageSignature(new byte[]{42, 04, 20});
    public static byte[] latest;
    private static boolean _active = false;

    public static void init() {
        if (Platform.getEnvironment().equals(Env.CLIENT)) {
            if (getServerIDAndVerify() != null || Platform.isDevelopmentEnvironment()) {
                _active = true;
            } else {
                logger.error("BlockShot will not run in offline mode.");
            }
        }
        if (BlockShot.isActive()) {
            Config.init(configLocation.toFile());
            ClientRawInputEvent.KEY_PRESSED.register(BlockShot::onRawInput);
            ClientGuiEvent.INIT_POST.register((screen, access) ->
            {
                if (screen instanceof ControlsScreen) {
                    int i = (screen.width / 2 - 155) + 160;
                    int k = (screen.height / 6 - 12) + 48;
                    String value = I18n.get("gui.blockshot.upload_mode.auto");
                    if (Config.INSTANCE.uploadMode == 0) value = I18n.get("gui.blockshot.upload_mode.off");
                    if (Config.INSTANCE.uploadMode == 1) value = I18n.get("gui.blockshot.upload_mode.prompt");
                    String name = I18n.get("gui.blockshot.upload_mode") + " " + value;

                    access.addRenderableWidget(new Button(i, k, 150, 20, Component.literal(name), button ->
                    {
                        if (Config.INSTANCE.uploadMode == 2) {
                            Config.INSTANCE.uploadMode = 0;
                        } else if (Config.INSTANCE.uploadMode == 1) {
                            Config.INSTANCE.uploadMode = 2;
                        } else if (Config.INSTANCE.uploadMode == 0) {
                            Config.INSTANCE.uploadMode = 1;
                        }
                        Config.saveConfigToFile(BlockShot.configLocation.toFile());
                        Minecraft.getInstance().setScreen(screen);
                    }));
                    String value2 = I18n.get("gui.blockshot.upload.anonymous");
                    if (!Config.INSTANCE.anonymous) value2 = Minecraft.getInstance().getUser().getName();
                    String name2 = I18n.get("gui.blockshot.upload.owner") + " " + value2;
                    i -= 160;
                    k += 24;
                    access.addRenderableWidget(new Button(i, k, 150, 20, Component.literal(name2), button ->
                    {
                        Config.INSTANCE.anonymous = Config.INSTANCE.anonymous ? false : true;
                        Config.saveConfigToFile(BlockShot.configLocation.toFile());
                        Minecraft.getInstance().setScreen(screen);
                    }));
                    String name3 = I18n.get("gui.blockshot.upload.history");
                    i += 160;
                    Button historyBtn = new Button(i, k, 150, 20, Component.literal(name3), button ->
                    {
                        Minecraft.getInstance().setScreen(new BlockShotHistoryScreen(screen));
                    });

                    historyBtn.active = (!Config.INSTANCE.anonymous);
                    access.addRenderableWidget(historyBtn);

                    //Move the "done button down as its currently under out buttons"
                    for (Widget renderable : access.getRenderables()) {
                        if (renderable instanceof Button button) {
                            if (button.getMessage().equals(CommonComponents.GUI_DONE)) {
                                button.y += 24;
                                break;
                            }
                        }
                    }
                }
            });
        }
    }

    public static boolean isActive() {
        return _active;
    }

    private static long keybindLast = 0;

    private static EventResult onRawInput(Minecraft minecraft, int keyCode, int scanCode, int action, int modifiers) {
        if (Screen.hasControlDown()) {
            if (Minecraft.getInstance().options.keyScreenshot.matches(keyCode, scanCode)) {
                if (keybindLast + 5 > Instant.now().getEpochSecond()) return EventResult.pass();
                keybindLast = Instant.now().getEpochSecond();
                if (GifEncoder.isRecording) {
                    GifEncoder.isRecording = false;
                } else if (GifEncoder.processedFrames.get() == 0 && GifEncoder.addedFrames.get() == 0) {
                    GifEncoder.begin();
                }
                return EventResult.interrupt(true);
            }
        }
        return EventResult.pass();
    }

    public static int getFPS() {
        return ((MixinMinecraft) Minecraft.getInstance()).getfps();
    }

    public static void uploadAndAddToChat(byte[] imageBytes) {
        Component finished = Component.translatable("chat.blockshot.upload.uploading");
        ClientUtil.sendMessage(finished, BlockShot.CHAT_UPLOAD_ID);

        String result = BlockShot.uploadImage(imageBytes);
        if (result == null) {
            finished = Component.translatable("chat.blockshot.upload.error");
            ClientUtil.sendMessage(finished, BlockShot.CHAT_UPLOAD_ID);

        } else if (result.startsWith("http")) {
            Component link = (Component.literal(result)).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, result)));
            finished = Component.translatable("chat.blockshot.upload.uploaded").append(" ").append(link);
            ClientUtil.deleteBlockshotMessages(BlockShot.CHAT_UPLOAD_ID);
            ClientUtil.sendMessage(finished);
        }
    }

    public static String uploadImage(byte[] imageBytes) {
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
        Minecraft mc = Minecraft.getInstance();
        String serverId = DigestUtils.sha1Hex(String.valueOf(new Random().nextInt()));
        try {
            mc.getMinecraftSessionService().joinServer(mc.getUser().getGameProfile(), mc.getUser().getAccessToken(), serverId);
        } catch (AuthenticationException e) {
            logger.error("Failed to validate with Mojang: " + e.getMessage());
            return null;
        }
        return serverId;
    }
}
