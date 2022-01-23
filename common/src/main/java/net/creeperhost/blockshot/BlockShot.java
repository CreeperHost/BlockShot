package net.creeperhost.blockshot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Base64;
import java.util.Random;

public class BlockShot
{
    public static final String MOD_ID = "blockshot";
    public static Logger logger = LogManager.getLogger();
    public static Path configLocation = Platform.getGameFolder().resolve(MOD_ID + ".json");
    public static NativeImage latest;

    public static void init()
    {
        CommandRegistrationEvent.EVENT.register(BlockShot::registerCommands);
        Config.init(configLocation.toFile());
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
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection selection) {
        Command uploadLatest = new Command() {
            @Override
            public int run(CommandContext context) {
                if(BlockShot.latest == null) return 0;
                Util.ioPool().execute(() ->
                {
                    try {
                        byte[] bytes = BlockShot.latest.asByteArray();
                        try {
                            String rsp = WebUtils.putWebResponse("https://blockshot.ch/upload", Base64.getEncoder().encodeToString(bytes), false, false);
                            JsonElement jsonElement = new JsonParser().parse(rsp);
                            String status = jsonElement.getAsJsonObject().get("status").getAsString();
                            if (!status.equals("error")) {
                                String url = jsonElement.getAsJsonObject().get("url").getAsString();
                                Component link = (new TextComponent(url)).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
                                if(Minecraft.getInstance().player != null)
                                {
                                    Minecraft.getInstance().player.sendMessage(link, Util.NIL_UUID);
                                }
                            } else {
                                String message = jsonElement.getAsJsonObject().get("message").getAsString();
                                Component failMessage = new TextComponent(message);
                                if(Minecraft.getInstance().player != null)
                                {
                                    Minecraft.getInstance().player.sendMessage(failMessage, Util.NIL_UUID);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception var7) {
                        TranslatableComponent err = new TranslatableComponent("Failed to create screenshot ", new Object[]{var7.getMessage()});
                        if(Minecraft.getInstance().player != null)
                        {
                            Minecraft.getInstance().player.sendMessage(err, Util.NIL_UUID);
                        }
                    } finally {
                        BlockShot.latest.close();
                    }
                    BlockShot.latest = null;
                });
                return 1;
            }
        };
        LiteralCommandNode<CommandSourceStack> command = dispatcher.register(
                Commands.literal("blockshot")
                        .then(
                                Commands.literal("upload").executes(uploadLatest)
                        )
        );
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
