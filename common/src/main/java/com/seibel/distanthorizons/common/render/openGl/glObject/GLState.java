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

package com.seibel.distanthorizons.common.render.openGl.glObject;

import com.seibel.distanthorizons.common.render.openGl.glObject.enums.GLEnums;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftGLWrapper;
import org.lwjgl.opengl.GL33;

public class GLState implements AutoCloseable
{
	private static final MinecraftGLWrapper GLMC = MinecraftGLWrapper.INSTANCE;
	
	
	public int program;
	public int vao;
	public int vbo;
	public int ebo;
	public int fbo;
	public int texture2D;
	/** IE: GL_TEXTURE0, GL_TEXTURE1, etc. */
	public int activeTextureNumber;
	public int texture0;
	public int texture1;
	public int texture2;
	public int texture3;
	public int frameBufferTexture0;
	public int frameBufferTexture1;
	public int frameBufferDepthTexture;
	public boolean blend;
	public boolean scissor;
	public int blendEqRGB;
	public int blendEqAlpha;
	public int blendSrcColor;
	public int blendSrcAlpha;
	public int blendDstColor;
	public int blendDstAlpha;
	public boolean depth;
	public boolean writeToDepthBuffer;
	public int depthFunc;
	public boolean stencil;
	public int stencilFunc;
	public int stencilRef;
	public int stencilMask;
	public int[] view;
	public boolean cull;
	public int cullMode;
	public int polyMode;
	
	
	
	public GLState() { this.saveState(); }
	
	public void saveState()
	{
		this.program = GL33.glGetInteger(GL33.GL_CURRENT_PROGRAM);
		this.vao = GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING);
		this.vbo = GL33.glGetInteger(GL33.GL_ARRAY_BUFFER_BINDING);
		this.ebo = GL33.glGetInteger(GL33.GL_ELEMENT_ARRAY_BUFFER_BINDING);
		
		this.fbo = GL33.glGetInteger(GL33.GL_FRAMEBUFFER_BINDING);
		
		this.texture2D = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
		this.activeTextureNumber = GL33.glGetInteger(GL33.GL_ACTIVE_TEXTURE);
		
		GLMC.glActiveTexture(GL33.GL_TEXTURE0);
		this.texture0 = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
		
		GLMC.glActiveTexture(GL33.GL_TEXTURE1);
		this.texture1 = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
		
		GLMC.glActiveTexture(GL33.GL_TEXTURE2); // problem with Iris
		this.texture2 = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
		
		GLMC.glActiveTexture(GL33.GL_TEXTURE3);
		this.texture3 = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
		
		GLMC.glActiveTexture(this.activeTextureNumber);
		
