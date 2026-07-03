package com.yision.phantom.registry;

import com.nobodiiiii.createbiotech.registry.CBMenuTypes;
import com.yision.phantom.block.phantomport.PhantomPortMenu;
import com.yision.phantom.item.miniphantom.MiniPhantomMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.RegistryObject;

public final class AllMenuTypes {
	public static final RegistryObject<MenuType<PhantomPortMenu>> PHANTOMPORT = CBMenuTypes.PHANTOMPORT;
	public static final RegistryObject<MenuType<MiniPhantomMenu>> MINI_PHANTOM = CBMenuTypes.MINI_PHANTOM;

	private AllMenuTypes() {}
}
