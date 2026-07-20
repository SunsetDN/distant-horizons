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

package com.seibel.distanthorizons.cleanroom;

import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class CleanroomMixinPlugin implements IMixinConfigPlugin
{
	private static final Logger LOGGER = LogManager.getLogger();
	
	@Override
	public void onLoad(String mixinPackage)
	{ }
	
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
/*		return switch (mixinClassName.split("\\.")[5]) {
			case "mist" -> Loader.isModLoaded("mist");
			default -> true;	
		};*/
		return true;
	}
	
	@Override public String getRefMapperConfig() { return null; }
	@Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
	@Override public List<String> getMixins() { return null; }
	@Override public void preApply(String targetClassName, ClassNode classNode, String mixinClassName, IMixinInfo mixinInfo) { }
	@Override public void postApply(String targetClassName, ClassNode classNode, String mixinClassName, IMixinInfo mixinInfo) { }
	
}
