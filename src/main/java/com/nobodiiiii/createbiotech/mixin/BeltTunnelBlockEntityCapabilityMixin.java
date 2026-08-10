package com.nobodiiiii.createbiotech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nobodiiiii.createbiotech.content.beltsurface.BeltTunnelCapabilityInvalidator;
import com.nobodiiiii.createbiotech.content.beltsurface.StandardItemBeltPort;
import com.nobodiiiii.createbiotech.content.beltsurface.StandardItemBeltPortResolver;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

@Mixin(BeltTunnelBlockEntity.class)
public abstract class BeltTunnelBlockEntityCapabilityMixin implements BeltTunnelCapabilityInvalidator {

	@Shadow(remap = false)
	protected LazyOptional<IItemHandler> cap;

	// Forge owns getCapability and its Forge types; Direction is identity-mapped by the 1.20.1 TSRG.
	@Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, remap = false)
	private <T> void createBiotech$getItemCapability(Capability<T> capability, Direction side,
		CallbackInfoReturnable<LazyOptional<T>> cir) {
		if (capability != ForgeCapabilities.ITEM_HANDLER)
			return;

		BeltTunnelBlockEntity tunnel = (BeltTunnelBlockEntity) (Object) this;
		Level level = tunnel.getLevel();
		if (level == null)
			return;
		StandardItemBeltPort belt =
			StandardItemBeltPortResolver.getHorizontalPort(level, tunnel.getBlockPos().below());
		if (belt == null)
			return;

		if (!cap.isPresent()) {
			IItemHandler handler = belt.createBiotech$getItemHandler();
			if (handler != null)
				cap = LazyOptional.of(() -> handler);
		}
		cir.setReturnValue(cap.cast());
	}

	@Override
	public void createBiotech$clearItemCapability() {
		cap.invalidate();
		cap = LazyOptional.empty();
	}
}
