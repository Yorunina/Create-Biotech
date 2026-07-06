package com.yision.phantom.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nobodiiiii.createbiotech.mixin.client.ModelPartAccessor;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.yision.phantom.CreatePhantom;
import com.yision.phantom.entity.courier.AirCourierEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.createmod.catnip.render.CachedBuffers;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class AirCourierEntityRenderer extends EntityRenderer<AirCourierEntity> {
	private static final ResourceLocation PHANTOM_TEXTURE = new ResourceLocation("textures/entity/phantom.png");
	private static final ResourceLocation PHANTOM_EYES_TEXTURE =
		new ResourceLocation("textures/entity/phantom_eyes.png");
	private static final ResourceLocation CARGO_MODEL =
		CreatePhantom.asResource("item/mini_phantom_package");
	private static final PartialModel LOGISTICS_HAT =
		PartialModel.of(new ResourceLocation("create", "entity/logistics_hat"));
	private static final int EYES_LIGHT = 15728640;
	private static final float CRUISE_SCALE = 0.58f;
	private static final float ACTIVE_SCALE = 0.66f;
	private static final float ACTIVE_MODEL_Y_OFFSET = 0.24f;
	private static final float WAITING_MODEL_Y_OFFSET = 0.66f;
	private static final float WAITING_SURFACE_LIFT = 7.0f / 16.0f;
	private static final float MODEL_Z_OFFSET = 0.1875f;
	private static final float PHANTOM_MODEL_Y_TRANSLATE = 1.3125f;
	private static final float LIVING_MODEL_Y_TRANSLATE = -1.501f;
	private static final float STATIC_WING_Z_ROTATION = 0.0f;
	private static final float STATIC_TAIL_X_ROTATION = -5.0f * ((float) Math.PI / 180.0f);
	private static final float LOGISTICS_HAT_OFFSET_X = 0.0f;
	private static final float LOGISTICS_HAT_OFFSET_Y = 0.0f;
	private static final float LOGISTICS_HAT_OFFSET_Z = -1.0f;
	private static final float LOGISTICS_HAT_MODEL_Y_OFFSET = -2.25f;
	private static final float LOGISTICS_HAT_X_ROTATION_DEGREES = -8.5f;

	private final PhantomModel<RenderPhantom> phantomModel;
	private final float phantomModelCenterX;
	@Nullable
	private ClientLevel cachedLevel;
	@Nullable
	private RenderPhantom cachedPhantom;

	public AirCourierEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.phantomModel = new PhantomModel<>(context.bakeLayer(ModelLayers.PHANTOM));
		this.phantomModelCenterX = measureModelCenterX(phantomModel.root());
		this.shadowRadius = 0.45f;
	}

	@Override
	public void render(AirCourierEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
		MultiBufferSource buffer, int packedLight) {
		if (entity.tickCount < 1) {
			super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
			return;
		}

		RenderPhantom phantom = getOrCreateRenderPhantom(entity.level());
		if (phantom == null) {
			super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
			return;
		}

		preparePhantomModel(entity, phantom, partialTick);

		poseStack.pushPose();
		applyCourierPose(entity, partialTick, poseStack);
		applySupportLift(entity, poseStack);
		applyModelTransform(entity, poseStack);
		VertexConsumer bodyBuffer = buffer.getBuffer(phantomModel.renderType(PHANTOM_TEXTURE));
		phantomModel.renderToBuffer(poseStack, bodyBuffer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
		VertexConsumer eyesBuffer = buffer.getBuffer(RenderType.eyes(PHANTOM_EYES_TEXTURE));
		phantomModel.renderToBuffer(poseStack, eyesBuffer, EYES_LIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
		renderLogisticsHatOnHead(poseStack, buffer, packedLight);
		renderCargo(entity, poseStack, buffer, packedLight);
		poseStack.popPose();

		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	private void applyCourierPose(AirCourierEntity entity, float partialTick, PoseStack poseStack) {
		poseStack.mulPose(Axis.YP.rotationDegrees(entity.getVisualYaw(partialTick) + 180.0f));
		poseStack.mulPose(Axis.XP.rotationDegrees(entity.getVisualPitch(partialTick)));
		poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getVisualRoll(partialTick)));
	}

	private void applyModelTransform(AirCourierEntity entity, PoseStack poseStack) {
		poseStack.scale(-1.0f, -1.0f, 1.0f);
		float scale = entity.getPhase() == AirCourierEntity.Phase.CRUISE ? CRUISE_SCALE : ACTIVE_SCALE;
		float yOffset = entity.getPhase() == AirCourierEntity.Phase.WAITING
			? WAITING_MODEL_Y_OFFSET
			: ACTIVE_MODEL_Y_OFFSET;
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-phantomModelCenterX, PHANTOM_MODEL_Y_TRANSLATE + yOffset, MODEL_Z_OFFSET);
		poseStack.translate(0.0f, LIVING_MODEL_Y_TRANSLATE, 0.0f);
	}

	private static void applySupportLift(AirCourierEntity entity, PoseStack poseStack) {
		if (entity.getPhase() == AirCourierEntity.Phase.WAITING && entity.shouldRenderOnSupport()) {
			// Waiting couriers rendered as world entities sit on a support surface instead of around a flight center.
			poseStack.translate(0.0f, WAITING_SURFACE_LIFT, 0.0f);
		}
	}

	private void preparePhantomModel(AirCourierEntity courier, RenderPhantom phantom, float partialTick) {
		phantomModel.root()
			.getAllParts()
			.forEach(ModelPart::resetPose);
		if (courier.getPhase() == AirCourierEntity.Phase.WAITING) {
			applyStaticWaitingPose();
			return;
		}
		phantom.copyAnimationStateFrom(courier);
		phantomModel.setupAnim(phantom, 0.0f, 0.0f, courier.tickCount + partialTick, 0.0f, 0.0f);
	}

	private void applyStaticWaitingPose() {
		ModelPart body = phantomModel.root().getChild("body");
		ModelPart leftWingBase = body.getChild("left_wing_base");
		ModelPart leftWingTip = leftWingBase.getChild("left_wing_tip");
		ModelPart rightWingBase = body.getChild("right_wing_base");
		ModelPart rightWingTip = rightWingBase.getChild("right_wing_tip");
		ModelPart tailBase = body.getChild("tail_base");
		ModelPart tailTip = tailBase.getChild("tail_tip");

		leftWingBase.zRot = STATIC_WING_Z_ROTATION;
		leftWingTip.zRot = STATIC_WING_Z_ROTATION;
		rightWingBase.zRot = -STATIC_WING_Z_ROTATION;
		rightWingTip.zRot = -STATIC_WING_Z_ROTATION;
		tailBase.xRot = STATIC_TAIL_X_ROTATION;
		tailTip.xRot = STATIC_TAIL_X_ROTATION;
	}

	private void renderLogisticsHatOnHead(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		ModelPart body = phantomModel.root().getChild("body");
		ModelPart head = body.getChild("head");
		if (head.isEmpty()) {
			return;
		}

		poseStack.pushPose();
		body.translateAndRotate(poseStack);
		head.translateAndRotate(poseStack);

		ModelPart.Cube headCube = ((ModelPartAccessor) (Object) head).createBiotech$getCubes()
			.get(0);
		poseStack.translate(LOGISTICS_HAT_OFFSET_X / 16.0f,
			(headCube.minY - headCube.maxY + LOGISTICS_HAT_OFFSET_Y) / 16.0f,
			LOGISTICS_HAT_OFFSET_Z / 16.0f);
		float hatScale = Math.max(headCube.maxX - headCube.minX, headCube.maxZ - headCube.minZ) / 8.0f;
		poseStack.scale(hatScale, hatScale, hatScale);

		poseStack.scale(1.0f, -1.0f, -1.0f);
		poseStack.translate(0.0f, LOGISTICS_HAT_MODEL_Y_OFFSET / 16.0f, 0.0f);
		poseStack.mulPose(Axis.XP.rotationDegrees(LOGISTICS_HAT_X_ROTATION_DEGREES));
		CachedBuffers.partial(LOGISTICS_HAT, Blocks.AIR.defaultBlockState())
			.disableDiffuse()
			.light(packedLight)
			.renderInto(poseStack, buffer.getBuffer(Sheets.cutoutBlockSheet()));
		poseStack.popPose();
	}

	private void renderCargo(AirCourierEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		if (entity.getPackage().isEmpty()) {
			return;
		}

		BakedModel cargoModel = Minecraft.getInstance()
			.getModelManager()
			.getModel(CARGO_MODEL);
		if (cargoModel == Minecraft.getInstance().getModelManager().getMissingModel()) {
			return;
		}

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
		poseStack.translate(0.5f, 0.5f, 0.5f);
		PartialItemModelRenderer.of(entity.getPackage(), ItemDisplayContext.NONE, poseStack, buffer,
			OverlayTexture.NO_OVERLAY)
			.render(cargoModel, packedLight);
		poseStack.popPose();
	}

	private @Nullable RenderPhantom getOrCreateRenderPhantom(Level level) {
		if (!(level instanceof ClientLevel clientLevel)) {
			return null;
		}
		if (cachedPhantom == null || cachedLevel != clientLevel) {
			cachedLevel = clientLevel;
			cachedPhantom = new RenderPhantom(clientLevel);
		}
		return cachedPhantom;
	}

	@Override
	public ResourceLocation getTextureLocation(AirCourierEntity entity) {
		return PHANTOM_TEXTURE;
	}

	private static float measureModelCenterX(ModelPart root) {
		ModelBounds bounds = new ModelBounds();
		root.visit(new PoseStack(), (pose, path, index, cube) -> bounds.include(pose.pose(), cube));
		return bounds.centerX();
	}

	private static class ModelBounds {
		private float minX = Float.POSITIVE_INFINITY;
		private float maxX = Float.NEGATIVE_INFINITY;

		private void include(Matrix4f matrix, ModelPart.Cube cube) {
			includeCorner(matrix, cube.minX, cube.minY, cube.minZ);
			includeCorner(matrix, cube.minX, cube.minY, cube.maxZ);
			includeCorner(matrix, cube.minX, cube.maxY, cube.minZ);
			includeCorner(matrix, cube.minX, cube.maxY, cube.maxZ);
			includeCorner(matrix, cube.maxX, cube.minY, cube.minZ);
			includeCorner(matrix, cube.maxX, cube.minY, cube.maxZ);
			includeCorner(matrix, cube.maxX, cube.maxY, cube.minZ);
			includeCorner(matrix, cube.maxX, cube.maxY, cube.maxZ);
		}

		private void includeCorner(Matrix4f matrix, float x, float y, float z) {
			Vector3f transformed = matrix.transformPosition(x / 16.0f, y / 16.0f, z / 16.0f, new Vector3f());
			minX = Math.min(minX, transformed.x());
			maxX = Math.max(maxX, transformed.x());
		}

		private float centerX() {
			return minX == Float.POSITIVE_INFINITY ? 0.0f : (minX + maxX) * 0.5f;
		}
	}

	private static class RenderPhantom extends Phantom {
		private int flapTickOffset;

		private RenderPhantom(ClientLevel level) {
			super(EntityType.PHANTOM, level);
			setNoAi(true);
			setSilent(true);
			setPhantomSize(0);
		}

		private void copyAnimationStateFrom(AirCourierEntity courier) {
			flapTickOffset = courier.getId() * 3;
			tickCount = courier.tickCount;
		}

		@Override
		public int getUniqueFlapTickOffset() {
			return flapTickOffset;
		}
	}
}
