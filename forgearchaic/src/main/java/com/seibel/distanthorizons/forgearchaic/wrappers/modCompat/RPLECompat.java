package com.seibel.distanthorizons.forgearchaic.wrappers.modCompat;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.DynamicTexture;

import com.falsepattern.rple.api.client.RPLELightMapAPI;
import com.falsepattern.rple.api.common.ServerColorHelper;
import com.falsepattern.rple.api.common.block.RPLEBlock;

/**
 * adds colored lighting
 */
public class RPLECompat 
{
    DynamicTexture lightmapTexture;
    int[] lightmapColors;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
    public RPLECompat() 
    {
        this.lightmapTexture = new DynamicTexture(16, 16);
	    this.lightmapColors = this.lightmapTexture.getTextureData();
    }
	
	//endregion
	
	
	
	//=========//
	// getters //
	//=========//
	//region
	
    public int getTextureId() { return this.lightmapTexture.getGlTextureId(); }

    public int getColor(Block block, int meta) 
    {
        short color = RPLEBlock.of(block)
            .rple$getBrightnessColor(meta);
        return ServerColorHelper.lightValueFromRGB16(color);
    }
	//endregion
	
	
	
	//==========//
	// lightmap //
	//==========//
	//region
	
	public void updateLightmap() 
    {
        RPLELightMapAPI.getMixedLightMapData(this.lightmapColors);
	    this.lightmapTexture.updateDynamicTexture();
    }
	
	//endregion
	
	
	
}
