package com.nobodiiiii.createbiotech.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nobodiiiii.createbiotech.content.wirelessterminal.WirelessStockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;

import net.minecraft.world.entity.player.Player;

@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin {

	@WrapOperation(
		method = "containerTick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;closeContainer()V",
			remap = true
		)
	)
	private void createBiotech$keepRemoteWirelessTerminalOpen(Player player, Operation<Void> original) {
		if ((Object) this instanceof WirelessStockKeeperRequestScreen)
			return;
		original.call(player);
	}
}
