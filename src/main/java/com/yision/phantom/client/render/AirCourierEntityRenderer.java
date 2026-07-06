package com.yision.phantom.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.yision.phantom.CreatePhantom;
import com.yision.phantom.entity.courier.AirCourierEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class AirCourierEntityRenderer extends EntityRenderer<AirCourierEntity> {
	private static final ResourceLocation PHANTOM_TEXTURE = new ResourceLocation("textures/entity/phantom.png");
	private static final ResourceLocation PHANTOM_EYES_TEXTURE =
		new ResourceLocation("textures/entity/phantom_eyes.png");
	private static final ResourceLocation CARGO_MODEL =
		CreatePhantom.asResource("item/mini_phantom_package");
	private static final int EYES_LIGHT = 15728640;
	private static final float CRUISE_SCALE = 0.58f;
	private static final float ACTIVE_SCALE = 0.66f;
	private static final float ACTIVE_MODEL_Y_OFFSET = 0.24f;
	private static final float WAITING_MODEL_Y_OFFSET = 0.66f;
	private static final float WAITING_SURFACE_LIFT = 0.5f;
	private static final float MODEL_Z_OFFSET = 0.1875f;
	private static final float PHANTOM_MODEL_Y_TRANSLATE = 1.3125f;
	private static final float LIVING_MODEL_Y_TRANSLATE = -1.501f;
	private static final float STATIC_WING_Z_ROTATION = 0.0f;
	private static final float STATIC_TAIL_X_ROTATION = -5.0f * ((float) Math.PI / 180.0f);

	private final PhantomModel<RenderPhantom> phantomModel;
	@Nullable
	private ClientLevel cachedLevel;
	@Nullable
	private RenderPhantom cachedPhantom;

	public AirCourierEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.phantomModel = new PhantomModel<>(context.bakeLayer(ModelLayers.PHANTOM));
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
		poseStack.translate(0.0f, PHANTOM_MODEL_Y_TRANSLATE + yOffset, MODEL_Z_OFFSET);
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
