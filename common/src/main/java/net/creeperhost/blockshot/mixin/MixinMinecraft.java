package net.creeperhost.blockshot.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MixinMinecraft {
    @Accessor("fps")
    static int getfps() {
        throw new AssertionError();
    }
}
