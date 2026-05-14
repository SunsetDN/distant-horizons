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

package com.seibel.distanthorizons.common.wrappers;

import java.nio.FloatBuffer;

import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.math.Mat4f;

#if MC_VER <= MC_1_12_2
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
#else
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
#endif

/**
 * This class converts to and from Minecraft objects (Ex: Matrix4f)
 * and objects we created (Ex: Mat4f).
 *
 * @author James Seibel
 * @version 11-20-2021
 */
public class McObjectConverter
{
	private static int bufferIndex(int x, int y)
	{
		return y * 4 + x;
	}
	
	
	/** 4x4 float matrix converter */
	public static Mat4f Convert(
			#if MC_VER <= MC_1_12_2 org.joml.Matrix4f
			#elif MC_VER < MC_1_19_4 com.mojang.math.Matrix4f 
			#elif MC_VER < MC_1_21_6 org.joml.Matrix4f
			#else org.joml.Matrix4fc 
			#endif 
			mcMatrix)
	{
		FloatBuffer buffer = FloatBuffer.allocate(16);
		storeMatrix(mcMatrix, buffer);
		Mat4f matrix = new Mat4f(buffer);
        #if MC_VER > MC_1_12_2 && MC_VER < MC_1_19_4
		matrix.transpose(); // In 1.19.3 and later, we no longer need to transpose it
        #endif
		return matrix;
	}
	/** Taken from Minecraft's com.mojang.math.Matrix4f class from 1.18.2 */
	private static void storeMatrix(
			#if MC_VER <= MC_1_12_2 org.joml.Matrix4f
			#elif MC_VER < MC_1_19_4 com.mojang.math.Matrix4f 
			#elif MC_VER < MC_1_21_6 org.joml.Matrix4f 
			#else org.joml.Matrix4fc 
			#endif
			matrix, 
			FloatBuffer buffer)
	{
		#if MC_VER <= MC_1_12_2
		matrix.get(buffer);
        #elif MC_VER < MC_1_19_4
		matrix.store(buffer);
        #else
		// Mojang starts to use joml's Matrix4f libary in 1.19.3 so we copy their store method and use it here if its newer than 1.19.3
		buffer.put(bufferIndex(0, 0), matrix.m00());
		buffer.put(bufferIndex(0, 1), matrix.m01());
		buffer.put(bufferIndex(0, 2), matrix.m02());
		buffer.put(bufferIndex(0, 3), matrix.m03());
		buffer.put(bufferIndex(1, 0), matrix.m10());
		buffer.put(bufferIndex(1, 1), matrix.m11());
		buffer.put(bufferIndex(1, 2), matrix.m12());
		buffer.put(bufferIndex(1, 3), matrix.m13());
		buffer.put(bufferIndex(2, 0), matrix.m20());
		buffer.put(bufferIndex(2, 1), matrix.m21());
		buffer.put(bufferIndex(2, 2), matrix.m22());
		buffer.put(bufferIndex(2, 3), matrix.m23());
		buffer.put(bufferIndex(3, 0), matrix.m30());
		buffer.put(bufferIndex(3, 1), matrix.m31());
		buffer.put(bufferIndex(3, 2), matrix.m32());
		buffer.put(bufferIndex(3, 3), matrix.m33());
        #endif
	}
	
	
	static final #if MC_VER <= MC_1_12_2 EnumFacing[] #else Direction[] #endif directions;
	static final EDhDirection[] lodDirections;
	static
	{
		EDhDirection[] lodDirs = EDhDirection.values();
		
		directions = new #if MC_VER <= MC_1_12_2 EnumFacing #else Direction #endif[lodDirs.length];
		
		lodDirections = new EDhDirection[lodDirs.length];
		for (EDhDirection lodDir : lodDirs)
		{
			#if MC_VER <= MC_1_12_2 EnumFacing #else Direction #endif dir;
			switch (lodDir.name().toUpperCase())
			{
				case "DOWN":
					dir = #if MC_VER <= MC_1_12_2 EnumFacing #else Direction #endif.DOWN;
					break;
				case "UP":
					dir = #if MC_VER <= MC_1_12_2 EnumFacing #else Direction #endif.UP;
					break;
				case "NORTH":
					dir = #if MC_VER <= MC_1_12_2 EnumFacing #else Direction #endif.NORTH;
					break;
				case "SOUTH":
					dir = #if MC_VER <= MC_1_12_2 EnumFacing #else Direction #endif.SOUTH;
					break;
				case "WEST":
					dir = #if MC_VER <= MC_1_12_2 EnumFacing #else Direction #endif.WEST;
					break;
				case "EAST":
					dir = #if MC_VER <= MC_1_12_2 EnumFacing #else Direction #endif.EAST;
					break;
				default:
					dir = null;
					break;
			}
			
			if (dir == null)
			{
				throw new IllegalArgumentException("Invalid direction on init mapping: " + lodDir);
			}
			directions[lodDir.ordinal()] = dir;
			lodDirections[dir.ordinal()] = lodDir;
		}
	}
	
	public static BlockPos Convert(DhBlockPos wrappedPos) { return new BlockPos(wrappedPos.getX(), wrappedPos.getY(), wrappedPos.getZ()); }
	
	public static ChunkPos Convert(DhChunkPos wrappedPos) { return new ChunkPos(wrappedPos.getX(), wrappedPos.getZ()); }
	public static DhChunkPos Convert(ChunkPos mcPos) 
	{ 
		#if MC_VER <= MC_1_21_11
		return new DhChunkPos(mcPos.x, mcPos.z);
		#else
		return new DhChunkPos(mcPos.x(), mcPos.z());
		#endif
	}
	
	public static #if MC_VER <= MC_1_12_2 EnumFacing #else Direction #endif Convert(EDhDirection lodDirection) { return directions[lodDirection.ordinal()]; }
	public static EDhDirection Convert(#if MC_VER <= MC_1_12_2 EnumFacing #else Direction #endif direction) { return lodDirections[direction.ordinal()]; }
	
}
