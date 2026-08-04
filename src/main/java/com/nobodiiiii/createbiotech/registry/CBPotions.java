package com.nobodiiiii.createbiotech.registry;

import com.nobodiiiii.createbiotech.CreateBiotech;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CBPotions {
	private static final DeferredRegister<Potion> POTIONS =
		DeferredRegister.create(Registries.POTION, CreateBiotech.MOD_ID);

	public static final RegistryObject<Potion> ROTATION = POTIONS.register("rotation_potion",
		() -> new Potion(new MobEffectInstance(CBMobEffects.BUTTER_ROTATION.get(), 1200, 2)));

	public static final RegistryObject<Potion> SUPER_ROTATION = POTIONS.register("super_rotation_potion",
		() -> new Potion(new MobEffectInstance(CBMobEffects.BUTTER_ROTATION.get(), 3600, 4)));

	private CBPotions() {}

	public static void register(IEventBus modEventBus) {
		POTIONS.register(modEventBus);
	}
}
