package com.seibel.distanthorizons.common.wrappers.gui;

#if MC_VER <= MC_1_12_2
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
#else
import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
#endif

#if MC_VER <= MC_1_12_2
#elif MC_VER < MC_1_20_1
import com.mojang.blaze3d.vertex.PoseStack;
#elif MC_VER <= MC_1_21_11
import net.minecraft.client.gui.GuiGraphics;
#else
import net.minecraft.client.gui.GuiGraphicsExtractor;
#endif

import java.util.List;

#if MC_VER <= MC_1_12_2
public class DhScreen extends GuiScreen
#else
public class DhScreen extends Screen
#endif
{
	#if MC_VER <= MC_1_12_2
	protected ITextComponent title;
	#endif
	
	#if MC_VER <= MC_1_12_2
	protected DhScreen(ITextComponent title)
	{
		this.title = title;
	}
	#else
	protected DhScreen(Component title)
	{
		super(title);
	}
	#endif
	
	// addRenderableWidget in 1.17 and over
	// addButton in 1.16 and below
	#if MC_VER <= MC_1_12_2
	protected GuiButton addBtn(GuiButton button)
	#else
	protected Button addBtn(Button button)
	#endif
	{
		#if MC_VER <= MC_1_12_2
		this.buttonList.add(button);
		return button;
		#elif MC_VER < MC_1_17_1
        return this.addButton(button);
		#else
		return this.addRenderableWidget(button);
		#endif
	}
	
	#if MC_VER <= MC_1_12_2
	@Override
	protected void actionPerformed(GuiButton button)
	{
		OnPressed handler = GuiHelper.HANDLER_BY_BUTTON.get(button);
		if (handler != null)
		{
			handler.pressed(button);
		}
	}
	
	protected void DhDrawCenteredString(ITextComponent text, int x, int y, int color) {
		drawCenteredString(fontRenderer, text.getFormattedText(), x, y, color);
	}
	
	protected void DhDrawString(ITextComponent text, int x, int y, int color) {
		drawString(fontRenderer, text.getFormattedText(), x, y, color);
	}
	
	protected void DhRenderComponentTooltip(List<ITextComponent> list, int x, int y) {
		drawHoveringText(list.stream().map(ITextComponent::getFormattedText).toList(), x, y, fontRenderer);
	}
	
	protected void DhRenderTooltip(ITextComponent text, int x, int y) {
		drawHoveringText(List.of(text.getFormattedText()), x, y, fontRenderer);
	}
	#elif MC_VER < MC_1_20_1
	protected void DhDrawCenteredString(PoseStack guiStack, Font font, Component text, int x, int y, int color)
	{
		drawCenteredString(guiStack, font, text, x, y, color);
	}
	protected void DhDrawString(PoseStack guiStack, Font font, Component text, int x, int y, int color)
	{
		drawString(guiStack, font, text, x, y, color);
	}
	protected void DhRenderTooltip(PoseStack guiStack, Font font, List<? extends net.minecraft.util.FormattedCharSequence> text, int x, int y)
	{
		renderTooltip(guiStack, text, x, y);
	}
	protected void DhRenderComponentTooltip(PoseStack guiStack, Font font, List<Component> comp, int x, int y)
	{
		renderComponentTooltip(guiStack, comp, x, y);
	}
	protected void DhRenderTooltip(PoseStack guiStack, Font font, Component comp, int x, int y)
	{
		renderTooltip(guiStack, comp, x, y);
	}
	#elif MC_VER < MC_1_21_6
	protected void DhDrawCenteredString(GuiGraphics guiStack, Font font, Component text, int x, int y, int color)
	{
		guiStack.drawCenteredString(font, text, x, y, color);
	}
	protected void DhDrawString(GuiGraphics guiStack, Font font, Component text, int x, int y, int color)
	{
		guiStack.drawString(font, text, x, y, color);
	}
	//protected void DhRenderTooltip(GuiGraphics guiStack, Font font, List<? extends net.minecraft.util.FormattedCharSequence> text, int x, int y)
	//{
	//	guiStack.renderTooltip(font, text, x, y);
	//}
	protected void DhRenderComponentTooltip(GuiGraphics guiStack, Font font, List<Component> comp, int x, int y)
	{
		guiStack.renderComponentTooltip(font, comp, x, y);
	}
	protected void DhRenderTooltip(GuiGraphics guiStack, Font font, Component text, int x, int y)
	{
		guiStack.renderTooltip(font, text, x, y);
	}
	#elif MC_VER <= MC_1_21_11
	protected void DhDrawCenteredString(GuiGraphics guiStack, Font font, Component text, int x, int y, int color)
	{
		guiStack.drawCenteredString(font, text, x, y, color);
	}
	protected void DhDrawString(GuiGraphics guiStack, Font font, Component text, int x, int y, int color)
	{
		guiStack.drawString(font, text, x, y, color);
	}
	protected void DhRenderComponentTooltip(GuiGraphics guiStack, Font font, List<Component> comp, int x, int y)
	{
		guiStack.setComponentTooltipForNextFrame(font, comp, x, y);
	}
	protected void DhRenderTooltip(GuiGraphics guiStack, Font font, Component text, int x, int y)
	{
		guiStack.setTooltipForNextFrame(font, text, x, y);
	}
	#else
	protected void DhDrawCenteredString(GuiGraphicsExtractor guiStack, Font font, Component text, int x, int y, int color)
	{
		guiStack.centeredText(font, text, x, y, color);
	}
	protected void DhDrawString(GuiGraphicsExtractor guiStack, Font font, Component text, int x, int y, int color)
	{
		guiStack.text(font, text, x, y, color);
	}
	protected void DhRenderComponentTooltip(GuiGraphicsExtractor guiStack, Font font, List<Component> comp, int x, int y)
	{
		guiStack.setComponentTooltipForNextFrame(font, comp, x, y);
	}
	protected void DhRenderTooltip(GuiGraphicsExtractor guiStack, Font font, Component text, int x, int y)
	{
		guiStack.setTooltipForNextFrame(font, text, x, y);
	}
    #endif
}
