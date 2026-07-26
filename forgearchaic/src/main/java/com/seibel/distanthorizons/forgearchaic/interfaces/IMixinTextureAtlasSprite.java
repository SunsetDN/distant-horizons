package com.seibel.distanthorizons.forgearchaic.interfaces;

public interface IMixinTextureAtlasSprite 
{
	/** packed in ARGB format */
    int[] distanthorizons$getSpriteData();
	
	/** set from the base texture */
    void distanthorizons$setSpriteData();
	
}
