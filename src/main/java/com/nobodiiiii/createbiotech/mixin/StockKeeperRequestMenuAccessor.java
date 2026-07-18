package com.nobodiiiii.createbiotech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;

@Mixin(StockKeeperRequestMenu.class)
public interface StockKeeperRequestMenuAccessor {

	@Accessor(value = "isAdmin", remap = false)
	void createBiotech$setAdmin(boolean isAdmin);

	@Accessor(value = "isLocked", remap = false)
	void createBiotech$setLocked(boolean isLocked);
}
