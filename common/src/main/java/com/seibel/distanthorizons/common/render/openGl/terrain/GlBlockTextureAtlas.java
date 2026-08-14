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

package com.seibel.distanthorizons.common.render.openGl.terrain;

import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftGLWrapper;
import com.seibel.distanthorizons.core.dataObjects.render.textures.BlockTextureRegistry;
import com.seibel.distanthorizons.core.render.AbstractBlockTextureAtlas;
import org.lwjgl.opengl.GL33;

import java.nio.ByteBuffer;

/**
 * The GPU side of the {@link BlockTextureRegistry},
 * a texture array with one layer per block face tile. <br><br>
 *
 * Layer {@link BlockTextureRegistry#UNTEXTURED_ID} is a uniform 1.0 color multiplier
 * so vertices without a texture render exactly like flat-colored LODs.
 *
 * @see BlockTextureRegistry
 */
public class GlBlockTextureAtlas extends AbstractBlockTextureAtlas
{
	public static final GlBlockTextureAtlas INSTANCE = new GlBlockTextureAtlas();
	
	private static final MinecraftGLWrapper GLMC = MinecraftGLWrapper.INSTANCE;
	
	/** the texture unit the atlas is bound to while LODs render, the lightmap uses unit 0 */
	public static final int GL_BOUND_INDEX = 1;
	
	
	private final PixelUploadGlState uploadGlState = new PixelUploadGlState();
	
	private int textureId = 0;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private GlBlockTextureAtlas() {}
	
	//endregion
	
	
	
	//==================//
	// texture handling //
	//==================//
	//region
	
	@Override
	protected void tryCreateOrResize(int width, int height)
	{
		if (this.textureId != 0)
		{
			GL33.glDeleteTextures(this.textureId);
		}
		
		this.textureId = GL33.glGenTextures();
		GL33.glBindTexture(GL33.GL_TEXTURE_2D, this.textureId);
		GL33.glTexImage2D(
			GL33.GL_TEXTURE_2D, 0,
			GL33.GL_RGBA8,
			width, height,
			0,
			GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE,
			(ByteBuffer) null
		);
		
		// nearest filtering keeps the blocky look and prevents
		// texels bleeding between unrelated tiles on adjacent layers
		GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST_MIPMAP_LINEAR);
		GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST);
		GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_S, GL33.GL_REPEAT);
		GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_WRAP_T, GL33.GL_REPEAT);
	}
	
	public int getTextureId() { return this.textureId; }
	
	//endregion
	
	
	
	//=========//
	// binding //
	//=========//
	//region
	
	public void bind()
	{
		GL33.glActiveTexture(GL33.GL_TEXTURE0 + GL_BOUND_INDEX);
		GL33.glBindSampler(GL_BOUND_INDEX, 0); // MC's sampler is probably LINEAR instead of NEAREST, which causes the textures to render blurry
		GL33.glBindTexture(GL33.GL_TEXTURE_2D, this.textureId);
		GL33.glActiveTexture(GL33.GL_TEXTURE0);
	}
	
	public void unbind()
	{
		GL33.glActiveTexture(GL33.GL_TEXTURE0 + GL_BOUND_INDEX);
		GL33.glBindTexture(GL33.GL_TEXTURE_2D, 0);
		GL33.glActiveTexture(GL33.GL_TEXTURE0);
	}
	
	//endregion
	
	
	
	//===========//
	// uploading //
	//===========//
	//region
	
	@Override
	protected void beforeWriteToTexture()
	{
		this.uploadGlState.saveState();
		
		GL33.glBindTexture(GL33.GL_TEXTURE_2D, this.textureId);
		
		GL33.glPixelStorei(GL33.GL_UNPACK_ROW_LENGTH, BlockTextureRegistry.TILE_HEIGHT_AND_WIDTH);
		GL33.glPixelStorei(GL33.GL_UNPACK_SKIP_PIXELS, 0);
		GL33.glPixelStorei(GL33.GL_UNPACK_SKIP_ROWS, 0);
		GL33.glPixelStorei(GL33.GL_UNPACK_ALIGNMENT, 1);
	}
	
	@Override
	protected void writeToTexture(ByteBuffer pixelBuffer, int destinationX, int destinationY, int tileWidth, int tileHeight)
	{
		GL33.glTexSubImage2D(
			GL33.GL_TEXTURE_2D, 0,
			destinationX, destinationY,
			tileWidth, tileHeight,
			GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, pixelBuffer
		);
	}
	
	@Override
	protected void afterWriteToTexture()
	{
		GL33.glGenerateMipmap(GL33.GL_TEXTURE_2D);
		this.uploadGlState.close();
	}
	
	//endregion
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	private static class PixelUploadGlState implements AutoCloseable
	{
		private int unpackRowLength = 0;
		private int unpackSkipPixels = 0;
		private int unpackSkipRows = 0;
		private int unpackAlignment = 0;
		
		private int textureBinding = 0;
		
		
		
		public void saveState()
		{
			this.unpackRowLength = GL33.glGetInteger(GL33.GL_UNPACK_ROW_LENGTH);
			this.unpackSkipPixels = GL33.glGetInteger(GL33.GL_UNPACK_SKIP_PIXELS);
			this.unpackSkipRows = GL33.glGetInteger(GL33.GL_UNPACK_SKIP_ROWS);
			this.unpackAlignment = GL33.glGetInteger(GL33.GL_UNPACK_ALIGNMENT);
			
			GLMC.glActiveTexture(GL33.GL_TEXTURE0);
			this.textureBinding = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
		}
		
		@Override
		public void close()
		{
			GL33.glBindTexture(GL33.GL_TEXTURE_2D, this.textureBinding);
			
			GL33.glPixelStorei(GL33.GL_UNPACK_ROW_LENGTH, this.unpackRowLength);
			GL33.glPixelStorei(GL33.GL_UNPACK_SKIP_PIXELS, this.unpackSkipPixels);
			GL33.glPixelStorei(GL33.GL_UNPACK_SKIP_ROWS, this.unpackSkipRows);
			GL33.glPixelStorei(GL33.GL_UNPACK_ALIGNMENT, this.unpackAlignment);
		}
		
	}
	
	//endregion
	
	
	
}
