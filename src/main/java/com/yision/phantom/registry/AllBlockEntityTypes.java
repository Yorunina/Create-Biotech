package com.yision.phantom.registry;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.yision.phantom.block.phantomport.PhantomPortBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public final class AllBlockEntityTypes {
	public static final RegistryObject<BlockEntityType<PhantomPortBlockEntity>> PHANTOMPORT =
		CBBlockEntityTypes.PHANTOMPORT;

	private AllBlockEntityTypes() {}
}
