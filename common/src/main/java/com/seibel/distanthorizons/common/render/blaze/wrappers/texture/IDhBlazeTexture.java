package com.seibel.distanthorizons.common.render.blaze.wrappers.texture;

#if MC_VER <= MC_1_21_10
public interface IDhBlazeTexture {}
#else

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

public interface IDhBlazeTexture
{
	
	GpuTexture getTexture();
	GpuTextureView getTextureView();
	GpuSampler getTextureSampler();
	
}
#endif