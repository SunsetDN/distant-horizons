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

package com.seibel.distanthorizons.common.wrappers.minecraft;

#if MC_VER <= MC_1_7_10
#elif MC_VER <= MC_1_12_2
import net.minecraft.client.renderer.GlStateManager;
#elif MC_VER < MC_1_21_5
import com.mojang.blaze3d.platform.GlStateManager;
#else
import com.mojang.blaze3d.opengl.GlStateManager;
#endif

import com.seibel.distanthorizons.core.jar.EPlatform;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;

import com.seibel.distanthorizons.core.logging.DhLogger;
import org.lwjgl.opengl.GL33;


/**
 * <b>Why does DH often call GL methods twice? </b><br>
 * Once using the base {@link GL33} function and a second time using
 * Minecraft's {@link GlStateManager}?<br><br>
 *
 * <b>Answer: </b><br>
 * Compatibility and robustness<br>
 * In general all MC rendering should go through MC's {@link GlStateManager},
 * however that isn't always the case.
 * So, to prevent issues if a mod (or MC itself) calls a direct GL function
 * instead of the {@link GlStateManager} wrapper, we need to be sure about what the actual
 * set value is (whether setting or getting) and that MC knows what DH has done.
 * This way whether a mod (or MC) is using the {@link GlStateManager} or direct GL calls,
 * they should always have the correct value for anything DH has modified.
 * <br><br>
 * This may slow down some low end GPUs that are driver limited,
 * however James would rather have slow correct rendering vs fast broken rendering.
 */
public class MinecraftGLWrapper
{
	public static final MinecraftGLWrapper INSTANCE = new MinecraftGLWrapper();

	private static final DhLogger LOGGER = new DhLoggerBuilder().build();



	/*
    private static final StencilState STENCIL;
	 */

	// scissor //
	//region

	/** @see GL33#GL_SCISSOR_TEST */
	public void enableScissorTest()
	{
		GL33.glEnable(GL33.GL_SCISSOR_TEST);
		#if MC_VER > MC_1_12_2
		GlStateManager._enableScissorTest();
		#endif
	}
	/** @see GL33#GL_SCISSOR_TEST */
	public void disableScissorTest()
	{
		GL33.glDisable(GL33.GL_SCISSOR_TEST);
		#if MC_VER > MC_1_12_2
		GlStateManager._disableScissorTest();
		#endif
	}

	//endregion



	// stencil //
	//region

//	/** @see GL33#GL_SCISSOR_TEST */
//	public void enableScissorTest() { GlStateManager._stencilFunc(); }
//	/** @see GL33#GL_SCISSOR_TEST */
//	public void disableScissorTest() { GlStateManager._disableScissorTest(); }

	//endregion



	// depth //
	//region

	/** @see GL33#GL_DEPTH_TEST */
	public void enableDepthTest()
	{
		GL33.glEnable(GL33.GL_DEPTH_TEST);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.enableDepth();
		#elif MC_VER > MC_1_12_2
		GlStateManager._enableDepthTest();
		#endif
	}
	/** @see GL33#GL_DEPTH_TEST */
	public void disableDepthTest()
	{
		GL33.glDisable(GL33.GL_DEPTH_TEST);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.disableDepth();
		#elif MC_VER > MC_1_12_2
		GlStateManager._disableDepthTest();
		#endif
	}

	/** @see GL33#glDepthFunc(int)  */
	public void glDepthFunc(int func)
	{
		GL33.glDepthFunc(func);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.depthFunc(func);
		#elif MC_VER > MC_1_12_2
		GlStateManager._depthFunc(func);
		#endif
	}
	public int getActiveDepthFunc() { return GL33.glGetInteger(GL33.GL_DEPTH_FUNC); }

	/** @see GL33#glDepthMask(boolean) */
	public void enableDepthMask()
	{
		GL33.glDepthMask(true);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.depthMask(true);
		#elif MC_VER > MC_1_12_2
		GlStateManager._depthMask(true);
		#endif
	}
	/** @see GL33#glDepthMask(boolean) */
	public void disableDepthMask()
	{
		GL33.glDepthMask(false);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.depthMask(false);
		#elif MC_VER > MC_1_12_2
		GlStateManager._depthMask(false);
		#endif
	}

	//endregion



	// blending //
	//region

