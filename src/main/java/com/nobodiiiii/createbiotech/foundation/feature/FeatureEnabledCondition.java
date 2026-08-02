package com.nobodiiiii.createbiotech.foundation.feature;

import com.google.gson.JsonObject;
import com.nobodiiiii.createbiotech.CreateBiotech;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public record FeatureEnabledCondition(CBFeature feature) implements ICondition {
	public static final ResourceLocation ID = CreateBiotech.asResource("feature_enabled");

	@Override
	public ResourceLocation getID() {
		return ID;
	}

	@Override
	public boolean test(IContext context) {
		return feature.isEnabled();
	}

	public static final class Serializer implements IConditionSerializer<FeatureEnabledCondition> {
		@Override
		public void write(JsonObject json, FeatureEnabledCondition value) {
			json.addProperty("feature", value.feature().serializedName());
		}

		@Override
		public FeatureEnabledCondition read(JsonObject json) {
			return new FeatureEnabledCondition(CBFeature.fromSerializedName(GsonHelper.getAsString(json, "feature")));
		}

		@Override
		public ResourceLocation getID() {
			return ID;
		}
	}
}
