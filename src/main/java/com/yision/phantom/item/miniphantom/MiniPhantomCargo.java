package com.yision.phantom.item.miniphantom;

import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.world.item.ItemStack;

public record MiniPhantomCargo(ItemStack packageStack) {
	public MiniPhantomCargo {
		packageStack = sanitize(packageStack);
	}

	public boolean isValid() {
		return PackageItem.isPackage(packageStack);
	}

	public ItemStack packageCopy() {
		return packageStack.copy();
	}

	private static ItemStack sanitize(ItemStack stack) {
		if (!PackageItem.isPackage(stack)) {
			return ItemStack.EMPTY;
		}

		ItemStack copy = stack.copy();
		copy.setCount(1);
		return copy;
	}
}
