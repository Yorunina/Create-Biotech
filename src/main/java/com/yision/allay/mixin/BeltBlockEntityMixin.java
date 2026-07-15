package com.yision.allay.mixin;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.yision.allay.logistics.courier.AllayCourierHelper;
import com.yision.allay.item.miniallay.MiniAllayItem;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BeltBlockEntity.class)
public abstract class BeltBlockEntityMixin {
	@Inject(method = "tryInsertingFromSide", at = @At("HEAD"), remap = false)
	private void createallay$alignDirectInsertedCourierLaunchStack(TransportedItemStack transportedStack,
		Direction side, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
		if (!AllayCourierHelper.isCourierLaunchStack(transportedStack.stack)) {
			return;
		}

		Direction heading = AllayCourierHelper.resolveBeltHeading((BeltBlockEntity) (Object) this);
		MiniAllayItem.setHeadingAngle(transportedStack.stack, AllayCourierHelper.getHeadingAngle(heading));
		transportedStack.angle = 180;
		transportedStack.sideOffset = transportedStack.prevSideOffset = transportedStack.getTargetSideOffset();
	}
}
