package com.nobodiiiii.createbiotech.registry;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.content.buttercat.mob_effect.ButterRotationEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CBMobEffects {
	private static final DeferredRegister<MobEffect> MOB_EFFECTS =
		DeferredRegister.create(Registries.MOB_EFFECT, CreateBiotech.MOD_ID);

	public static final RegistryObject<ButterRotationEffect> BUTTER_ROTATION =
		MOB_EFFECTS.register("rotation", ButterRotationEffect::new);

	private CBMobEffects() {}

	public static void register(IEventBus modEventBus) {
		MOB_EFFECTS.register(modEventBus);
	}
}
