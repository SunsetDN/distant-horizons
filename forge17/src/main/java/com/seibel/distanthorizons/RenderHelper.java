package com.seibel.distanthorizons;

import java.nio.FloatBuffer;

import com.seibel.distanthorizons.common.commonMixins.MixinVanillaFogCommon;
import net.minecraft.client.Minecraft;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.util.math.DhMat4f;
import com.seibel.distanthorizons.forge.ForgeMain;

/*
Since 1.7.10 doesn't natively support modern OpenGL and lwjgl3, we use lwjgl3ify.
lwjgl3ify uses class transformers to rewrite lwjgl2 calls to lwjgl3.
Since we use lwjgl3 and modern OpenGL directly we set the "Lwjgl3ify-Aware" in out mod manifest.
However, this means we can't use modern OpenGL calls in Mixins, that's why this helper exists.
*/
public class RenderHelper 
{

    public static void drawLods() 
    {
        ClientApi.RENDER_STATE.mcModelViewMatrix = getModelViewMatrix();
        ClientApi.RENDER_STATE.mcProjectionMatrix = getProjectionMatrix();
        ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapper(Minecraft.getMinecraft().theWorld);

        if (ForgeMain.angelicaCompat == null) 
		{
            GL32.glDisable(GL32.GL_ALPHA_TEST);
        }
        GL11.glClearColor(1, 1, 1, 0.0F);
        int oldActiveTex = GL11.glGetInteger(GL32.GL_ACTIVE_TEXTURE);
        int oldBoundTex = GL11.glGetInteger(GL32.GL_TEXTURE_BINDING_2D);
        ClientApi.INSTANCE.renderLods();
        GL32.glDepthFunc(GL32.GL_LEQUAL);
        if (ForgeMain.angelicaCompat == null) 
		{
            GL32.glEnable(GL32.GL_ALPHA_TEST);
        }
        GL32.glDisable(GL32.GL_BLEND);
        GL32.glActiveTexture(oldActiveTex);
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, oldBoundTex);
    }

    public static void beforeWater() { GL11.glDepthMask(true); }

    public static void drawLodsFade(boolean translucent) 
    {
        if (ForgeMain.angelicaCompat != null
            && !ForgeMain.angelicaCompat.canDoFadeShader()) 
		{
			return;
        }
        ClientApi.RENDER_STATE.mcModelViewMatrix = getModelViewMatrix();
        ClientApi.RENDER_STATE.mcProjectionMatrix = getProjectionMatrix();
        ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapper(Minecraft.getMinecraft().theWorld);
	    
	    if (ForgeMain.angelicaCompat == null)
	    {
		    GL32.glDisable(GL32.GL_ALPHA_TEST);
	    }
		
	    if (translucent)
	    {
		    ClientApi.INSTANCE.renderFadeTransparent();
	    }
	    else
	    {
		    ClientApi.INSTANCE.renderFadeOpaque();
	    }
	    if (ForgeMain.angelicaCompat == null)
	    {
		    GL32.glEnable(GL32.GL_ALPHA_TEST);
	    }

        GL32.glDepthFunc(GL32.GL_LEQUAL);
        GL32.glDisable(GL32.GL_BLEND);
    }

    public static void drawDeferredLods() 
    {
        if (ForgeMain.angelicaCompat == null) 
		{
            return;
        }
        ClientApi.RENDER_STATE.mcModelViewMatrix = getModelViewMatrix();
        ClientApi.RENDER_STATE.mcProjectionMatrix = getProjectionMatrix();
        ClientApi.RENDER_STATE.clientLevelWrapper = ClientLevelWrapper.getWrapper(Minecraft.getMinecraft().theWorld);

        ClientApi.INSTANCE.renderDeferredLodsForShaders();
    }

    private static DhMat4f modelViewMatrix;
    private static DhMat4f projectionMatrix;

    public static DhMat4f getModelViewMatrix() { return new DhMat4f(modelViewMatrix); }

    public static DhMat4f getProjectionMatrix() { return new DhMat4f(projectionMatrix); }

    public static void setModelViewMatrix(FloatBuffer modelview) { modelViewMatrix = McObjectConverter.convert(new Matrix4f(modelview)); }

    public static void setProjectionMatrix(FloatBuffer projection) {  projectionMatrix = McObjectConverter.convert(new Matrix4f(projection));  }

	/** 
	 * Unbinding is necessary to prevent 
	 * a crash if DH is enabled while MC starts loading into the world 
	 */
    public static void UnbindAfterTesselatorDraw() { GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0); }

    public static void enableFog() { GL11.glEnable(GL11.GL_FOG); }

    public static void disableFog() 
    {
        if (!MixinVanillaFogCommon.cancelFog()) 
		{
            return;
        }
        GL11.glDisable(GL11.GL_FOG);
		
        // Extremely high values cause issues, but 15 mebi-meters out should be 
	    // practically infinite For Angelica
        GL11.glFogf(GL11.GL_FOG_START, 1024 * 1024 * 15);
        GL11.glFogf(GL11.GL_FOG_END, 1024 * 1024 * 16);
    }
	
	public static void glEnable(int cap)
	{
		// Cancel enabling of fog if needed
		if (MixinVanillaFogCommon.cancelFog()
			&& cap == GL11.GL_FOG)
		{
			return;
		}
		
		GL11.glEnable(cap);
	}
	
	
	
}
