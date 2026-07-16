package com.yision.allay.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.foundation.render.BlockEntityModelElement;
import com.simibubi.create.content.logistics.FlapStuffs;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.yision.allay.block.allayport.AllayPortBlock;
import com.yision.allay.block.allayport.AllayPortBlockEntity;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class AllayPortRenderer extends SmartBlockEntityRenderer<AllayPortBlockEntity> {
	public static final ResourceLocation FLAP_MODEL_LOCATION = CreateBiotech.asResource("block/allay_port/flap");
	public static final PartialModel FLAP = PartialModel.of(FLAP_MODEL_LOCATION);
	public static final Vec3 FLAP_PIVOT = VecHelper.voxelSpace(0, 1, 1.5f);
	private static final ResourceLocation ALLAY_TEXTURE =
		CreateBiotech.asResource("textures/entity/allay_port/allay.png");
	private static final double ALLAY_POSITION_Y = 1.0d - 2.0d / 16.0d;
	private static final float LIVING_ENTITY_MODEL_Y_OFFSET = -1.501f;
	private static final float ALLAY_SCALE = 1.0f;

	private final AllayModel allayModel;

	public AllayPortRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		allayModel = new AllayModel(context.bakeLayer(ModelLayers.ALLAY));
	}

	@Override
	protected void renderSafe(AllayPortBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(AllayPortBlock.FACING);
		renderGreetingAllay(ms, buffer, light, facing);

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			return;
		}

		VertexConsumer vb = buffer.getBuffer(RenderType.solid());
		SuperByteBuffer flapBuffer = CachedBuffers.partial(FLAP, blockState);
		FlapStuffs.renderFlaps(ms, vb, flapBuffer, FLAP_PIVOT, facing, be.getFlap(partialTicks), 0, light);
	}

	private void renderGreetingAllay(PoseStack ms, MultiBufferSource buffer, int light, Direction facing) {
		prepareGreetingPose();
		int allayLight = LightTexture.pack(15, LightTexture.sky(light));

		BlockEntityModelElement.builder()
			.atLocal(0.5d, ALLAY_POSITION_Y, 0.5d)
			.rotateY(180.0f - facing.toYRot())
			.scale(-ALLAY_SCALE, -ALLAY_SCALE, ALLAY_SCALE)
			.packedLight(allayLight)
			.render(ms, buffer, (poseStack, buf, packedLight) -> {
				poseStack.translate(0.0f, LIVING_ENTITY_MODEL_Y_OFFSET, 0.0f);
				allayModel.renderToBuffer(
					poseStack,
					buf.getBuffer(allayModel.renderType(ALLAY_TEXTURE)),
					packedLight,
					OverlayTexture.NO_OVERLAY,
					1.0f,
					1.0f,
					1.0f,
					1.0f);
			});
	}

	private void prepareGreetingPose() {
		ModelPart root = allayModel.root();
		root.getAllParts().forEach(ModelPart::resetPose);

		ModelPart head = root.getChild("head");
		ModelPart body = root.getChild("body");
		ModelPart rightArm = body.getChild("right_arm");
		ModelPart leftArm = body.getChild("left_arm");
		ModelPart rightWing = body.getChild("right_wing");
		ModelPart leftWing = body.getChild("left_wing");

		root.zRot = degrees(18.0f);
		body.zRot = degrees(5.0f);
		head.zRot = degrees(-6.0f);
		head.yRot = degrees(-5.0f);

		rightArm.xRot = degrees(-20.0f);
		rightArm.yRot = degrees(-15.0f);
		rightArm.zRot = degrees(130.0f);
		leftArm.xRot = degrees(-5.0f);
		leftArm.zRot = degrees(-25.0f);

		rightWing.xRot = degrees(25.0f);
		rightWing.yRot = degrees(-45.0f);
		leftWing.xRot = degrees(25.0f);
		leftWing.yRot = degrees(45.0f);
	}

	private static float degrees(float degrees) {
		return (float) Math.toRadians(degrees);
	}
}
