package com.nobodiiiii.createbiotech.foundation.block;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags.AllItemTags;

import net.minecraft.world.item.ItemStack;

public final class CBWrenchHelper {

	private CBWrenchHelper() {}

	public static boolean isWrench(ItemStack stack) {
		return AllItems.WRENCH.isIn(stack)
			|| AllItemTags.WRENCH.matches(stack.getItem());
	}
}
