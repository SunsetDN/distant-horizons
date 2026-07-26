package com.seibel.distanthorizons.forgearchaic.wrappers.modCompat;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;

import gregtech.api.interfaces.IBlockWithTextures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.common.render.GTRenderedTexture;
import org.jetbrains.annotations.Nullable;

public class GregTechCompat 
{
	@Nullable
    public IIcon resolveIcon(Block block, int meta)
    {
        if (!(block instanceof IBlockWithTextures))
		{
			return null;
		}
	    IBlockWithTextures blockWithTextures = (IBlockWithTextures) block;
		
        ITexture[][] textures = blockWithTextures.getTextures(meta);
        if (textures == null 
	        || textures[0] == null) 
		{
			return null;
		}
		
        ITexture firstTexture = textures[0][0];
        if (!(firstTexture instanceof GTRenderedTexture)) 
		{
           return null;
        }
	    GTRenderedTexture renderedTexture = (GTRenderedTexture) firstTexture;
	    
	    IIconContainer container = (IIconContainer) this.getObjectByReflection(renderedTexture, "mIconContainer");
	    if (container != null)
	    {
		    return container.getIcon();
	    }
		
        return null;
    }
	private Object getObjectByReflection(Object base, String name)
	{
		try
		{
			Field field = base.getClass()
				.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(base);
		}
		catch (NoSuchFieldException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	
	
}
