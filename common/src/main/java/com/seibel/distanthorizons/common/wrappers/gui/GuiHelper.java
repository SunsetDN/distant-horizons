package com.seibel.distanthorizons.common.wrappers.gui;

#if MC_VER <= MC_1_12_2
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import java.util.HashMap;
import java.util.Map;
#else
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
#endif

#if MC_VER < MC_1_19_2 && MC_VER > MC_1_12_2
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
#endif

public class GuiHelper
{
	/**
	 * Helper static methods for versional compat
	 */
	#if MC_VER <= MC_1_12_2
	public static final Map<GuiButton, OnPressed> HANDLER_BY_BUTTON = new HashMap<>();
	#endif
	
	#if MC_VER <= MC_1_12_2
	public static GuiButton MakeBtn(ITextComponent base, int posX, int posZ, int width, int height, OnPressed action)
	#else
	public static Button MakeBtn(Component base, int posX, int posZ, int width, int height, Button.OnPress action)
	#endif
	{
		#if MC_VER <= MC_1_12_2
		GuiButton button = new GuiButton(HANDLER_BY_BUTTON.size(), posX, posZ, width, height, base.getFormattedText());
		HANDLER_BY_BUTTON.put(button, action);
		return button;
        #elif MC_VER < MC_1_19_4
		return new Button(posX, posZ, width, height, base, action);
        #else
		return Button.builder(base, action).bounds(posX, posZ, width, height).build();
        #endif
	}
	
	#if MC_VER <= MC_1_12_2
	public static ITextComponent TextOrLiteral(String text)
	#else
	public static MutableComponent TextOrLiteral(String text)
	#endif
	{
		#if MC_VER <= MC_1_12_2
		return new TextComponentString(text);
        #elif MC_VER < MC_1_19_2
		return new TextComponent(text);
        #else
		return Component.literal(text);
        #endif
	}
	
	#if MC_VER <= MC_1_12_2
	public static ITextComponent TextOrTranslatable(String text)
	#else
	public static MutableComponent TextOrTranslatable(String text)
	#endif
	{
		#if MC_VER <= MC_1_12_2
		return new TextComponentString(text);
        #elif MC_VER < MC_1_19_2
		return new TextComponent(text);
        #else
		return Component.translatable(text);
        #endif
	}
	
	#if MC_VER <= MC_1_12_2
	public static ITextComponent Translatable(String text, Object... args)
	#else
	public static MutableComponent Translatable(String text, Object... args)
	#endif
	{
		#if MC_VER <= MC_1_12_2
		return new TextComponentTranslation(text, args);
        #elif MC_VER < MC_1_19_2
		return new TranslatableComponent(text, args);
        #else
		return Component.translatable(text, args);
        #endif
	}
	
	#if MC_VER <= MC_1_12_2
	public static void SetX(GuiButton widget, int x) 
	#else 
	public static void SetX(AbstractWidget widget, int x)
	#endif
	{
        #if MC_VER < MC_1_19_4
		widget.x = x;
        #else
		widget.setX(x);
        #endif
	}
	
	#if MC_VER <= MC_1_12_2
	public static void SetY(GuiTextField textField, int y) { textField.y = y; }
	#endif
	
	#if MC_VER <= MC_1_12_2
	public static void SetY(GuiButton widget, int y)
	#else
	public static void SetY(AbstractWidget widget, int y)
	#endif
	{
        #if MC_VER < MC_1_19_4
		widget.y = y;
        #else
		widget.setY(y);
        #endif
	}
	
}
