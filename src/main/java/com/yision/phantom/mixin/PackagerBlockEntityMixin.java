package com.yision.phantom.mixin;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.yision.phantom.block.phantomport.PhantomPortWakeupHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackagerBlockEntity.class)
public abstract class PackagerBlockEntityMixin {
	@Inject(method = "wakeTheFrogs", at = @At("TAIL"), remap = false)
	private void createphantom$wakeAdjacentPhantomPorts(CallbackInfo ci) {
		PhantomPortWakeupHandler.tryWakeAdjacentPorts((PackagerBlockEntity) (Object) this);
	}
}
