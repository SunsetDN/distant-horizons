package com.seibel.distanthorizons.neoforge.mixins.client;

import com.seibel.distanthorizons.common.commonMixins.MixinImmersivePortalsRenderStatesCommon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "qouteall.imm_ptl.core.render.context_management.RenderStates")
public class MixinImmersivePortalsRenderStates
{
	
	@Inject(method = "updatePreRenderInfo", at = @At("HEAD"))
	private static void preRender(CallbackInfo ci)
	{
		MixinImmersivePortalsRenderStatesCommon.saveVolatileOriginals();
	}
	
}
