package com.yision.allay.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.foundation.render.BlockEntityModelElement;
import com.nobodiiiii.createbiotech.mixin.client.ModelPartAccessor;
import com.simibubi.create.content.logistics.FlapStuffs;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.yision.allay.block.allayport.AllayPortBlock;
import com.yision.allay.block.allayport.AllayPortBlockEntity;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class AllayPortRenderer extends SmartBlockEntityRenderer<AllayPortBlockEntity> {
	public static final int CURTAIN_SEGMENT_COUNT = 4;
	public static final List<ResourceLocation> CURTAIN_MODEL_LOCATIONS = List.of(
		CreateBiotech.asResource("block/allay_port/curtain_0"),
		CreateBiotech.asResource("block/allay_port/curtain_1"),
		CreateBiotech.asResource("block/allay_port/curtain_2"),
		CreateBiotech.asResource("block/allay_port/curtain_3")
	);
	public static final List<PartialModel> CURTAIN_SEGMENTS = CURTAIN_MODEL_LOCATIONS.stream()
		.map(PartialModel::of)
		.toList();
	public static final Vec3 CURTAIN_PIVOT = VecHelper.voxelSpace(0, 12, 3);
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
		float flapness = be.getFlap(partialTicks);
		float waveStrength = be.isCourierWaving() ? 1.0f : 0.0f;
		float animationTime = be.getLevel() == null
			? partialTicks
			: be.getLevel().getGameTime() % 24_000L + partialTicks;
		renderGreetingAllay(ms, buffer, light, facing, animationTime, waveStrength);

		if (VisualizationManager.supportsVisualization(be.getLevel())) {
			return;
		}

		renderCurtain(ms, buffer, blockState, facing, flapness, light, overlay);
	}

	private void renderCurtain(PoseStack ms, MultiBufferSource buffer, BlockState blockState, Direction facing,
		float flapness, int light, int overlay) {
		float horizontalAngle = AngleHelper.horizontalAngle(facing.getOpposite());
		VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutoutMipped());
		var transform = TransformStack.of(ms);

		ms.pushPose();
		transform.center()
			.rotateYDegrees(horizontalAngle)
			.uncenter();
		for (int segment = 0; segment < CURTAIN_SEGMENT_COUNT; segment++) {
			ms.pushPose();
			transform.translate(CURTAIN_PIVOT)
				.rotateXDegrees(FlapStuffs.flapAngle(flapness, segment))
				.translateBack(CURTAIN_PIVOT);
			SuperByteBuffer curtainBuffer = CachedBuffers.partial(CURTAIN_SEGMENTS.get(segment), blockState);
			curtainBuffer.light(light)
				.overlay(overlay)
				.renderInto(ms, vertexConsumer);
			ms.popPose();
		}
		ms.popPose();
	}

	private void renderGreetingAllay(PoseStack ms, MultiBufferSource buffer, int light, Direction facing,
		float animationTime, float waveStrength) {
		prepareGreetingPose(animationTime, waveStrength);
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
				renderLogisticsHat(poseStack, buf, packedLight);
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
