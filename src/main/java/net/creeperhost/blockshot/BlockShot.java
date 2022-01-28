package net.creeperhost.blockshot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Base64;
import java.util.Random;

@Mod(modid = BlockShot.MODID, name = BlockShot.NAME, version = BlockShot.VERSION, clientSideOnly = true)
public class BlockShot
{
    public static final String MODID = "blockshot";
    public static final String NAME = "BlockShot";
    public static final String VERSION = "1.2.4";
    public static Path configLocation = null;//Platform.getGameFolder().resolve(MOD_ID + ".json");
    public static final int CHAT_UPLOAD_ID = 360360;
    public static final int CHAT_ENCODING_ID = 420420;
    public static byte[] latest;
    private static boolean _active = false;

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        if (getServerIDAndVerify() != null || ((boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment"))) {
            _active = true;
        } else {
            logger.error("BlockShot will not run in offline mode.");
        }
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
            //TODO: Learn how to format text in 1.12
            ITextComponent link = (new TextComponentString(result)).setStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.LIGHT_PURPLE).setStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, result)));
            ITextComponent finished = new TextComponentString("[BlockShot] Your content is now available on BlockShot! ").append(link);
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
