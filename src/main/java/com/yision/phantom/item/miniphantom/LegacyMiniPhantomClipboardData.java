package com.yision.phantom.item.miniphantom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

final class LegacyMiniPhantomClipboardData {
	private static final String ROOT_KEY = "CreateBiotechMiniPhantomClipboard";

	private LegacyMiniPhantomClipboardData() {}

	static void recoverClipboard(Player player) {
		CompoundTag persistentData = player.getPersistentData();
		if (!persistentData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
			return;
		}

		ItemStackHandler legacyInventory = new ItemStackHandler(1);
		legacyInventory.deserializeNBT(persistentData.getCompound(ROOT_KEY));
		ItemStack clipboard = legacyInventory.getStackInSlot(0).copy();
		persistentData.remove(ROOT_KEY);
		if (!clipboard.isEmpty()) {
			ItemHandlerHelper.giveItemToPlayer(player, clipboard);
		}
	}
}
