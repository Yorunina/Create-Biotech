package com.yision.allay.mixin;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.yision.allay.block.allayport.AllayPortWakeupHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackagerBlockEntity.class)
public abstract class PackagerBlockEntityMixin {
	@Inject(method = "wakeTheFrogs", at = @At("TAIL"), remap = false)
	private void createallay$wakeAdjacentAllayPorts(CallbackInfo ci) {
		AllayPortWakeupHandler.tryWakeAdjacentPorts((PackagerBlockEntity) (Object) this);
	}
}
