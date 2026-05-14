package com.seibel.distanthorizons.common.wrappers.gui;

import com.seibel.distanthorizons.core.wrapperInterfaces.config.ILangWrapper;
#if MC_VER <= MC_1_12_2
import net.minecraft.client.resources.I18n;
#else
import net.minecraft.client.resources.language.I18n;
#endif

public class LangWrapper implements ILangWrapper
{
	public static final LangWrapper INSTANCE = new LangWrapper();
	
	@Override
	public boolean langExists(String str)
	{
		#if MC_VER <= MC_1_12_2
		return I18n.hasKey(str);
		#else
		return I18n.exists(str);
		#endif
	}
	
	@Override
	public String getLang(String str)
	{
		#if MC_VER <= MC_1_12_2
		return I18n.format(str);
		#else
		return I18n.get(str);
		#endif
	}
	
}
