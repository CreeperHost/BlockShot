package net.creeperhost.blockshot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.creeperhost.blockshot.capture.Encoder;
import net.creeperhost.blockshot.capture.GifEncoder;
import net.creeperhost.blockshot.capture.RecordingHandler;
import net.creeperhost.blockshot.capture.VideoEncoder;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class Config {
    public static Config INSTANCE;

    public Mode uploadMode = Mode.PROMPT;
    public ButtonPos buttonPos = ButtonPos.BOTTOM_LEFT;
    private EncoderType encoderType = EncoderType.GIF;
    public boolean anonymous;

    public Config() {
        this.anonymous = true;
    }

    public Config(Mode mode) {
        this.uploadMode = mode;
    }

    public Config(Mode mode, boolean anonymous) {
        this.uploadMode = mode;
        this.anonymous = anonymous;
    }

    public void setEncoderType(EncoderType encoderType) {
        if (RecordingHandler.setEncoder(encoderType.createEncoder())) {
            this.encoderType = encoderType;
        }
    }

    public EncoderType getEncoderType() {
        return encoderType;
    }

    public static String saveConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        return gson.toJson(INSTANCE);
    }

    public static void loadFromFile(File file) {
        Gson gson = new Gson();
        try {
            FileReader fileReader = new FileReader(file);
            INSTANCE = gson.fromJson(fileReader, Config.class);
            if (INSTANCE.uploadMode == null) INSTANCE.uploadMode = Mode.PROMPT; //Fixes null pointer due to loading previous config format.
        } catch (Exception ex) {
            BlockShot.LOGGER.error("Failed to load config, Resetting to default", ex);
            INSTANCE = new Config();
            saveConfigToFile(BlockShot.configLocation.toFile());
        }
    }

    public static void saveConfigToFile(File file) {
        try (FileOutputStream configOut = new FileOutputStream(file)) {
            IOUtils.write(Config.saveConfig(), configOut, Charset.defaultCharset());
        } catch (Throwable ex) {
            BlockShot.LOGGER.error("An error occurred while saving config to file", ex);
        }
    }

    public static void init(File file) {
        try {
            if (!file.exists()) {
                Config.INSTANCE = new Config();

                FileWriter tileWriter = new FileWriter(file);
                tileWriter.write(Config.saveConfig());
                tileWriter.close();
            } else {
                Config.loadFromFile(file);
            }
        } catch (Exception ignored) {
        }
    }

    public enum Mode {
        OFF,
        PROMPT,
        AUTO;

        public Mode next() { return values()[(ordinal() + 1) % values().length]; }

        public String translatableName() {
            return "gui.blockshot.settings.upload_mode." + name().toLowerCase(Locale.ENGLISH);
        }
    }

    public enum ButtonPos {
        TOP_LEFT((sw, bw) -> 10, (sh, bh) -> 10),
        TOP_RIGHT((sw, bw) -> sw - bw - 10, (sh, bh) -> 10),
        BOTTOM_LEFT((sw, bw) -> 10, (sh, bh) -> sh - bh - 10),
        BOTTOM_RIGHT((sw, bw) -> sw - bw - 10, (sh, bh) -> sh - bh - 10);

        private final BiFunction<Integer, Integer, Integer> xGetter;
        private final BiFunction<Integer, Integer, Integer> yGetter;

        ButtonPos(BiFunction<Integer, Integer, Integer> xGetter, BiFunction<Integer, Integer, Integer> yGetter) {
            this.xGetter = xGetter;
            this.yGetter = yGetter;
        }

        public ButtonPos next() { return values()[(ordinal() + 1) % values().length]; }

        public String translatableName() {
            return "gui.blockshot.settings.button_pos." + name().toLowerCase(Locale.ENGLISH);
        }

        public int getX(int screenWidth, int buttonWidth) {
            return xGetter.apply(screenWidth, buttonWidth);
        }
        public int getY(int screenHeight, int buttonHeight) {
            return yGetter.apply(screenHeight, buttonHeight);
        }
    }

    public enum EncoderType {
        GIF(GifEncoder::new, false),
        MOV(VideoEncoder::new, true);

        private final Supplier<Encoder> getEncoder;
        private final boolean requiresPremium;

        EncoderType(Supplier<Encoder> getEncoder, boolean requiresPremium) {
            this.getEncoder = getEncoder;
            this.requiresPremium = requiresPremium;
        }

        public Encoder createEncoder() {
            return getEncoder.get();
        }

        public boolean requiresPremium() {
            return requiresPremium;
        }
    }
}
