package com.yision.phantom.block.phantomport;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerItemHandler;
import com.simibubi.create.foundation.item.ItemHelper;
import com.yision.phantom.logistics.address.PhantomAddressRules;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

final class PhantomPortAutomation {

	private final PhantomPortBlockEntity port;
	private final PhantomPortInventory inventory;
	private final PhantomPortBeltAccess beltAccess;

	PhantomPortAutomation(PhantomPortBlockEntity port,
						  PhantomPortInventory inventory,
						  PhantomPortBeltAccess beltAccess) {
		this.port = port;
		this.inventory = inventory;
		this.beltAccess = beltAccess;
	}

	void tick() {
		tryPullingFromSide(beltAccess.packagerSide());
	}

	boolean tryPullingFromSide(Direction side) {
		if (!isAutomatedInputSide(side)) {
			return false;
		}
		IItemHandler handler = getAdjacentInventory(side);
		return handler != null && tryPullingFrom(handler);
	}

	private boolean tryPullingFrom(IItemHandler handler) {
		ItemStack extract = ItemHelper.extract(handler, stack -> {
			if (!PackageItem.isPackage(stack)) {
				return false;
			}
			String filterString = port.getFilterString();
			return filterString == null || handler instanceof PackagerItemHandler
				|| !PhantomAddressRules.matchesPackage(stack, filterString);
		}, true);
		if (extract.isEmpty() || !inventory.addPackage(extract, true)) {
			return false;
		}

		ItemStack extracted = ItemHelper.extract(handler, stack -> {
			if (!PackageItem.isPackage(stack)) {
				return false;
			}
			String filterString = port.getFilterString();
			return filterString == null || handler instanceof PackagerItemHandler
				|| !PhantomAddressRules.matchesPackage(stack, filterString);
		}, false);
		if (extracted.isEmpty()) {
			return false;
		}
		boolean inserted = inventory.addPackage(extracted, false);
		if (inserted) {
			port.flap(true);
		}
		return inserted;
	}

	private boolean isAutomatedInputSide(Direction side) {
		return side.getAxis().isHorizontal() && side == beltAccess.packagerSide();
	}

	private @Nullable IItemHandler getAdjacentInventory(Direction side) {
		if (port.getLevel() == null) {
			return null;
		}
		BlockEntity blockEntity = port.getLevel().getBlockEntity(port.getBlockPos().relative(side));
		if (blockEntity == null || blockEntity instanceof PhantomPortBlockEntity) {
			return null;
		}
		return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite()).orElse(null);
	}
}
