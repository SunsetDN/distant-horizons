/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.common.wrappers.block;

import com.seibel.distanthorizons.coreapi.util.ColorUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

#if MC_VER <= MC_1_7_10
import com.seibel.distanthorizons.interfaces.IMixinTextureAtlasSprite;
#endif

#if MC_VER < MC_1_17_1
#elif MC_VER < MC_1_21_3
#else
import net.minecraft.client.renderer.texture.SpriteContents;
#endif

/**
 * For wrapping/utilizing around TextureAtlasSprite
 *
 * @author Ran
 */
public class TextureAtlasSpriteWrapper
{
	public static int getPixelARGB(TextureAtlasSprite sprite, int frameIndex, int x, int y)
	{
		#if MC_VER <= MC_1_7_10
		// In 1.7.10 the sprite's pixel array isn't publicly accessible, so we rely on a Mixin
		// (see forge17/.../MixinTextureAtlasSprite) which caches the base mipmap level in RGB-low order.
		IMixinTextureAtlasSprite spriteExt = (IMixinTextureAtlasSprite) sprite;
		int[] spriteData = spriteExt.distanthorizons$getSpriteData();
		if (spriteData == null)
		{
			// missing texture sentinel (matches the magenta "missing" colour used elsewhere)
			return 0xFFFF00FF;
		}
		return spriteData[sprite.getIconWidth() * y + x];
		#elif MC_VER <= MC_1_12_2
		int[][] frameData = sprite.getFrameTextureData(frameIndex);
		int argb = frameData[0][y * sprite.getIconWidth() + x];
		return argb;
        #elif MC_VER < MC_1_17_1
        int rgba = sprite.mainImage[0].getPixelRGBA(
                x + sprite.framesX[frameIndex] * sprite.getWidth(),
                y + sprite.framesY[frameIndex] * sprite.getHeight());
        return convertRgbaToArgb(rgba);
        #elif MC_VER < MC_1_19_4
		if (sprite.animatedTexture != null)
		{
			x += sprite.animatedTexture.getFrameX(frameIndex) * sprite.width;
			y += sprite.animatedTexture.getFrameY(frameIndex) * sprite.height;
		}
		int rgba = sprite.mainImage[0].getPixelRGBA(x, y);
		return convertRgbaToArgb(rgba);
		#elif MC_VER < MC_1_21_3
		if (sprite.contents().animatedTexture != null)
		{
			x += sprite.contents().animatedTexture.getFrameX(frameIndex) * sprite.contents().width();
			y += sprite.contents().animatedTexture.getFrameY(frameIndex) * sprite.contents().width();
		}
		int rgba = sprite.contents().originalImage.getPixelRGBA(x, y);
		return convertRgbaToArgb(rgba);
        #else
		
		SpriteContents content = sprite.contents(); // don't close, otherwise MC will be corrupted and you won't be able to re-access the texture
		if (content.animatedTexture != null)
		{
			x += content.animatedTexture.getFrameX(frameIndex) * content.width();
			y += content.animatedTexture.getFrameY(frameIndex) * content.width();
		}
		
		int argb = content.originalImage.getPixel(x, y);
		return argb;
        #endif
	}
	
	// used for some MC versions
	private static int convertRgbaToArgb(int rgba)
	{
		int r = (rgba & 0x000000FF);
		int g = (rgba & 0x0000FF00) >>> 8;
		int b = (rgba & 0x00FF0000) >>> 16;
		int a = (rgba & 0xFF000000) >>> 24;
		return ColorUtil.argbToInt(a, r, g, b);
	}
	
	
	
	public static int getWidth(TextureAtlasSprite texture)
	{
		#if MC_VER <= MC_1_12_2
		return texture.getIconWidth();
        #elif MC_VER < MC_1_19_4
		return texture.getWidth();
        #else
		return texture.contents().width();
        #endif
	}
	public static int getHeight(TextureAtlasSprite texture)
	{
		#if MC_VER <= MC_1_12_2
		return texture.getIconHeight();
        #elif MC_VER < MC_1_19_4
		return texture.getHeight();
        #else
		return texture.contents().height();
        #endif
	}
	
	public static float getMinU(TextureAtlasSprite sprite)
	{
		#if MC_VER <= MC_1_12_2
		return sprite.getMinU();
		#else
		return sprite.getU0();
		#endif
	}
	public static float getMaxU(TextureAtlasSprite sprite)
	{
		#if MC_VER <= MC_1_12_2
		return sprite.getMaxU();
		#else
		return sprite.getU1();
		#endif
	}
	
	public static float getMinV(TextureAtlasSprite sprite)
	{
		#if MC_VER <= MC_1_12_2
		return sprite.getMinV();
		#else
		return sprite.getV0();
		#endif
	}
	public static float getMaxV(TextureAtlasSprite sprite)
	{
		#if MC_VER <= MC_1_12_2
		return sprite.getMaxV();
		#else
		return sprite.getV1();
		#endif
	}
	
	
	
}
