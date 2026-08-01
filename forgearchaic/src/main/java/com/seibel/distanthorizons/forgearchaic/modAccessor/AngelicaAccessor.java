package com.seibel.distanthorizons.forgearchaic.modAccessor;

import java.awt.Color;

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IAngelicaAccessor;
import com.seibel.distanthorizons.forgearchaic.modAccessor.exceptions.AngelicaVersionGuiException;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.common.versioning.VersionRange;
import net.coderbot.iris.rendertarget.IRenderTargetExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import org.joml.Vector3d;

import com.gtnewhorizons.angelica.config.AngelicaConfig;
import com.gtnewhorizons.angelica.glsm.GLStateManager;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;

public class AngelicaAccessor implements IAngelicaAccessor 
{
	
	public static final String ANGELICA_MOD_ID = "angelica";
	public static final String MINIMUM_ANGELICA_VERSION = "2.1.5";
	public static final VersionRange SUPPORTED_ANGELICA_RANGE = VersionParser
		.parseRange("[" + MINIMUM_ANGELICA_VERSION + ",)");
	
	@Override
	public String getModName() { return ANGELICA_MOD_ID; }
	
	
	
	//====================//
	// version validation //
	//====================//
	//region
	
	public void throwIfUnsupportedAngelicaVersion()
		throws IllegalStateException, AngelicaVersionGuiException
	{
		ModContainer angelica = Loader.instance()
			.getIndexedModList()
			.get(ANGELICA_MOD_ID);
		
		if (angelica == null)
		{
			throw new IllegalStateException("Angelica mod container could not be found.");
		}
		
		String installedVersion = angelica.getVersion();
		ArtifactVersion installedArtifactVersion = new DefaultArtifactVersion(installedVersion);
		if (SUPPORTED_ANGELICA_RANGE.containsVersion(installedArtifactVersion))
		{
			return;
		}
		
		throw new AngelicaVersionGuiException(installedVersion, MINIMUM_ANGELICA_VERSION);
	}
	
	//endregion
	
	
	
	//==================//
	// accessor methods //
	//==================//
	//region
	
	@Override
	public int getDepthTextureId() 
    {
	    final Framebuffer framebuffer = Minecraft.getMinecraft().getFramebuffer();
		return ((IRenderTargetExt) framebuffer).iris$getDepthTextureId(); 
	}

	@Override
    public boolean canDoFadeShader() { return AngelicaConfig.enableIris; }

	@Override
    public Color getFogColor() 
    {
        Vector3d color = GLStateManager.getFogColor();
        return new Color(
            Math.max(0.0f, Math.min(1.0f, (float) color.x)),
            Math.max(0.0f, Math.min(1.0f, (float) color.y)),
            Math.max(0.0f, Math.min(1.0f, (float) color.z)));
    }
	
	//endregion
	
	
	
}
