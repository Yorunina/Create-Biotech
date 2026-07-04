package com.nobodiiiii.createbiotech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nobodiiiii.createbiotech.content.cardboardbox.CapturedEntityBoxItem;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;

import net.minecraft.world.item.ItemStack;

@Mixin(PackagerBlockEntity.class)
public abstract class PackagerBlockEntityCardboardBoxMixin {

	@Inject(method = "unwrapBox", at = @At("HEAD"), cancellable = true, remap = false)
	private void createBiotech$rejectCardboardBoxes(ItemStack box, boolean simulate,
		CallbackInfoReturnable<Boolean> cir) {
		if (CapturedEntityBoxItem.isBox(box))
			cir.setReturnValue(false);
	}
}
