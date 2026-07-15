package com.yision.allay.registry;

import com.nobodiiiii.createbiotech.registry.CBEntityTypes;
import com.yision.allay.entity.courier.AllayCourierEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.RegistryObject;

public final class AllEntityTypes {
	public static final RegistryObject<EntityType<AllayCourierEntity>> ALLAY_COURIER = CBEntityTypes.ALLAY_COURIER;

	private AllEntityTypes() {}
}
