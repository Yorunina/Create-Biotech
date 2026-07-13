package com.yision.phantom.mixin;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.yision.phantom.block.phantomport.PhantomPortBlock;
import com.yision.phantom.entity.courier.AirCourierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BeltBlock.class)
public abstract class BeltBlockMixin {
	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void createphantom$ignoreAirCouriers(BlockState state, Level level, BlockPos pos, Entity entity,
		CallbackInfo ci) {
		if (entity instanceof AirCourierEntity) {
			ci.cancel();
		}
	}

	@Inject(method = "isBlockCoveringBelt(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Z",
		at = @At("HEAD"), cancellable = true, remap = false)
	private static void createphantom$phantomPortsDoNotCoverBelt(LevelAccessor level, BlockPos pos,
		CallbackInfoReturnable<Boolean> cir) {
		if (level.getBlockState(pos).getBlock() instanceof PhantomPortBlock) {
			cir.setReturnValue(false);
		}
	}
}
