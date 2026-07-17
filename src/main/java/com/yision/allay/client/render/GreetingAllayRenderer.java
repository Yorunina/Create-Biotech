package com.yision.allay.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.foundation.render.BlockEntityModelElement;
import com.nobodiiiii.createbiotech.mixin.client.ModelPartAccessor;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

final class GreetingAllayRenderer {

	private static final ResourceLocation ALLAY_TEXTURE =
		CreateBiotech.asResource("textures/entity/allay_port/allay.png");
	private static final PartialModel LOGISTICS_HAT =
		PartialModel.of(new ResourceLocation("create", "entity/logistics_hat"));
	private static final float LOGISTICS_HAT_OFFSET_X = 0.0f;
	private static final float LOGISTICS_HAT_OFFSET_Y = 0.0f;
	private static final float LOGISTICS_HAT_OFFSET_Z = -0.5f;
	private static final float LOGISTICS_HAT_MODEL_Y_OFFSET = -2.25f;
	private static final double ALLAY_POSITION_Y = 1.0d - 2.0d / 16.0d;
	private static final float LIVING_ENTITY_MODEL_Y_OFFSET = -1.501f;
	private static final float ALLAY_SCALE = 1.0f;

	private final AllayModel allayModel;

	GreetingAllayRenderer(ModelPart bakedLayerRoot) {
		allayModel = new AllayModel(bakedLayerRoot);
	}

	void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Direction facing,
		float animationTime, float waveStrength) {
		prepareGreetingPose(animationTime, waveStrength);
		int allayLight = LightTexture.pack(15, LightTexture.sky(packedLight));

		BlockEntityModelElement.builder()
			.atLocal(0.5d, ALLAY_POSITION_Y, 0.5d)
			.rotateY(180.0f - facing.toYRot())
			.scale(-ALLAY_SCALE, -ALLAY_SCALE, ALLAY_SCALE)
			.packedLight(allayLight)
			.render(poseStack, buffer, (modelPose, modelBuffer, modelLight) -> {
				modelPose.translate(0.0f, LIVING_ENTITY_MODEL_Y_OFFSET, 0.0f);
				allayModel.renderToBuffer(
					modelPose,
					modelBuffer.getBuffer(allayModel.renderType(ALLAY_TEXTURE)),
					modelLight,
					OverlayTexture.NO_OVERLAY,
					1.0f,
					1.0f,
					1.0f,
					1.0f);
				renderLogisticsHat(modelPose, modelBuffer, modelLight);
			});
	}

	private void renderLogisticsHat(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		ModelPart root = allayModel.root();
		ModelPart head = root.getChild("head");
		if (head.isEmpty()) {
			return;
		}

		poseStack.pushPose();
		root.translateAndRotate(poseStack);
		head.translateAndRotate(poseStack);
		ModelPart.Cube headCube = ((ModelPartAccessor) (Object) head).createBiotech$getCubes().get(0);
		poseStack.translate(LOGISTICS_HAT_OFFSET_X / 16.0f,
			(headCube.minY - headCube.maxY + LOGISTICS_HAT_OFFSET_Y) / 16.0f,
			LOGISTICS_HAT_OFFSET_Z / 16.0f);
		float hatScale = Math.max(headCube.maxX - headCube.minX, headCube.maxZ - headCube.minZ) / 8.0f;
		poseStack.scale(hatScale, hatScale, hatScale);
		poseStack.scale(1.0f, -1.0f, -1.0f);
		poseStack.translate(0.0f, LOGISTICS_HAT_MODEL_Y_OFFSET / 16.0f, 0.0f);
		CachedBuffers.partial(LOGISTICS_HAT, Blocks.AIR.defaultBlockState())
			.disableDiffuse()
			.light(packedLight)
			.renderInto(poseStack, buffer.getBuffer(Sheets.cutoutBlockSheet()));
		poseStack.popPose();
	}

	private void prepareGreetingPose(float animationTime, float waveStrength) {
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

		float wave = Mth.sin(animationTime * 0.65f) * waveStrength;
		rightArm.xRot = degrees(-20.0f + wave * 8.0f);
		rightArm.yRot = degrees(-15.0f - wave * 6.0f);
		rightArm.zRot = degrees(130.0f + wave * 24.0f);
		head.zRot += degrees(-wave * 3.0f);
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
