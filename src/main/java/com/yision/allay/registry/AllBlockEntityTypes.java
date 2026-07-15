package com.yision.allay.registry;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.yision.allay.block.allayport.AllayPortBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public final class AllBlockEntityTypes {
	public static final RegistryObject<BlockEntityType<AllayPortBlockEntity>> ALLAY_PORT =
		CBBlockEntityTypes.ALLAY_PORT;

	private AllBlockEntityTypes() {}
}
