package com.yision.phantom.registry;

import com.nobodiiiii.createbiotech.registry.CBEntityTypes;
import com.yision.phantom.entity.courier.AirCourierEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.RegistryObject;

public final class AllEntityTypes {
	public static final RegistryObject<EntityType<AirCourierEntity>> AIR_COURIER = CBEntityTypes.AIR_COURIER;

	private AllEntityTypes() {}
}
