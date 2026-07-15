package com.yision.allay.mixin;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.yision.allay.logistics.courier.AllayCourierDispatchService;
import com.yision.allay.entity.courier.AllayCourierEntity;
import com.yision.allay.logistics.courier.AllayCourierHelper;
import com.yision.allay.logistics.courier.AllayCourierLaunchRules;
import com.yision.allay.logistics.courier.AllayCourierTask;
import com.yision.allay.logistics.courier.AllayCourierTaskManager;
import com.yision.allay.logistics.courier.AllayCourierTarget;
import com.yision.allay.item.miniallay.MiniAllayItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BeltInventory.class)
public abstract class BeltInventoryMixin {
	@Shadow(remap = false)
	@Final
	BeltBlockEntity belt;

	@Shadow(remap = false)
	boolean beltMovementPositive;

	@Inject(method = "eject", at = @At("HEAD"), cancellable = true, remap = false)
	private void createallay$launchCourier(TransportedItemStack stack, CallbackInfo ci) {
		if (!AllayCourierHelper.isCourierLaunchStack(stack.stack)) {
			return;
		}
		if (!AllayCourierLaunchRules.canLaunchFrom(belt, stack.insertedAt, beltMovementPositive)) {
			return;
		}

		if (MiniAllayItem.hasCargo(stack.stack)) {
			launchPackageCourier(stack, ci);
			return;
		}
		if (MiniAllayItem.getReturnTarget(stack.stack).isPresent()) {
			launchReturningCarrierToAllayPort(stack, ci);
			return;
		}
		if (MiniAllayItem.getPlayerReturnTarget(stack.stack).isPresent()) {
			launchReturningCarrierToPlayer(stack, ci);
		}
	}

	private void launchPackageCourier(TransportedItemStack stack, CallbackInfo ci) {
		if (!(belt.getLevel() instanceof ServerLevel serverLevel)) {
			ci.cancel();
			return;
		}

		var box = MiniAllayItem.copyCargoPackage(stack.stack);
		var sourceReturnTarget = MiniAllayItem.getReturnTarget(stack.stack);
		var sourceAllayPortDimension = sourceReturnTarget.map(target -> target.dimension())
			.orElse(serverLevel.dimension());
		var sourceAllayPortPos = sourceReturnTarget.map(target -> target.pos())
			.orElseGet(() -> AllayCourierHelper.findSourceAllayPortPos(belt));
		var returnMode = MiniAllayItem.getReturnMode(stack.stack);
		AllayCourierTarget target = AllayCourierDispatchService.resolvePackageTarget(serverLevel, box,
			Vec3.atCenterOf(belt.getBlockPos()), sourceAllayPortDimension, sourceAllayPortPos);
		if (target == null) {
			return;
		}

		Vec3 outPos = BeltHelper.getVectorForOffset(belt, stack.beltPosition);
		Vec3 launchDirection = AllayCourierHelper.getCourierLaunchDirection(belt, stack);
		Vec3 launchMotion = AllayCourierHelper.getCourierLaunchMotion(belt, stack);
		Vec3 spawnPos = outPos.add(launchMotion.normalize().scale(0.001)).add(0, 6 / 16f, 0);

		UUID taskId = UUID.randomUUID();

		AllayCourierTask task;
		if (target instanceof AllayCourierTarget.AllayPortTarget allayPort) {
			task = AllayCourierTask.forPackageToAllayPort(
				taskId, box, serverLevel, allayPort.dimension(), allayPort.pos(),
				spawnPos, launchDirection,
				sourceAllayPortDimension, sourceAllayPortPos, null, returnMode);
		} else if (target instanceof AllayCourierTarget.PlayerTarget player) {
			ServerPlayer targetPlayer = serverLevel.getServer().getPlayerList().getPlayer(player.playerId());
			task = targetPlayer != null
				? AllayCourierTask.forPackageToPlayer(taskId, box, serverLevel, player.playerId(),
					player.dimension(), spawnPos, launchDirection,
					sourceAllayPortDimension, sourceAllayPortPos, null, returnMode)
				: null;
		} else {
			task = null;
		}
		if (task == null) {
			return;
		}

		AllayCourierTaskManager.addTask(serverLevel.getServer(), task);

		ci.cancel();
	}

	private void launchReturningCarrierToAllayPort(TransportedItemStack stack, CallbackInfo ci) {
		var returnTarget = MiniAllayItem.getReturnTarget(stack.stack);
		if (returnTarget.isEmpty()) {
			return;
		}
		if (!(belt.getLevel() instanceof ServerLevel serverLevel)) {
			ci.cancel();
			return;
		}

		Vec3 outPos = BeltHelper.getVectorForOffset(belt, stack.beltPosition);
		Vec3 launchDirection = AllayCourierHelper.getCourierLaunchDirection(belt, stack);
		Vec3 launchMotion = AllayCourierHelper.getCourierLaunchMotion(belt, stack);
		Vec3 spawnPos = outPos.add(launchMotion.normalize().scale(0.001)).add(0, 6 / 16f, 0);

		var target = returnTarget.get();
		AllayCourierTask task = AllayCourierTask.forCarrierReturn(
			UUID.randomUUID(), serverLevel, target.dimension(), target.pos(),
			spawnPos, launchDirection);

		AllayCourierTaskManager.addTask(serverLevel.getServer(), task);

		ci.cancel();
	}

	private void launchReturningCarrierToPlayer(TransportedItemStack stack, CallbackInfo ci) {
		var returnTarget = MiniAllayItem.getPlayerReturnTarget(stack.stack);
		if (returnTarget.isEmpty()) {
			return;
		}
		if (!(belt.getLevel() instanceof ServerLevel serverLevel)) {
			ci.cancel();
			return;
		}
		ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(returnTarget.get());
		if (player == null || !player.isAlive()) {
			return;
		}

		Vec3 outPos = BeltHelper.getVectorForOffset(belt, stack.beltPosition);
		Vec3 launchDirection = AllayCourierHelper.getCourierLaunchDirection(belt, stack);
		Vec3 launchMotion = AllayCourierHelper.getCourierLaunchMotion(belt, stack);
		Vec3 spawnPos = outPos.add(launchMotion.normalize().scale(0.001)).add(0, 6 / 16f, 0);

		AllayCourierTask task = AllayCourierTask.forCarrierReturnToPlayer(
			UUID.randomUUID(), serverLevel, player.getUUID(), player.serverLevel().dimension(),
			spawnPos, launchDirection);

		AllayCourierTaskManager.addTask(serverLevel.getServer(), task);

		ci.cancel();
	}
}
