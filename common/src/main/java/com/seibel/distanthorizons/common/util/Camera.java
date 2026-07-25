package com.seibel.distanthorizons.common.util;

#if MC_VER <= MC_1_7_10
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.seibel.distanthorizons.RenderHelper;
#endif

/**
 * Recreation of MC's camera object
 * for use with MC 1.7.10.
 */
public class Camera
{
	#if MC_VER <= MC_1_7_10
	
	public static final Camera INSTANCE = new Camera();
	
    private final Vector3d pos = new Vector3d();
    //final BlockPos blockPos = new BlockPos();
    //float pitch;
    //float yaw;
    //EntityLivingBase entity;
    //boolean thirdPerson;
    //final float partialTicks;
	
	
	
	//========//
	// update //
	//========//
	//region
	
	public void update(EntityLivingBase entity, float partialTicks)
	{
		final Vector4f offset = new Vector4f(); // third person offset
		final Matrix4f inverseModelView = RenderHelper.getModelViewMatrixMC()
			.invert();
		inverseModelView.transform(offset);
		
		final double camX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks + offset.x;
		final double camY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks + offset.y;
		final double camZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks + offset.z;
		this.pos.set(camX, camY, camZ);
		
		//this.partialTicks = partialTicks;
		//this.entity = entity;
		//this.blockPos.set(
		//	MathHelper.floor_double(camX),
		//	MathHelper.floor_double(camY),
		//	MathHelper.floor_double(camZ));
		//this.pitch = entity.cameraPitch;
		//this.yaw = entity.rotationYaw;
		//this.thirdPerson = (Minecraft.getMinecraft().gameSettings.thirdPersonView == 1);
	}
	
	//endregion
	
	
	
	//=========//
	// getters //
	//=========//
	//region
	
	public Vector3d getPos() { return this.pos; }
	
	//endregion
	
	
	
	#endif
}
