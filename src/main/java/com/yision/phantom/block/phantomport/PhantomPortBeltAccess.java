package com.yision.phantom.block.phantomport;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.yision.phantom.logistics.courier.AirCourierHelper;
import com.yision.phantom.logistics.courier.AirCourierLaunchRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

final class PhantomPortBeltAccess {

	private final PhantomPortBlockEntity port;

	PhantomPortBeltAccess(PhantomPortBlockEntity port) {
		this.port = port;
	}

	Direction specialSide() {
		return port.getBlockState().getValue(PhantomPortBlock.FACING);
	}

	Direction packagerSide() {
		return specialSide().getOpposite();
	}

	boolean canLaunchFromBelt() {
		BeltBlockEntity belt = launchBelt();
		return belt != null
			&& AirCourierLaunchRules.canLaunchFrom(belt, belt.index, belt.getDirectionAwareBeltMovementSpeed() > 0);
	}

	@Nullable BeltBlockEntity launchBelt() {
		if (port.getLevel() == null) {
			return null;
		}
		BlockPos beltPos = beltPos();
		BlockState beltState = port.getLevel().getBlockState(beltPos);
		if (!AllBlocks.BELT.has(beltState) || beltState.getValue(BeltBlock.SLOPE) != BeltSlope.HORIZONTAL) {
			return null;
		}
		return port.getLevel().getBlockEntity(beltPos) instanceof BeltBlockEntity beltBlockEntity ? beltBlockEntity
			: null;
	}

	Direction resolveBeltHeading() {
		BeltBlockEntity belt = launchBelt();
		return belt == null ? specialSide() : AirCourierHelper.resolveBeltHeading(belt);
	}

	@Nullable IItemHandler launchBeltHandler() {
		BeltBlockEntity belt = launchBelt();
		if (belt == null) {
			return null;
		}
		BlockEntity blockEntity = port.getLevel().getBlockEntity(belt.getBlockPos());
		return blockEntity == null ? null
			: blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
	}

	boolean canAcceptLaunchStack(ItemStack stack) {
		if (!canLaunchFromBelt()) {
			return false;
		}
		IItemHandler beltHandler = launchBeltHandler();
		return beltHandler != null && beltHandler.insertItem(0, stack.copy(), true).isEmpty();
	}

	boolean insertToLaunchBelt(ItemStack stack) {
		if (!canLaunchFromBelt()) {
			return false;
		}
		IItemHandler beltHandler = launchBeltHandler();
		return beltHandler != null && beltHandler.insertItem(0, stack.copy(), false).isEmpty();
	}

	boolean tryInsertToLaunchBelt(ItemStack stack) {
		return canAcceptLaunchStack(stack) && insertToLaunchBelt(stack);
	}

	BlockPos beltPos() {
		return port.getBlockPos().below();
	}
}
