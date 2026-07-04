package com.nobodiiiii.createbiotech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nobodiiiii.createbiotech.content.cardboardbox.CapturedEntityBoxItem;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;

import net.minecraft.world.item.ItemStack;

@Mixin(RepackagerBlockEntity.class)
public abstract class RepackagerBlockEntityCardboardBoxMixin {

	@Inject(method = "unwrapBox", at = @At("HEAD"), cancellable = true, remap = false)
	private void createBiotech$rejectCardboardBoxes(ItemStack box, boolean simulate,
		CallbackInfoReturnable<Boolean> cir) {
		if (CapturedEntityBoxItem.isBox(box))
			cir.setReturnValue(false);
	}

	@Redirect(method = "attemptToRepackage", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/logistics/box/PackageItem;isPackage(Lnet/minecraft/world/item/ItemStack;)Z"),
		remap = false)
	private boolean createBiotech$skipCardboardBoxesDuringRepackaging(ItemStack stack) {
		return PackageItem.isPackage(stack) && !CapturedEntityBoxItem.isBox(stack);
	}
}
