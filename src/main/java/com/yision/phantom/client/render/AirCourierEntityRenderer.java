package com.yision.phantom.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class AirCourierEntityRenderer extends EntityRenderer<AirCourierEntity> {
	private static final ResourceLocation PHANTOM_TEXTURE = new ResourceLocation("textures/entity/phantom.png");
	private static final ResourceLocation PHANTOM_EYES_TEXTURE =
		new ResourceLocation("textures/entity/phantom_eyes.png");
	private static final ModelResourceLocation PACKAGE_MODEL = new ModelResourceLocation(
		CreatePhantom.asResource("mini_phantom_attached_package"), "inventory");
	private static final int EYES_LIGHT = 15728640;
	private static final float CRUISE_SCALE = 0.58f;
	private static final float ACTIVE_SCALE = 0.66f;
	private static final float ACTIVE_MODEL_Y_OFFSET = 0.24f;
	private static final float WAITING_MODEL_Y_OFFSET = 0.66f;
	private static final float MODEL_Z_OFFSET = 0.1875f;
	private static final float PHANTOM_MODEL_Y_TRANSLATE = 1.3125f;
	private static final float LIVING_MODEL_Y_TRANSLATE = -1.501f;
	private static final float PACKAGE_X_OFFSET = -0.03125f;
	private static final float PACKAGE_Y_OFFSET = -0.375f;
	private static final float PACKAGE_Z_OFFSET = -0.265625f;

	private final PhantomModel<RenderPhantom> phantomModel;
	private final ModelPart phantomBody;
	private final ItemRenderer itemRenderer;
	@Nullable
	private ClientLevel cachedLevel;
	@Nullable
	private RenderPhantom cachedPhantom;

	public AirCourierEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.phantomModel = new PhantomModel<>(context.bakeLayer(ModelLayers.PHANTOM));
		this.phantomBody = phantomModel.root()
			.getChild("body");
		this.itemRenderer = context.getItemRenderer();
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
		applyModelTransform(entity, poseStack);
		VertexConsumer bodyBuffer = buffer.getBuffer(phantomModel.renderType(PHANTOM_TEXTURE));
		phantomModel.renderToBuffer(poseStack, bodyBuffer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
		renderPackage(entity, poseStack, buffer, packedLight);
		VertexConsumer eyesBuffer = buffer.getBuffer(RenderType.eyes(PHANTOM_EYES_TEXTURE));
		phantomModel.renderToBuffer(poseStack, eyesBuffer, EYES_LIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
		poseStack.popPose();

		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	private void applyCourierPose(AirCourierEntity entity, float partialTick, PoseStack poseStack) {
		poseStack.mulPose(Axis.YP.rotationDegrees(entity.getVisualYaw(partialTick) + 180.0f));
		poseStack.mulPose(Axis.XP.rotationDegrees(entity.getVisualPitch(partialTick)));
		if (entity.getPhase() == AirCourierEntity.Phase.WAITING) {
			poseStack.mulPose(Axis.XP.rotationDegrees(18.0f));
		}
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

	private void preparePhantomModel(AirCourierEntity courier, RenderPhantom phantom, float partialTick) {
		phantom.copyAnimationStateFrom(courier);
		phantomModel.root()
			.getAllParts()
			.forEach(ModelPart::resetPose);
		phantomModel.setupAnim(phantom, 0.0f, 0.0f, courier.tickCount + partialTick, 0.0f, 0.0f);
	}

	private void renderPackage(AirCourierEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		if (entity.getPackage()
			.isEmpty()) {
			return;
		}

		BakedModel packageModel = Minecraft.getInstance()
			.getModelManager()
			.getModel(PACKAGE_MODEL);

		poseStack.pushPose();
		phantomBody.translateAndRotate(poseStack);
		poseStack.translate(PACKAGE_X_OFFSET, PACKAGE_Y_OFFSET, PACKAGE_Z_OFFSET);
		itemRenderer.render(ItemStack.EMPTY, ItemDisplayContext.NONE, false, poseStack, buffer, packedLight,
			OverlayTexture.NO_OVERLAY, packageModel);
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
