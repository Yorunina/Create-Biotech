package com.nobodiiiii.createbiotech.registry;

import java.util.Set;

import com.nobodiiiii.createbiotech.CreateBiotech;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CBPoiTypes {

	private static final DeferredRegister<PoiType> POI_TYPES =
		DeferredRegister.create(ForgeRegistries.POI_TYPES, CreateBiotech.MOD_ID);

	public static final ResourceKey<PoiType> TELEPORTATION_KEY =
		ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, CreateBiotech.asResource("teleportation"));

	public static final RegistryObject<PoiType> TELEPORTATION =
		POI_TYPES.register("teleportation",
			() -> new PoiType(
				Set.copyOf(CBFluids.TELEPORTATION_BLOCK.get()
					.getStateDefinition()
					.getPossibleStates()),
				0,
				1));

	private CBPoiTypes() {}

	public static void register(IEventBus modEventBus) {
		POI_TYPES.register(modEventBus);
	}
}
