package com.yision.allay.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nobodiiiii.createbiotech.mixin.client.ModelPartAccessor;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.yision.allay.CreateAllay;
import com.yision.allay.entity.courier.AllayCourierEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Blocks;

/**
 * Vanilla Allay renderer with the courier hat and compact package as additional layers. The body,
 * wings, arms and holding pose remain entirely controlled by vanilla AllayRenderer/AllayModel.
 */
public class AllayCourierEntityRenderer extends AllayRenderer {

	private static final ResourceLocation CARGO_MODEL = CreateAllay.asResource("item/mini_allay_package");
	private static final PartialModel LOGISTICS_HAT =
		PartialModel.of(new ResourceLocation("create", "entity/logistics_hat"));

	public AllayCourierEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		layers.removeIf(layer -> layer instanceof ItemInHandLayer<?, ?>);
		addLayer(new CourierAccessoriesLayer(this));
	}

	private static class CourierAccessoriesLayer extends RenderLayer<Allay, AllayModel> {

		private static final float LOGISTICS_HAT_OFFSET_X = 0.0f;
		private static final float LOGISTICS_HAT_OFFSET_Y = 0.0f;
		private static final float LOGISTICS_HAT_OFFSET_Z = -0.5f;
		private static final float LOGISTICS_HAT_MODEL_Y_OFFSET = -2.25f;

		private CourierAccessoriesLayer(RenderLayerParent<Allay, AllayModel> renderer) {
			super(renderer);
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Allay allay,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
			float netHeadYaw, float headPitch) {
			if (!(allay instanceof AllayCourierEntity courier)) {
				return;
			}
			if (courier.shouldRenderLogisticsHat()) {
				renderLogisticsHat(poseStack, buffer, packedLight);
			}
			renderCargo(courier, poseStack, buffer, packedLight);
		}

		private void renderLogisticsHat(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
			ModelPart root = getParentModel().root();
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

		private void renderCargo(AllayCourierEntity courier, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight) {
			if (courier.getPackage().isEmpty()) {
				return;
			}
			BakedModel cargoModel = Minecraft.getInstance().getModelManager().getModel(CARGO_MODEL);
			if (cargoModel == Minecraft.getInstance().getModelManager().getMissingModel()) {
				return;
			}

			poseStack.pushPose();
			getParentModel().translateToHand(HumanoidArm.RIGHT, poseStack);
			poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
			poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
			poseStack.translate(1.0f / 16.0f, 0.125f, -0.625f);
			poseStack.translate(0.5f, 0.5f, 0.5f);
			PartialItemModelRenderer.of(courier.getPackage(), ItemDisplayContext.NONE, poseStack, buffer,
				OverlayTexture.NO_OVERLAY).render(cargoModel, packedLight);
			poseStack.popPose();
		}
	}
}
