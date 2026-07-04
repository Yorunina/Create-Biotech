package com.yision.phantom.block.phantomport;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.item.ItemHandlerWrapper;
import com.yision.phantom.logistics.address.PhantomAddressRules;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

final class PhantomPortAutomationInventoryWrapper extends ItemHandlerWrapper {
	private final PhantomPortBlockEntity port;

	PhantomPortAutomationInventoryWrapper(IItemHandlerModifiable wrapped, PhantomPortBlockEntity port) {
		super(wrapped);
		this.port = port;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		ItemStack preview = super.extractItem(slot, 64, true);
		if (!PackageItem.isPackage(preview)) {
			return ItemStack.EMPTY;
		}

		String filterString = port.getFilterString();
		if (filterString == null || !PhantomAddressRules.matchesPackage(preview, filterString)) {
			return ItemStack.EMPTY;
		}

		return simulate ? preview : super.extractItem(slot, amount, false);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (!PackageItem.isPackage(stack)) {
			return stack;
		}

		String filterString = port.getFilterString();
		if (filterString != null && PhantomAddressRules.matchesPackage(stack, filterString)) {
			return stack;
		}

		return super.insertItem(slot, stack, simulate);
	}
}
