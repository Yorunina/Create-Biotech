package com.yision.phantom.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.yision.phantom.entity.courier.AirCourierEntity;
import com.yision.phantom.item.miniphantom.MiniPhantomItem;
import com.nobodiiiii.createbiotech.foundation.render.EntityRenderHelper;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MiniPhantomItemRenderer extends CustomRenderedItemModelRenderer {
	private static final float FIXED_CONTEXT_ROLL_DEGREES = 90.0f;

	@Nullable
	private AirCourierEntity cachedCourier;
	@Nullable
	private ClientLevel cachedLevel;

	@Override
	protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
		ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		AirCourierEntity courier = getOrCreateCourier(Minecraft.getInstance().level);
		if (courier == null) {
			ms.pushPose();
			applyHeadingRotation(stack, transformType, ms);
			renderer.render(model.getOriginalModel(), light);
			ms.popPose();
			return;
		}

		configureCourier(courier, stack);

		ms.pushPose();
		applyHeadingRotation(stack, transformType, ms);
		applyDisplayCorrection(transformType, ms);
		boolean guiLighting = transformType == ItemDisplayContext.GUI;
		if (guiLighting) {
			Lighting.setupForEntityInInventory();
		}
		try {
			EntityRenderHelper.render(EntityRenderHelper.settings(courier)
				.packedLight(light)
				.partialTicks(AnimationTickHolder.getPartialTicks())
				.ticks((int) AnimationTickHolder.getRenderTime())
				.dispatcherYaw(0.0f)
				.preserveOrientation()
				.flushBuffers(false), ms, buffer);
		} finally {
			if (guiLighting) {
				Lighting.setupFor3DItems();
			}
		}
		ms.popPose();
	}

	private void configureCourier(AirCourierEntity courier, ItemStack stack) {
		courier.setPackage(MiniPhantomItem.copyCargoPackage(stack));
		courier.setPhase(AirCourierEntity.Phase.WAITING);
		courier.setNoGravity(true);
		courier.setDeltaMovement(Vec3.ZERO);
		courier.setPos(0, 0, 0);
		courier.setYRot(0.0f);
		courier.yRotO = 0.0f;
		courier.setXRot(0.0f);
		courier.xRotO = 0.0f;
	}

	private @Nullable AirCourierEntity getOrCreateCourier(@Nullable Level level) {
		if (!(level instanceof ClientLevel clientLevel)) {
			return null;
		}

		if (cachedCourier == null || cachedLevel != clientLevel) {
			cachedLevel = clientLevel;
			cachedCourier = AirCourierEntity.createWaiting(clientLevel, ItemStack.EMPTY, new Vec3(0, 0, 1));
		}

		return cachedCourier;
	}

	private static void applyHeadingRotation(ItemStack stack, ItemDisplayContext transformType, PoseStack ms) {
		if (transformType != ItemDisplayContext.FIXED && transformType != ItemDisplayContext.GROUND) {
			return;
		}
		if (!MiniPhantomItem.hasHeadingAngle(stack)) {
			return;
		}
		ms.mulPose(Axis.YP.rotationDegrees(MiniPhantomItem.getHeadingAngle(stack)));
	}

	private static void applyDisplayCorrection(ItemDisplayContext transformType, PoseStack ms) {
		if (transformType != ItemDisplayContext.FIXED) {
			return;
		}
		ms.mulPose(Axis.ZP.rotationDegrees(FIXED_CONTEXT_ROLL_DEGREES));
	}
}
