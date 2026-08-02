package com.nobodiiiii.createbiotech.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.createmod.ponder.foundation.PonderScene;

/**
 * Backports Ponder's 1.21.1 scene-camera pitch fix to the 1.20.1 runtime.
 */
@Mixin(value = PonderScene.class, remap = false)
public abstract class PonderSceneMixin {

	@ModifyArg(
		method = "renderScene",
		at = @At(value = "INVOKE",
			target = "Lnet/createmod/ponder/foundation/PonderScene$SceneCamera;set(FF)V"),
		index = 0
	)
	private float createBiotech$correctSceneCameraPitch(float legacyPitch) {
		// Ponder 1.20.1 passes scenePitch + 90; Ponder 1.21.1 passes -scenePitch.
		return 90.0f - legacyPitch;
	}
}
