package com.nobodiiiii.createbiotech.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.content.processing.sequenced.SequencedRecipe;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CompoundIngredient;

@Mixin(SequencedRecipe.class)
public abstract class SequencedRecipeMixin {

	@Redirect(method = "fromJson", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/crafting/Ingredient;merge(Ljava/util/Collection;)Lnet/minecraft/world/item/crafting/Ingredient;",
		remap = false), remap = false)
	private static Ingredient createBiotech$preserveCustomAssemblyIngredient(Collection<Ingredient> ingredients) {
		return CompoundIngredient.of(ingredients.toArray(Ingredient[]::new));
	}
}
