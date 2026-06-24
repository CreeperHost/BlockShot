package net.creeperhost.blockshot.lib;

import com.mojang.authlib.GameProfile;
import net.creeperhost.minetogether.session.MojangUtils;
import net.creeperhost.minetogether.session.SessionProvider;
import net.creeperhost.minetogether.session.data.mc.ProfileKeyPairResponse;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.UUID;

public class MTSessionProvider implements SessionProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Minecraft minecraft = Minecraft.getMinecraft();

    @Override
    public UUID getUUID() {
        return profile().getId();
    }

    @Override
    public String getUsername() {
        return minecraft.getSession().getUsername();
    }

    @Override
    public String beginAuth() throws IOException {
        return MojangUtils.joinServer(getUUID(), minecraft.getSession().getToken());
    }

    @Override
    public ProfileKeyPairResponse getProfileKeyPair() throws IOException {
        return MojangUtils.getProfileKeypair(minecraft.getSession().getToken());
    }

    @Override
    public void infoLog(String msg, Object... args) {
        LOGGER.info(msg, args);
    }

    @Override
    public void warnLog(String msg, Object... args) {
        LOGGER.warn(msg, args);
    }

    @Override
    public void errorLog(String msg, Object... args) {
        LOGGER.error(msg, args);
    }

    @Override
    public String describe() {
        return "BlockShot Forge 1.12.2";
    }

    private GameProfile profile() {
        return minecraft.getSession().getProfile();
    }
}
