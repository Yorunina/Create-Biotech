package com.yision.allay.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nobodiiiii.createbiotech.CreateBiotech;
import com.simibubi.create.content.logistics.FlapStuffs;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.yision.allay.block.allayport.AllayPortBlock;
import com.yision.allay.block.allayport.AllayPortBlockEntity;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class AllayPortRenderer extends SmartBlockEntityRenderer<AllayPortBlockEntity> {
	public static final ResourceLocation FLAP_MODEL_LOCATION = CreateBiotech.asResource("block/allay_port/flap");
	public static final PartialModel FLAP = PartialModel.of(FLAP_MODEL_LOCATION);
	public static final Vec3 FLAP_PIVOT = VecHelper.voxelSpace(0, 1, 1.5f);

	public AllayPortRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(AllayPortBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			return;
		}

		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(AllayPortBlock.FACING);
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());
		SuperByteBuffer flapBuffer = CachedBuffers.partial(FLAP, blockState);
		FlapStuffs.renderFlaps(ms, vb, flapBuffer, FLAP_PIVOT, facing, be.getFlap(partialTicks), 0, light);
	}
}