		if (this.fbo != 0)
		{
			this.frameBufferTexture0 = GL33.glGetFramebufferAttachmentParameteri(GL33.GL_FRAMEBUFFER, GL33.GL_COLOR_ATTACHMENT0, GL33.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
			this.frameBufferTexture1 = GL33.glGetFramebufferAttachmentParameteri(GL33.GL_FRAMEBUFFER, GL33.GL_COLOR_ATTACHMENT1, GL33.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
			
			int depthType = GL33.glGetFramebufferAttachmentParameteri(GL33.GL_FRAMEBUFFER, GL33.GL_DEPTH_ATTACHMENT, GL33.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
			this.frameBufferDepthTexture = (depthType == GL33.GL_TEXTURE) ? GL33.glGetFramebufferAttachmentParameteri(GL33.GL_FRAMEBUFFER, GL33.GL_DEPTH_ATTACHMENT, GL33.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME) : 0;
		}
		else
		{
			// attempting to get values from the default framebuffer can throw errors on Linux
			this.frameBufferTexture0 = 0;
			this.frameBufferTexture1 = 0;
			this.frameBufferDepthTexture = 0;
		}
		
		this.blend = GL33.glIsEnabled(GL33.GL_BLEND);
		this.scissor = GL33.glIsEnabled(GL33.GL_SCISSOR_TEST);
		this.blendEqRGB = GL33.glGetInteger(GL33.GL_BLEND_EQUATION_RGB);
		this.blendEqAlpha = GL33.glGetInteger(GL33.GL_BLEND_EQUATION_ALPHA);
		this.blendSrcColor = GL33.glGetInteger(GL33.GL_BLEND_SRC_RGB);
		this.blendSrcAlpha = GL33.glGetInteger(GL33.GL_BLEND_SRC_ALPHA);
		this.blendDstColor = GL33.glGetInteger(GL33.GL_BLEND_DST_RGB);
		this.blendDstAlpha = GL33.glGetInteger(GL33.GL_BLEND_DST_ALPHA);
		this.depth = GL33.glIsEnabled(GL33.GL_DEPTH_TEST);
		this.writeToDepthBuffer = GL33.glGetInteger(GL33.GL_DEPTH_WRITEMASK) == GL33.GL_TRUE;
		this.depthFunc = GL33.glGetInteger(GL33.GL_DEPTH_FUNC);
		this.stencil = GL33.glIsEnabled(GL33.GL_STENCIL_TEST);
		this.stencilFunc = GL33.glGetInteger(GL33.GL_STENCIL_FUNC);
		this.stencilRef = GL33.glGetInteger(GL33.GL_STENCIL_REF);
		this.stencilMask = GL33.glGetInteger(GL33.GL_STENCIL_VALUE_MASK);
		this.view = new int[4];
		GL33.glGetIntegerv(GL33.GL_VIEWPORT, this.view);
		this.cull = GL33.glIsEnabled(GL33.GL_CULL_FACE);
		this.cullMode = GL33.glGetInteger(GL33.GL_CULL_FACE_MODE);
		this.polyMode = GL33.glGetInteger(GL33.GL_POLYGON_MODE);
	}
	
	@Override 
	public void close()
	{
		// explicitly unbinding the frame buffer is necessary to prevent GL_CLEAR calls from hitting the wrong buffer
		GLMC.glBindFramebuffer(GL33.GL_FRAMEBUFFER, 0);
		boolean frameBufferSet = false;
		
		if (this.fbo != 0 && GL33.glIsFramebuffer(this.fbo))
		{
			GLMC.glBindFramebuffer(GL33.GL_FRAMEBUFFER, this.fbo);
			frameBufferSet = true;
		}
		
		
		if (this.blend)
		{
			GLMC.enableBlend();
		}
		else
		{
			GLMC.disableBlend();
		}
		
		if (this.scissor)
		{
			GLMC.enableScissorTest();
		}
		else
		{
			GLMC.disableScissorTest();
		}
		
		GLMC.glActiveTexture(GL33.GL_TEXTURE0);
		GLMC.glBindTexture(GL33.glIsTexture(this.texture0) ? this.texture0 : 0);
		
		GLMC.glActiveTexture(GL33.GL_TEXTURE1);
		GLMC.glBindTexture(GL33.glIsTexture(this.texture1) ? this.texture1 : 0);
		
		GLMC.glActiveTexture(GL33.GL_TEXTURE2);
		GLMC.glBindTexture(GL33.glIsTexture(this.texture2) ? this.texture2 : 0);
		
		GLMC.glActiveTexture(GL33.GL_TEXTURE3);
		GLMC.glBindTexture(GL33.glIsTexture(this.texture3) ? this.texture3 : 0);
		
		GLMC.glActiveTexture(this.activeTextureNumber);
		GLMC.glBindTexture(GL33.glIsTexture(this.texture2D) ? this.texture2D : 0);
		
		// attempting to set textures on the default frame buffer (ID 0) will throw errors
		if (frameBufferSet)
		{
			if (GL33.glIsTexture(this.frameBufferTexture0))
			{
				GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER, GL33.GL_COLOR_ATTACHMENT0, GL33.GL_TEXTURE_2D, this.frameBufferTexture0, 0);
			}
			
			if (this.frameBufferTexture1 != 0 && GL33.glIsTexture(this.frameBufferTexture1))
			{
				GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER, GL33.GL_COLOR_ATTACHMENT1, GL33.GL_TEXTURE_2D, this.frameBufferTexture1, 0);
			}
			
			if (GL33.glIsTexture(this.frameBufferDepthTexture))
			{
				GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER, GL33.GL_DEPTH_ATTACHMENT, GL33.GL_TEXTURE_2D, this.frameBufferDepthTexture, 0);
			}
				
		}
		
