package com.yision.phantom.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.yision.phantom.entity.courier.AirCourierEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public class AirCourierEntityRenderer extends EntityRenderer<AirCourierEntity> {
	private static final ResourceLocation PHANTOM_TEXTURE = new ResourceLocation("textures/entity/phantom.png");
	private static final ResourceLocation PHANTOM_EYES_TEXTURE =
		new ResourceLocation("textures/entity/phantom_eyes.png");
	private static final int EYES_LIGHT = 15728640;
	private static final float CRUISE_SCALE = 0.58f;
	private static final float ACTIVE_SCALE = 0.66f;
	private static final float ACTIVE_MODEL_Y_OFFSET = 0.24f;
	private static final float WAITING_MODEL_Y_OFFSET = 0.66f;
	private static final float MODEL_Z_OFFSET = 0.1875f;
	private static final float PHANTOM_MODEL_Y_TRANSLATE = 1.3125f;
	private static final float LIVING_MODEL_Y_TRANSLATE = -1.501f;
	private static final float MOTION_EPSILON_SQR = 1.0E-6f;
	private static final double UP_PROJECTION_EPSILON_SQR = 1.0E-6;
	private static final Vec3 WORLD_UP = new Vec3(0.0, 1.0, 0.0);
	private static final Vec3 WORLD_NORTH = new Vec3(0.0, 0.0, -1.0);
	private static final Vec3 WORLD_EAST = new Vec3(1.0, 0.0, 0.0);

	private final PhantomModel<RenderPhantom> phantomModel;
	private final Map<Integer, Vec3> cachedUpVectors = new HashMap<>();
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
		poseStack.mulPose(resolveRenderOrientation(entity, partialTick));
		if (entity.getPhase() == AirCourierEntity.Phase.WAITING) {
			poseStack.mulPose(Axis.XP.rotationDegrees(18.0f));
		}
		poseStack.scale(-1.0f, -1.0f, 1.0f);

		float scale = entity.getPhase() == AirCourierEntity.Phase.CRUISE ? CRUISE_SCALE : ACTIVE_SCALE;
		float yOffset = entity.getPhase() == AirCourierEntity.Phase.WAITING
			? WAITING_MODEL_Y_OFFSET
			: ACTIVE_MODEL_Y_OFFSET;
		poseStack.scale(scale, scale, scale);
		poseStack.translate(0.0f, PHANTOM_MODEL_Y_TRANSLATE + yOffset, MODEL_Z_OFFSET);
		poseStack.translate(0.0f, LIVING_MODEL_Y_TRANSLATE, 0.0f);

		VertexConsumer bodyBuffer = buffer.getBuffer(phantomModel.renderType(PHANTOM_TEXTURE));
		phantomModel.renderToBuffer(poseStack, bodyBuffer, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
		VertexConsumer eyesBuffer = buffer.getBuffer(RenderType.eyes(PHANTOM_EYES_TEXTURE));
		phantomModel.renderToBuffer(poseStack, eyesBuffer, EYES_LIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
		poseStack.popPose();

		super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
	}

	private Quaternionf resolveRenderOrientation(AirCourierEntity courier, float partialTick) {
		Vec3 direction = resolveRenderDirection(courier, partialTick);
		Vec3 back = direction.scale(-1.0);
		Vec3 up = resolveContinuousUp(courier, direction);
		Vec3 right = up.cross(back)
			.normalize();
		Vec3 correctedUp = back.cross(right)
			.normalize();
		cachedUpVectors.put(courier.getId(), correctedUp);

		Matrix3f basis = new Matrix3f(
			(float) right.x, (float) right.y, (float) right.z,
			(float) correctedUp.x, (float) correctedUp.y, (float) correctedUp.z,
			(float) back.x, (float) back.y, (float) back.z
		);
		return new Quaternionf().setFromUnnormalized(basis);
	}

	private Vec3 resolveContinuousUp(AirCourierEntity courier, Vec3 direction) {
		Vec3 previousUp = cachedUpVectors.get(courier.getId());
		Vec3 projectedPreviousUp = projectOntoDirectionPlane(previousUp, direction);
		if (projectedPreviousUp != null) {
			return projectedPreviousUp.normalize();
		}

		Vec3 fallbackUp = projectOntoDirectionPlane(WORLD_UP, direction);
		if (fallbackUp != null) {
			return fallbackUp.normalize();
		}
		fallbackUp = projectOntoDirectionPlane(WORLD_NORTH, direction);
		if (fallbackUp != null) {
			return fallbackUp.normalize();
		}
		fallbackUp = projectOntoDirectionPlane(WORLD_EAST, direction);
		if (fallbackUp != null) {
			return fallbackUp.normalize();
		}

		return WORLD_UP;
	}

	private @Nullable Vec3 projectOntoDirectionPlane(@Nullable Vec3 vector, Vec3 direction) {
		if (vector == null || vector.lengthSqr() <= UP_PROJECTION_EPSILON_SQR) {
			return null;
		}
		Vec3 projected = vector.subtract(direction.scale(vector.dot(direction)));
		if (projected.lengthSqr() <= UP_PROJECTION_EPSILON_SQR) {
			return null;
		}
		return projected;
	}

	private Vec3 resolveRenderDirection(AirCourierEntity courier, float partialTick) {
		Vec3 motion = courier.getDeltaMovement();
		if (motion.lengthSqr() > MOTION_EPSILON_SQR) {
			return motion.normalize();
		}

		float yaw = Mth.rotLerp(partialTick, courier.yRotO, courier.getYRot()) * Mth.DEG_TO_RAD;
		float pitch = Mth.lerp(partialTick, courier.xRotO, courier.getXRot()) * Mth.DEG_TO_RAD;
		float horizontal = Mth.cos(pitch);
		return new Vec3(Mth.sin(yaw) * horizontal, Mth.sin(pitch), Mth.cos(yaw) * horizontal);
	}

	private void preparePhantomModel(AirCourierEntity courier, RenderPhantom phantom, float partialTick) {
		phantom.copyAnimationStateFrom(courier);
		phantomModel.root()
			.getAllParts()
			.forEach(ModelPart::resetPose);
		phantomModel.setupAnim(phantom, 0.0f, 0.0f, courier.tickCount + partialTick, 0.0f, 0.0f);
	}

	private @Nullable RenderPhantom getOrCreateRenderPhantom(Level level) {
		if (!(level instanceof ClientLevel clientLevel)) {
			return null;
		}
		if (cachedPhantom == null || cachedLevel != clientLevel) {
			cachedLevel = clientLevel;
			cachedUpVectors.clear();
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
