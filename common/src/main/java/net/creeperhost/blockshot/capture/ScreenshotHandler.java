package net.creeperhost.blockshot.capture;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.architectury.platform.Platform;
import net.creeperhost.blockshot.*;
import net.creeperhost.blockshot.gui.BlockShotClickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.function.Consumer;

/**
 * Created by brandon3055 on 23/03/2023
 */
public class ScreenshotHandler {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    public static final Logger LOGGER = LogManager.getLogger();
    public static byte[] latest;

    public static boolean handleScreenshot(byte[] imageBytes) {
        latest = imageBytes;

        if (Config.INSTANCE.uploadMode == Config.Mode.PROMPT) {
            if (latest.length == 0) {
                return false;
            }

            Component confirmMessage = new TranslatableComponent("chat.blockshot.prompt.blockshot")
                    .append(" ")
                    .append(new TranslatableComponent("chat.blockshot.prompt.click_here")
                            .withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE)
                    )
                    .append(" ")
                    .append(new TranslatableComponent("chat.blockshot.prompt.upload_screenshot"))
                    .withStyle(style -> style.withClickEvent(new BlockShotClickEvent(ClickEvent.Action.RUN_COMMAND, "/blockshot upload")));

            ClientUtil.sendMessage(confirmMessage, BlockShot.CHAT_UPLOAD_ID);
            return false;
        }

        uploadLast(true);
        return true;
    }

    public static void uploadLast(boolean writeOnFail) {
        if (!ClientUtil.validState()) return;
        if (latest == null || latest.length == 0) return;

        //Copy the array just in case another screenshot is taken while the previous is being uploaded.
        byte[] bytes = Arrays.copyOf(latest, latest.length);
        latest = null;

        Util.ioPool().execute(() -> uploadAndAddToChat(bytes, writeOnFail, "png", null, WebUtils.MediaType.PNG));
    }

    public static void uploadAndAddToChat(byte[] imageBytes, boolean writeOnFail, String fallbackExt, @Nullable AtomicDouble progress, WebUtils.MediaType type) {
        Component finished = new TranslatableComponent("chat.blockshot.upload.uploading");
        ClientUtil.sendMessage(finished, BlockShot.CHAT_UPLOAD_ID);

        String result = uploadImage(imageBytes, progress, type);
        if (result == null) {
            finished = new TranslatableComponent("chat.blockshot.upload.error");
            ClientUtil.sendMessage(finished, BlockShot.CHAT_UPLOAD_ID);

            //Fallback
            if (writeOnFail) {
                saveLocal(imageBytes, Platform.getGameFolder().toFile(), null, fallbackExt, ClientUtil::sendMessage, "chat.blockshot.fallback.success", "chat.blockshot.fallback.failure");
            }
        } else if (result.startsWith("http")) {
            Component link = (new TextComponent(result)).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, result)));
            finished = new TranslatableComponent("chat.blockshot.upload.uploaded").append(" ").append(link);
            ClientUtil.sendMessage(finished);
        }
    }

    public static String uploadImage(byte[] imageBytes, @Nullable AtomicDouble progress, WebUtils.MediaType type) {
        try {
            String rsp = WebUtils.post("https://blockshot.ch/upload", Base64.getEncoder().encodeToString(imageBytes), type, progress);
            return readJsonResponse(rsp);
        } catch (Throwable t) {
            LOGGER.error("An error occurred while uploading image", t);
        }
        return null;
    }

    public static String readJsonResponse(String rsp) {
        try {
            if (rsp.equals("error")) return null;
            JsonElement jsonElement = JsonParser.parseString(rsp);
            String status = jsonElement.getAsJsonObject().get("status").getAsString();
            if (!status.equals("error")) {
                return jsonElement.getAsJsonObject().get("url").getAsString();
            } else {
                LOGGER.error("Server Response: {}", jsonElement.getAsJsonObject().get("message").getAsString());
            }
        } catch (Throwable t) {
            LOGGER.error("An error occurred while uploading image", t);
        }
        return null;
    }

    /**
     * extension is only used if a file name is not provided.
     * */
    public static void saveLocal(byte[] bytes, File gameDir, @Nullable String fileName, String extension, Consumer<Component> consumer, String msgSuccess, String msgFail) {
        File file2 = new File(gameDir, "screenshots");
        file2.mkdir();
        File outputFile = fileName == null ? getFile(file2, extension) : new File(file2, fileName);

        Util.ioPool().execute(() -> {
            try (OutputStream os = new FileOutputStream(outputFile)){
                os.write(bytes);
                Component component = new TextComponent(outputFile.getName()).withStyle(ChatFormatting.UNDERLINE).withStyle((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outputFile.getAbsolutePath())));
                consumer.accept(new TranslatableComponent(msgSuccess, component));
            } catch (Exception e) {
                LOGGER.warn("Couldn't save screenshot", e);
                consumer.accept(new TranslatableComponent(msgFail, e.getMessage()));
            }
        });
    }

    public static File getFile(File directory, String extension) {
        String string = DATE_FORMAT.format(new Date());
        int i = 1;
        while(true) {
            File result = new File(directory, string + (i == 1 ? "" : "_" + i) + "." + extension);
            if (!result.exists()) {
                return result;
            }
            ++i;
        }
    }
}