		GL33.glBindVertexArray(GL33.glIsVertexArray(this.vao) ? this.vao : 0);
		GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, GL33.glIsBuffer(this.vbo) ? this.vbo : 0);
		GL33.glBindBuffer(GL33.GL_ELEMENT_ARRAY_BUFFER, GL33.glIsBuffer(this.ebo) ? this.ebo: 0);
		GL33.glUseProgram(GL33.glIsProgram(this.program) ? this.program : 0);
		
		if (this.writeToDepthBuffer)
		{
			GLMC.enableDepthMask();
		}
		else
		{
			GLMC.disableDepthMask();
		}
		
		GLMC.glBlendFunc(this.blendSrcColor, this.blendDstColor);
		GL33.glBlendEquationSeparate(this.blendEqRGB, this.blendEqAlpha);
		GLMC.glBlendFuncSeparate(this.blendSrcColor, this.blendDstColor, this.blendSrcAlpha, this.blendDstAlpha);
		
		if (this.depth)
		{
			GLMC.enableDepthTest();
		}
		else
		{
			GLMC.disableDepthTest();
		}
		GLMC.glDepthFunc(this.depthFunc);
		
		if (this.stencil)
		{
			GL33.glEnable(GL33.GL_STENCIL_TEST);
		}
		else
		{
			GL33.glDisable(GL33.GL_STENCIL_TEST);
		}
		GL33.glStencilFunc(this.stencilFunc, this.stencilRef, this.stencilMask);
		
		GLMC.glViewport(this.view[0], this.view[1], this.view[2], this.view[3]);
		if (this.cull)
		{
			GLMC.enableFaceCulling();
		}
		else
		{
			GLMC.disableFaceCulling();
		}
		GL33.glCullFace(this.cullMode);
		GL33.glPolygonMode(GL33.GL_FRONT_AND_BACK, this.polyMode);
	}
	
	@Override
	public String toString()
	{
		return "GLState{" +
			"program=" + this.program + ", vao=" + this.vao + ", vbo=" + this.vbo + ", ebo=" + this.ebo + ", fbo=" + this.fbo +
			", text=" + GLEnums.getString(this.texture2D) + "@" + this.activeTextureNumber + ", text0=" + GLEnums.getString(this.texture0) +
			", FB text0=" + this.frameBufferTexture0 +
			", FB text1=" + this.frameBufferTexture1 +
			", FB depth=" + this.frameBufferDepthTexture +
			", blend=" + this.blend + ", scissor=" + this.scissor + ", blendMode=" + GLEnums.getString(this.blendSrcColor) + "," + GLEnums.getString(this.blendDstColor) +
			", depth=" + this.depth +
			", depthFunc=" + GLEnums.getString(this.depthFunc) + ", stencil=" + this.stencil +
			", stencilFunc=" + GLEnums.getString(this.stencilFunc) + ", stencilRef=" + this.stencilRef + ", stencilMask=" + this.stencilMask +
			", view={x:" + this.view[0] + ", y:" + this.view[1] +
			", w:" + this.view[2] + ", h:" + this.view[3] + "}" + ", cull=" + this.cull +
			", cullMode=" + GLEnums.getString(this.cullMode) + ", polyMode=" + GLEnums.getString(this.polyMode) +
			'}';
	}
	
	
	
}
