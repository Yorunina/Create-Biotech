package com.yision.allay.block.allayport;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.yision.allay.item.miniallay.MiniAllayItem;
import com.yision.allay.logistics.address.AllayAddressRules;
import com.yision.allay.logistics.courier.AllayCourierDispatchService;
import com.yision.allay.logistics.courier.AllayCourierHelper;
import com.yision.allay.logistics.courier.AllayCourierTarget;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

final class AllayPortDispatchAccess {

	private final AllayPortBlockEntity port;
	private final AllayPortInventory inventory;
	private final AllayPortBeltAccess beltAccess;

	AllayPortDispatchAccess(AllayPortBlockEntity port,
							  AllayPortInventory inventory,
							  AllayPortBeltAccess beltAccess) {
		this.port = port;
		this.inventory = inventory;
		this.beltAccess = beltAccess;
	}

	@Nullable IItemHandler getItemHandler(@Nullable net.minecraft.core.Direction side) {
		return inventory.combinedHandler();
	}

	boolean tryDispatchToLaunchBelt() {
		if (!(port.getLevel() instanceof ServerLevel serverLevel)) {
			return false;
		}
		if (!beltAccess.canLaunchFromBelt() || !inventory.hasStoredCarrier()) {
			return false;
		}

		DispatchCandidate candidate = findDispatchCandidate(serverLevel);
		if (candidate == null) {
			return false;
		}
		ItemStack allayStack = candidate.allayStack().copy();
		if (!beltAccess.canAcceptLaunchStack(allayStack)) {
			return false;
		}

		ItemStack extractedPackage = port.inventory.extractItem(candidate.packageSlot(), 1, false);
		ItemStack extractedCarrier = inventory.extractOneCarrier(false);
		if (extractedPackage.isEmpty() || extractedCarrier.isEmpty()) {
			restoreFailedDispatch(candidate.packageSlot(), extractedPackage, extractedCarrier);
			return false;
		}
		if (!beltAccess.insertToLaunchBelt(allayStack)) {
			restoreFailedDispatch(candidate.packageSlot(), extractedPackage, extractedCarrier);
			return false;
		}

		port.flap(false);
		port.markPortContentsChanged();
		return true;
	}

	private void restoreFailedDispatch(int packageSlot, ItemStack extractedPackage, ItemStack extractedCarrier) {
		if (!extractedPackage.isEmpty()) {
			ItemStack remainder = port.inventory.insertItem(packageSlot, extractedPackage, false);
			if (!remainder.isEmpty()) {
				port.drop(remainder);
			}
		}
		if (!extractedCarrier.isEmpty()) {
			inventory.returnCarrier(extractedCarrier);
		}
		port.markPortContentsChanged();
	}

	private @Nullable DispatchCandidate findDispatchCandidate(ServerLevel serverLevel) {
		Direction heading = beltAccess.resolveBeltHeading();
		int headingAngle = AllayCourierHelper.getHeadingAngle(heading);
		String filterString = port.getFilterString();
		for (int slot = 0; slot < port.inventory.getSlots(); slot++) {
			ItemStack packageInSlot = port.inventory.getStackInSlot(slot);
			if (packageInSlot.isEmpty() || !PackageItem.isPackage(packageInSlot)) {
				continue;
			}
			if (filterString != null && !AllayAddressRules.isBlank(filterString)
				&& AllayAddressRules.matchesPackage(packageInSlot, filterString)) {
				continue;
			}
			AllayCourierTarget target = AllayCourierDispatchService.resolvePackageTarget(serverLevel, packageInSlot,
				Vec3.atCenterOf(port.getBlockPos()), serverLevel.dimension(), port.getBlockPos());
			if (target == null) {
				continue;
			}

			ItemStack singlePackage = packageInSlot.copy();
			singlePackage.setCount(1);
			ItemStack allayStack = MiniAllayItem.createLoadedWithHeading(singlePackage, headingAngle);
			MiniAllayItem.setReturnTarget(allayStack, serverLevel.dimension(), port.getBlockPos());
			MiniAllayItem.setReturnMode(allayStack, port.getReturnMode());

			return new DispatchCandidate(slot, allayStack);
		}
		return null;
	}

	private record DispatchCandidate(int packageSlot, ItemStack allayStack) {}
}
