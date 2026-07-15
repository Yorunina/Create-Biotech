package com.yision.allay.logistics.courier;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.yision.allay.block.allayport.AllayPortBlockEntity;
import com.yision.allay.logistics.address.AllayAddressRules;
import com.yision.allay.block.allayport.AllayPortTargetRegistry;
import com.yision.allay.block.allayport.AllayPortTargetRegistry.TargetLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class AllayCourierDispatchService {
	private AllayCourierDispatchService() {}

	public static @Nullable AllayCourierTarget resolvePackageTarget(ServerLevel level, ItemStack box,
		Vec3 origin, @Nullable ResourceKey<Level> sourceDimension, @Nullable BlockPos sourcePos) {
		if (!PackageItem.isPackage(box)) {
			return null;
		}

		String rawAddress = PackageItem.getAddress(box);
		String address = AllayAddressRules.normalize(rawAddress);
		if (address.isBlank()) {
			return null;
		}

		if (AllayAddressRules.isExplicitPlayerAddress(rawAddress)) {
			return findPlayer(level, AllayAddressRules.explicitPlayerName(rawAddress), box);
		}

		AllayCourierTarget.AllayPortTarget allayPort =
			findAllayPortExcludingSource(level, address, box, origin, sourceDimension, sourcePos);
		return allayPort != null ? allayPort : findPlayer(level, address, box);
	}

	private static @Nullable AllayCourierTarget.PlayerTarget findPlayer(ServerLevel level, String playerName, ItemStack box) {
		ServerPlayer player = AllayCourierDimensionRules.allowCrossDimensionDelivery()
			? AllayCourierHelper.findTargetPlayerAnyDimension(level, playerName)
			: AllayCourierHelper.findTargetPlayer(level, playerName);
		if (player == null) {
			return null;
		}
		AllayCourierTarget.PlayerTarget target =
			new AllayCourierTarget.PlayerTarget(player.getUUID(), player.serverLevel().dimension());
		return canReceivePackageTarget(level, target, box) ? target : null;
	}

	private static @Nullable AllayCourierTarget.AllayPortTarget findAllayPortExcludingSource(ServerLevel level,
		String address, ItemStack box, Vec3 origin, @Nullable ResourceKey<Level> sourceDimension,
		@Nullable BlockPos sourcePos) {
		TargetLocation location = AllayPortTargetRegistry.findMatchingAnyDimension(level, address, origin,
			sourceDimension, sourcePos, target -> AllayCourierDimensionRules.canTarget(level, target.dimension())
					&& canReceiveAllayPortTarget(level, target, box));
		if (location == null) {
			return null;
		}
		return new AllayCourierTarget.AllayPortTarget(location.dimension(), location.pos());
	}

	public static boolean canReceivePackageTarget(ServerLevel level, AllayCourierTarget target, ItemStack box) {
		if (!AllayCourierDimensionRules.canTarget(level, target.dimension())) {
			return false;
		}
		if (target instanceof AllayCourierTarget.PlayerTarget playerTarget) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerTarget.playerId());
			return player != null && player.isAlive()
				&& player.serverLevel().dimension().equals(playerTarget.dimension());
		}
		if (target instanceof AllayCourierTarget.AllayPortTarget allayPortTarget) {
			return canReceiveAllayPortTarget(level,
				new TargetLocation(allayPortTarget.dimension(), allayPortTarget.pos(), ""), box);
		}
		return false;
	}

	private static boolean canReceiveAllayPortTarget(ServerLevel level, TargetLocation target, ItemStack box) {
		ServerLevel targetLevel = level.getServer().getLevel(target.dimension());
		if (targetLevel == null) {
			return false;
		}
		BlockEntity blockEntity = targetLevel.getBlockEntity(target.pos());
		return blockEntity instanceof AllayPortBlockEntity allayPort && allayPort.acceptsPackages;
	}
}
