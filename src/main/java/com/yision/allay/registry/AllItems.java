package com.yision.allay.registry;

import com.nobodiiiii.createbiotech.registry.CBItems;
import com.yision.allay.item.miniallay.MiniAllayItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

public final class AllItems {
	public static final ItemRef<MiniAllayItem> MINI_ALLAY = new ItemRef<>(CBItems.MINI_ALLAY);

	private AllItems() {}

	public record ItemRef<T extends Item>(RegistryObject<T> object) {
		public T get() {
			return object.get();
		}

		public ItemStack asStack() {
			return new ItemStack(get());
		}
	}
}