	/** @see GL33#GL_BLEND */
	public void enableBlend()
	{
		GL33.glEnable(GL33.GL_BLEND);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.enableBlend();
		#elif MC_VER > MC_1_12_2 && MC_VER <= MC_26_1_2
		GlStateManager._enableBlend();
		#elif MC_VER > MC_26_1_2
		GlStateManager._enableBlend(0);
		#endif
	}
	/** @see GL33#GL_BLEND */
	public void disableBlend()
	{
		GL33.glDisable(GL33.GL_BLEND);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.disableBlend();
		#elif MC_VER > MC_1_12_2 && MC_VER <= MC_26_1_2
		GlStateManager._disableBlend();
		#elif MC_VER > MC_26_1_2
		GlStateManager._disableBlend(0);
		#endif
	}

	/** @see GL33#glBlendFunc */
	public void glBlendFunc(int sfactor, int dfactor)
	{
		GL33.glBlendFunc(sfactor, dfactor);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.blendFunc(sfactor, dfactor);
		#elif MC_VER > MC_1_12_2 && MC_VER < MC_1_21_5
		GlStateManager._blendFunc(sfactor, dfactor);
		#endif
	}
	/** @see GL33#glBlendFuncSeparate */
	public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha)
	{
		GL33.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.tryBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
		#elif MC_VER > MC_1_12_2
		GlStateManager._blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
		#endif
	}

	//endregion



	// frame buffers //
	//region

	/** @see GL33#glBindFramebuffer */
	public void glBindFramebuffer(int target, int framebuffer)
	{
		GL33.glBindFramebuffer(target, framebuffer);
		#if MC_VER > MC_1_12_2
		GlStateManager._glBindFramebuffer(target, framebuffer);
		#endif
	}

	//endregion



	// buffers //
	//region

	/** @see GL33#glGenBuffers() */
	public int glGenBuffers()
	{ return GL33.glGenBuffers(); }

	/** @see GL33#glDeleteBuffers(int) */
	public void glDeleteBuffers(int buffer)
	{
		GL33.glDeleteBuffers(buffer);

		// MC's implementation has a bug where it will throw:
		// GL_INVALID_OPERATION in glBufferData(immutable)
		// when attempting to delete Storage Buffers
		// So we need to manually delete the buffers ourselves
		//GlStateManager._glDeleteBuffers(buffer);
	}

	//endregion



	// culling //
	//region

	/** @see GL33#GL_CULL_FACE */
	public void enableFaceCulling()
	{
		GL33.glEnable(GL33.GL_CULL_FACE);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.enableCull();
		#elif MC_VER > MC_1_12_2
		GlStateManager._enableCull();
		#endif
	}
	/** @see GL33#GL_CULL_FACE */
	public void disableFaceCulling()
	{
		GL33.glDisable(GL33.GL_CULL_FACE);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.disableCull();
		#elif MC_VER > MC_1_12_2
		GlStateManager._disableCull();
		#endif
	}

	//endregion



	// textures //
	//region

	/** @see GL33#glGenTextures() */
	public int glGenTextures()
	{
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		return GlStateManager.generateTexture();
		#elif MC_VER > MC_1_12_2
		return GlStateManager._genTexture();
		#else
		return GL33.glGenTextures();
		#endif
	}
	/** @see GL33#glDeleteTextures(int) */
	public void glDeleteTextures(int texture)
	{
		#if MC_VER <= MC_1_7_10
		GL33.glDeleteTextures(texture);
		#elif MC_VER <= MC_1_12_2
		GlStateManager.deleteTexture(texture);
		#else
		GlStateManager._deleteTexture(texture);
		#endif
	}

	/** @see GL33#glActiveTexture(int) */
	public void glActiveTexture(int textureId)
	{
		GL33.glActiveTexture(textureId);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.setActiveTexture(textureId);
		#elif MC_VER > MC_1_12_2
		GlStateManager._activeTexture(textureId);
		#endif
	}
	public int getActiveTexture() { return GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D); }

	/**
	 * Always binds to {@link GL33#GL_TEXTURE_2D}
	 * @see GL33#glBindTexture(int, int)
	 */
	public void glBindTexture(int texture)
	{
		GL33.glBindTexture(GL33.GL_TEXTURE_2D, texture);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.bindTexture(texture);
		#elif MC_VER > MC_1_12_2
		GlStateManager._bindTexture(texture);
		#endif
	}

	//endregion



	// viewport //
	//region

	/** @see GL33#glViewport(int, int, int, int) */
	public void glViewport(int x, int y, int viewportWidth, int viewportHeight)
	{
		GL33.glViewport(x, y, viewportWidth, viewportHeight);
		#if MC_VER > MC_1_7_10 && MC_VER <= MC_1_12_2
		GlStateManager.viewport(x, y, viewportWidth, viewportHeight);
		#elif MC_VER > MC_1_12_2
		GlStateManager._viewport(x,y, viewportWidth, viewportHeight);
		#endif
	}

	//endregion



}
