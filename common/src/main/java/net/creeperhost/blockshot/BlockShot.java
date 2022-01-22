package net.creeperhost.blockshot;

import com.mojang.authlib.exceptions.AuthenticationException;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.TextComponent;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Random;

public class BlockShot
{
    public static final String MOD_ID = "blockshot";
    public static Logger logger = LogManager.getLogger();
    public static Path configLocation = Platform.getGameFolder().resolve(MOD_ID + ".json");
    
    public static void init()
    {
        Config.init(configLocation.toFile());
        ClientGuiEvent.INIT_POST.register((screen, access) ->
        {
            if(screen instanceof ControlsScreen)
            {
                int i = (screen.width / 2 - 155) + 160;
                int k = (screen.height / 6 - 12) + 48;
                String value = "ON";
                if(!Config.INSTANCE.enabled) value = "OFF";
                String name = "BlockShot: " + value;

                access.addRenderableWidget(new Button(i, k, 150, 20, new TextComponent(name), button ->
                {
                    Config.INSTANCE.enabled = !Config.INSTANCE.enabled;
                    Config.saveConfigToFile(BlockShot.configLocation.toFile());
                    Minecraft.getInstance().setScreen(screen);
                }));
            }
        });
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
            logger.error("Failed to get serverID from Mojang");
            return null;
        }
        logger.info("new ServerID requested");
        return serverId;
    }
}
