package com.nobodiiiii.createbiotech.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nobodiiiii.createbiotech.compat.jei.CapturedEntityBoxJeiRenderer;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;

@Pseudo
@Mixin(targets = "mezz.jei.library.gui.recipes.RecipeLayout", remap = false)
public abstract class JeiRecipeLayoutMixin {
	@WrapOperation(method = "drawRecipe", at = @At(value = "INVOKE",
		target = "Lmezz/jei/api/gui/ingredient/IRecipeSlotDrawable;draw(Lnet/minecraft/client/gui/GuiGraphics;)V"),
		require = 0)
	private void createBiotech$drawLegacySlotWithHoverContext(IRecipeSlotDrawable slot, GuiGraphics slotGraphics,
		Operation<Void> original, GuiGraphics methodGraphics, int mouseX, int mouseY) {
		Rect2i recipeArea = ((IRecipeLayoutDrawable<?>) (Object) this).getRect();
		boolean hovered = slot.isMouseOver(mouseX - recipeArea.getX(), mouseY - recipeArea.getY());
		CapturedEntityBoxJeiRenderer.beginSlotDraw(slot, hovered);
		try {
			original.call(slot, slotGraphics);
		} finally {
			CapturedEntityBoxJeiRenderer.endSlotDraw();
		}
	}

	@WrapOperation(method = "drawRecipe", at = @At(value = "INVOKE",
		target = "Lmezz/jei/api/gui/ingredient/IRecipeSlotDrawable;draw(Lnet/minecraft/client/gui/GuiGraphics;Z)V"),
		require = 0)
	private void createBiotech$drawSlotWithHoverContext(IRecipeSlotDrawable slot, GuiGraphics slotGraphics,
		boolean hovered, Operation<Void> original) {
		CapturedEntityBoxJeiRenderer.beginSlotDraw(slot, hovered);
		try {
			original.call(slot, slotGraphics, hovered);
		} finally {
			CapturedEntityBoxJeiRenderer.endSlotDraw();
		}
	}
}
