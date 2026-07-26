package com.seibel.distanthorizons.forgearchaic.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.seibel.distanthorizons.forgearchaic.interfaces.IMixinMinecraft;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements IMixinMinecraft 
{
    @Override
    @Accessor("timer")
    public abstract Timer getTimer();
}
