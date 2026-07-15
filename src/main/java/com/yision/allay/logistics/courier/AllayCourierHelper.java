package com.yision.allay.logistics.courier;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.yision.allay.block.allayport.AllayPortBlockEntity;
import com.yision.allay.item.miniallay.MiniAllayItem;
import com.yision.allay.registry.AllItems;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public final class AllayCourierHelper {
	private AllayCourierHelper() {}

	public static ServerPlayer findTargetPlayer(ServerLevel level, String playerName) {
		return findTargetPlayer(level, playerName, true);
	}

	public static ServerPlayer findTargetPlayerAnyDimension(ServerLevel level, String playerName) {
		return findTargetPlayer(level, playerName, false);
	}

	private static ServerPlayer findTargetPlayer(ServerLevel level, String playerName, boolean requireSameDimension) {
		String normalizedName = playerName == null ? "" : playerName.trim();
		if (normalizedName.isBlank()) {
			return null;
		}
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			if (requireSameDimension && !player.serverLevel().dimension().equals(level.dimension())) {
				continue;
			}
			if (!player.isAlive()) {
				continue;
			}
			if (!normalizedName.equalsIgnoreCase(player.getGameProfile().getName())) {
				continue;
			}
			return player;
		}
		return null;
	}

	public static boolean canReceiveDelivery(ServerPlayer player, ItemStack box) {
		return PackageItem.isPackage(box)
			&& ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.getInventory()), box.copy(), true)
				.isEmpty();
	}

	public static boolean deliverPackage(ServerPlayer player, ItemStack box) {
		if (!PackageItem.isPackage(box)) {
			return false;
		}
		ItemHandlerHelper.giveItemToPlayer(player, box.copy());
		return true;
	}

	public static boolean deliverPackageOnly(ServerPlayer player, ItemStack box) {
		if (!PackageItem.isPackage(box)) {
			return false;
		}
		return ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.getInventory()), box.copy(), false)
			.isEmpty();
	}

	public static boolean canReceiveCarrier(ServerPlayer player) {
		return ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.getInventory()),
			AllItems.MINI_ALLAY.asStack(), true).isEmpty();
	}

	public static boolean deliverCarrier(ServerPlayer player) {
		ItemHandlerHelper.giveItemToPlayer(player, AllItems.MINI_ALLAY.asStack());
		return true;
	}

	public static void dropPackage(ServerLevel level, Vec3 position, ItemStack box) {
		if (PackageItem.isPackage(box)) {
			level.addFreshEntity(PackageEntity.fromItemStack(level, position, box.copy()));
		}
		level.addFreshEntity(new ItemEntity(level, position.x, position.y, position.z, AllItems.MINI_ALLAY.asStack()));
	}

	public static void dropPackageOnly(ServerLevel level, Vec3 position, ItemStack box) {
		if (level != null && PackageItem.isPackage(box)) {
			PackageEntity packageEntity = PackageEntity.fromItemStack(level, position, box.copy());
			packageEntity.insertionDelay = 0;
			level.addFreshEntity(packageEntity);
		}
	}

	public static TransportedItemStack createAlignedTransportedStack(ItemStack stack, Direction movementDirection) {
		if (isCourierLaunchStack(stack)) {
			MiniAllayItem.setHeadingAngle(stack, getHeadingAngle(movementDirection));
		}
		TransportedItemStack transported = new TransportedItemStack(stack);
		transported.angle = 180;
		transported.sideOffset = transported.prevSideOffset = transported.getTargetSideOffset();
		return transported;
	}

	public static boolean isCourierLaunchStack(ItemStack stack) {
		return stack.is(AllItems.MINI_ALLAY.get())
			&& (MiniAllayItem.hasCargo(stack)
				|| MiniAllayItem.getReturnTarget(stack).isPresent()
				|| MiniAllayItem.getPlayerReturnTarget(stack).isPresent());
	}

	public static int getHeadingAngle(Direction movementDirection) {
		return Math.floorMod(Math.round(AngleHelper.horizontalAngle(movementDirection)), 360);
	}

	public static Direction getHeadingDirection(int headingAngle) {
		int normalized = Math.floorMod(headingAngle, 360);
		Direction bestDirection = Direction.SOUTH;
		float bestDifference = Float.MAX_VALUE;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			float difference = Math.abs(net.minecraft.util.Mth.wrapDegrees(normalized - getHeadingAngle(direction)));
			if (difference < bestDifference) {
				bestDifference = difference;
				bestDirection = direction;
			}
		}
		return bestDirection;
	}

	public static Direction resolveBeltHeading(BeltBlockEntity belt) {
		Vec3i chainDirection = belt.getBeltChainDirection();
		int x = chainDirection.getX();
		int z = chainDirection.getZ();
		if (x == 0 && z == 0) {
			return belt.getMovementFacing();
		}
		if (Math.abs(x) > Math.abs(z)) {
			return x > 0 ? Direction.EAST : Direction.WEST;
		}
		return z > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	public static Direction resolveCourierHeading(BeltBlockEntity belt, TransportedItemStack stack) {
		if (isCourierLaunchStack(stack.stack) && MiniAllayItem.hasHeadingAngle(stack.stack)) {
			return getHeadingDirection(MiniAllayItem.getHeadingAngle(stack.stack));
		}
		return resolveBeltHeading(belt);
	}

	public static Vec3 getCourierLaunchDirection(BeltBlockEntity belt, TransportedItemStack stack) {
		return Vec3.atLowerCornerOf(resolveCourierHeading(belt, stack).getNormal()).normalize();
	}

	public static Vec3 getCourierLaunchMotion(BeltBlockEntity belt, TransportedItemStack stack) {
		float movementSpeed = Math.max(Math.abs(belt.getBeltMovementSpeed()), 1 / 8f);
		Vec3 chainMotion = Vec3.atLowerCornerOf(belt.getBeltChainDirection()).scale(movementSpeed);
		Vec3 launchDirection = getCourierLaunchDirection(belt, stack);
		return new Vec3(launchDirection.x * movementSpeed, Math.max(chainMotion.y, 0) + movementSpeed,
			launchDirection.z * movementSpeed);
	}

	public static BlockPos findSourceAllayPortPos(BeltBlockEntity belt) {
		if (belt.getLevel() == null) {
			return null;
		}
		BlockPos beltPos = belt.getBlockPos();
		BlockEntity directlyAbove = belt.getLevel().getBlockEntity(beltPos.above());
		if (directlyAbove instanceof AllayPortBlockEntity allayPortBlockEntity) {
			return allayPortBlockEntity.getBlockPos();
		}
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockEntity blockEntity = belt.getLevel().getBlockEntity(beltPos.above().relative(direction));
			if (blockEntity instanceof AllayPortBlockEntity allayPortBlockEntity) {
				return allayPortBlockEntity.getBlockPos();
			}
		}
		return null;
	}

}
