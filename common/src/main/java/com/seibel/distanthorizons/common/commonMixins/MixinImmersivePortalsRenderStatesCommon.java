package com.seibel.distanthorizons.common.commonMixins;

#if MC_VER > MC_1_12_2
import com.seibel.distanthorizons.common.wrappers.modAccessor.ImmersivePortalsAccessorCommon;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public class MixinImmersivePortalsRenderStatesCommon
{
	
	public static void saveVolatileOriginals()
	{
		Minecraft mc = Minecraft.getInstance();
		ImmersivePortalsAccessorCommon.originalLevel = mc.level;
		
		if (mc.player == null) {
			ImmersivePortalsAccessorCommon.originalBlockPos = null;
			ImmersivePortalsAccessorCommon.originalChunkPos = null;
			return;
		}
		BlockPos pos = mc.player.blockPosition();
		ImmersivePortalsAccessorCommon.originalBlockPos = new DhBlockPos(pos.getX(), pos.getY(), pos.getZ());
		#if MC_VER < MC_1_17_1
        ChunkPos cPos = new ChunkPos(mc.player.blockPosition());
        #else
		ChunkPos cPos = mc.player.chunkPosition();
        #endif
		
		#if MC_VER <= MC_1_21_11
		ImmersivePortalsAccessorCommon.originalChunkPos = new DhChunkPos(cPos.x, cPos.z);
		#else
		ImmersivePortalsAccessorCommon.originalChunkPos = new DhChunkPos(cPos.x(), cPos.z());
		#endif
	}
	
}
#endif