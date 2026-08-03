package com.nobodiiiii.createbiotech.content.buttercat.item;

import com.nobodiiiii.createbiotech.content.buttercat.register.ModEffects;
import com.nobodiiiii.createbiotech.registry.CBConfigs;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ButterFoodProperties {

	public enum Variant {
		BUTTER,
		SUPER_BUTTER,
		INCOMPLETE_SUPER_BUTTER
	}

	private ButterFoodProperties() {}

	public static FoodProperties create(Variant variant) {
		CBConfigs.ButterCat config = CBConfigs.SERVER.butterCat;
		FoodProperties.Builder builder = switch (variant) {
		case BUTTER -> food(value(config.butterNutrition), value(config.butterSaturation));
		case SUPER_BUTTER -> food(value(config.superButterNutrition), value(config.superButterSaturation))
			.withEffect(ModEffects.BUTTER_ROTATION_EFFECT.get(), value(config.superButterRotationDuration),
				value(config.superButterRotationAmplifier))
			.withEffect(MobEffects.LEVITATION, value(config.superButterLevitationDuration),
				value(config.superButterLevitationAmplifier));
		case INCOMPLETE_SUPER_BUTTER -> food(value(config.incompleteSuperButterNutrition),
			value(config.incompleteSuperButterSaturation))
				.withEffect(ModEffects.BUTTER_ROTATION_EFFECT.get(), value(config.incompleteSuperButterRotationDuration),
					value(config.incompleteSuperButterRotationAmplifier));
		};
		return builder.build();
	}

	private static <T> T value(ForgeConfigSpec.ConfigValue<T> configValue) {
		return CBConfigs.SERVER_SPEC.isLoaded() ? configValue.get() : configValue.getDefault();
	}

	private static Builder food(int nutrition, double saturation) {
		return new Builder(nutrition, saturation);
	}

	private static class Builder extends FoodProperties.Builder {
		private Builder(int nutrition, double saturation) {
			nutrition(nutrition);
			saturationMod((float) saturation);
		}

		private Builder withEffect(MobEffect effect, int duration, int amplifier) {
			if (duration > 0)
				effect(() -> new MobEffectInstance(effect, duration, amplifier), 1.0f);
			return this;
		}
	}
}
