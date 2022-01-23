package net.creeperhost.blockshot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;

public class Config
{
    public static Config INSTANCE;

    public int uploadMode;
    public boolean anonymous;

    public Config()
    {
        this.uploadMode = 1; this.anonymous = true;
    }

    public Config(int mode)
    {
        this.uploadMode = mode;
    }
    public Config(int mode, boolean anonymous)
    {
        this.uploadMode = mode; this.anonymous = anonymous;
    }

    public static String saveConfig()
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        return gson.toJson(INSTANCE);
    }

    public static void loadFromFile(File file)
    {
        Gson gson = new Gson();
        try
        {
            FileReader fileReader = new FileReader(file);
            INSTANCE = gson.fromJson(fileReader, Config.class);
        } catch (Exception ignored) {}
    }

    public static void saveConfigToFile(File file)
    {
        try (FileOutputStream configOut = new FileOutputStream(file))
        {
            IOUtils.write(Config.saveConfig(), configOut, Charset.defaultCharset());
        } catch (Throwable ignored) {}
    }

    public static void init(File file)
    {
        try
        {
            if (!file.exists())
            {
                Config.INSTANCE = new Config();

                FileWriter tileWriter = new FileWriter(file);
                tileWriter.write(Config.saveConfig());
                tileWriter.close();
            }
            else
            {
                Config.loadFromFile(file);
            }
        } catch (Exception ignored) {}
    }
}
