package com.yision.phantom.item.miniphantom;

import com.simibubi.create.AllBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class PlayerMiniPhantomClipboardInventory extends ItemStackHandler {
	private static final String ROOT_KEY = "CreateBiotechMiniPhantomClipboard";
	private static final String ADDRESS_KEY = "Address";

	private String address = "";

	public PlayerMiniPhantomClipboardInventory() {
		super(1);
	}

	public static PlayerMiniPhantomClipboardInventory get(Player player) {
		PlayerMiniPhantomClipboardInventory inventory = new PlayerMiniPhantomClipboardInventory();
		CompoundTag persistentData = player.getPersistentData();
		if (persistentData.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
			inventory.deserializeNBT(persistentData.getCompound(ROOT_KEY));
		}
		return inventory;
	}

	public static void save(Player player, PlayerMiniPhantomClipboardInventory inventory) {
		player.getPersistentData().put(ROOT_KEY, inventory.serializeNBT());
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address == null ? "" : address.trim();
	}

	@Override
	public int getSlotLimit(int slot) {
		return 1;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return AllBlocks.CLIPBOARD.isIn(stack);
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = super.serializeNBT();
		if (!address.isBlank()) {
			tag.putString(ADDRESS_KEY, address);
		}
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag tag) {
		super.deserializeNBT(tag);
		setAddress(tag.getString(ADDRESS_KEY));
	}
}
