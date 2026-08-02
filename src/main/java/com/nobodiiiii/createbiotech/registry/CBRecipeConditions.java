package com.nobodiiiii.createbiotech.registry;

import com.nobodiiiii.createbiotech.foundation.feature.FeatureEnabledCondition;

import net.minecraftforge.common.crafting.CraftingHelper;

public final class CBRecipeConditions {
	private CBRecipeConditions() {}

	public static void register() {
		CraftingHelper.register(new FeatureEnabledCondition.Serializer());
	}
}
