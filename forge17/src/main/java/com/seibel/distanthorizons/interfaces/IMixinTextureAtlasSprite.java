package com.seibel.distanthorizons.interfaces;

public interface IMixinTextureAtlasSprite 
{
	/** packed in ARGB format */
    int[] distanthorizons$getSpriteData();
	
	/** set from the base texture */
    void distanthorizons$setSpriteData();
	
}
