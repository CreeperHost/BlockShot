package net.creeperhost.blockshot.capture;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.architectury.platform.Platform;
import net.creeperhost.blockshot.*;
import net.creeperhost.blockshot.gui.BlockShotClickEvent;
import net.creeperhost.blockshot.lib.HistoryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Created by brandon3055 on 23/03/2023
 */
public class ScreenshotHandler {
    public static final Logger LOGGER = LogManager.getLogger();
    public static byte[] latest;

    public static boolean handleScreenshot(byte[] imageBytes) {
        latest = imageBytes;

        if (Config.INSTANCE.uploadMode == Config.Mode.PROMPT) {
            if (latest.length == 0) {
                return false;
            }

            Component confirmMessage = Component.translatable("chat.blockshot.prompt.blockshot")
                    .append(" ")
                    .append(Component.translatable("chat.blockshot.prompt.click_here")
                            .withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE)
                    )
                    .append(" ")
                    .append(Component.translatable("chat.blockshot.prompt.upload_screenshot"))
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
        Component finished = Component.translatable("chat.blockshot.upload.uploading");
        ClientUtil.sendMessage(finished, BlockShot.CHAT_UPLOAD_ID);

        String result = uploadImage(imageBytes, progress, type);
        if (result != null && !result.equals("error")) {
            if (result.startsWith("http")) {
                MutableComponent link = (Component.literal(result)).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, result)));
                finished = Component.translatable("chat.blockshot.upload.uploaded").append(" ").append(link);
                if (Config.INSTANCE.copyToClipboard) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(result);
                    finished.append(" ").append(Component.translatable("chat.blockshot.upload.copied").withStyle(ChatFormatting.GRAY));
                }
                ClientUtil.deleteMessage(BlockShot.CHAT_UPLOAD_ID);
                ClientUtil.sendMessage(finished);
            } else {
                finished = Component.translatable("chat.blockshot.upload.error.reason", result);
                ClientUtil.sendMessage(finished, BlockShot.CHAT_UPLOAD_ID);

                //Fallback
                if (writeOnFail) {
                    saveLocal(imageBytes, Platform.getGameFolder().toFile(), null, fallbackExt, ClientUtil::sendMessage, "chat.blockshot.fallback.success", "chat.blockshot.fallback.failure");
                }
            }
        } else {
            finished = Component.translatable("chat.blockshot.upload.error");
            ClientUtil.sendMessage(finished, BlockShot.CHAT_UPLOAD_ID);

            //Fallback
            if (writeOnFail) {
                saveLocal(imageBytes, Platform.getGameFolder().toFile(), null, fallbackExt, ClientUtil::sendMessage, "chat.blockshot.fallback.success", "chat.blockshot.fallback.failure");
            }
        }
    }

    public static String uploadImage(byte[] imageBytes, @Nullable AtomicDouble progress, WebUtils.MediaType type) {
        try {
            String rsp = WebUtils.put("https://blocks.hot/api/v1/shares", imageBytes, type, progress);
            return readJsonResponse(rsp);
        } catch (Throwable t) {
            LOGGER.error("An error occurred while uploading image", t);
        }
        return null;
    }

    public static String readJsonResponse(String rsp) {
        try {
            if (!rsp.startsWith("{")) return rsp;
            JsonElement jsonElement = JsonParser.parseString(rsp);
            HistoryManager.instance.markDirty();
            return "https://blocks.hot/%s".formatted(jsonElement.getAsJsonObject().get("code").getAsString());
        } catch (Throwable t) {
            LOGGER.error("An error occurred while uploading image", t);
        }
        return null;
    }

    /**
     * extension is only used if a file name is not provided.
     */
    public static void saveLocal(byte[] bytes, File gameDir, @Nullable String fileName, String extension, Consumer<Component> consumer, String msgSuccess, String msgFail) {
        File file2 = new File(gameDir, "screenshots");
        file2.mkdir();
        File outputFile = fileName == null ? getFile(file2, extension) : new File(file2, fileName);

        Util.ioPool().execute(() -> {
            try (OutputStream os = new FileOutputStream(outputFile)) {
                os.write(bytes);
                Component component = Component.literal(outputFile.getName()).withStyle(ChatFormatting.UNDERLINE).withStyle((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outputFile.getAbsolutePath())));
                consumer.accept(Component.translatable(msgSuccess, component));
            } catch (Exception e) {
                LOGGER.warn("Couldn't save screenshot", e);
                consumer.accept(Component.translatable(msgFail, e.getMessage()));
            }
        });
    }

    public static File getFile(File directory, String extension) {
        String dateTimeString = Util.getFilenameFormattedDateTime();
        int i = 1;
        while (true) {
            File result = new File(directory, dateTimeString + (i == 1 ? "" : "_" + i) + "." + extension);
            if (!result.exists()) {
                return result;
            }
            ++i;
        }
    }
}
