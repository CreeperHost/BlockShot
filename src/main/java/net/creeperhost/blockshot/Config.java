package net.creeperhost.blockshot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;

public class Config {
    public static Config INSTANCE;

    public int uploadMode;
    public boolean anonymous;
    public boolean copyToClipboard;
    public Integer buttonPos;

    public Config() {
        this.uploadMode = 1;
        this.anonymous = true;
        this.copyToClipboard = false;
        this.buttonPos = 2;
    }

    public Config(int mode) {
        this();
        this.uploadMode = mode;
    }

    public Config(int mode, boolean anonymous) {
        this(mode);
        this.uploadMode = mode;
        this.anonymous = anonymous;
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
            validate();
        } catch (Exception ignored) {
            INSTANCE = new Config();
        }
    }

    public static void saveConfigToFile(File file) {
        try (FileOutputStream configOut = new FileOutputStream(file)) {
            IOUtils.write(Config.saveConfig(), configOut, Charset.defaultCharset());
        } catch (Throwable ignored) {
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
            validate();
        } catch (Exception ignored) {
            Config.INSTANCE = new Config();
        }
    }

    public static void validate() {
        if (INSTANCE == null) {
            INSTANCE = new Config();
        }
        if (INSTANCE.uploadMode < 0 || INSTANCE.uploadMode > 2) {
            INSTANCE.uploadMode = 1;
        }
        if (INSTANCE.buttonPos == null || INSTANCE.buttonPos < 0 || INSTANCE.buttonPos > 3) {
            INSTANCE.buttonPos = 2;
        }
    }

    public void cycleUploadMode() {
        uploadMode++;
        if (uploadMode > 2) {
            uploadMode = 0;
        }
    }

    public String uploadModeName() {
        if (uploadMode == 0) {
            return "Off";
        }
        if (uploadMode == 2) {
            return "Auto";
        }
        return "Prompt";
    }

    public void cycleButtonPos() {
        buttonPos++;
        if (buttonPos > 3) {
            buttonPos = 0;
        }
    }

    public String buttonPosName() {
        if (buttonPos == 0) {
            return "Top Left";
        }
        if (buttonPos == 1) {
            return "Top Right";
        }
        if (buttonPos == 3) {
            return "Bottom Right";
        }
        return "Bottom Left";
    }

    public int getButtonX(int screenWidth, int buttonWidth) {
        return (buttonPos == 1 || buttonPos == 3) ? screenWidth - buttonWidth - 10 : 10;
    }

    public int getButtonY(int screenHeight, int buttonHeight) {
        return (buttonPos == 0 || buttonPos == 1) ? 10 : screenHeight - buttonHeight - 10;
    }
}
