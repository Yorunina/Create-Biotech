package com.nobodiiiii.createbiotech.content.beltsurface;

import javax.annotation.Nullable;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** Shared one-slot capability contract for a standard item-belt segment. */
public abstract class AbstractItemHandlerBeltSegment implements IItemHandler {

	@Nullable
	protected abstract TransportedItemStack getTransportedStack();

	protected abstract boolean canInsert();

	protected abstract void insertTransportedStack(ItemStack stack);

	protected abstract void onExtracted(TransportedItemStack transported, boolean emptied);

	@Override
	public final int getSlots() {
		return 1;
	}

	@Override
	public final ItemStack getStackInSlot(int slot) {
		TransportedItemStack transported = getTransportedStack();
		return transported == null ? ItemStack.EMPTY : transported.stack;
	}

	@Override
	public final ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (!canInsert())
			return stack;
		ItemStack remainder = ItemHelper.limitCountToMaxStackSize(stack, simulate);
		if (!simulate)
			insertTransportedStack(stack);
		return remainder;
	}

	@Override
	public final ItemStack extractItem(int slot, int amount, boolean simulate) {
		TransportedItemStack transported = getTransportedStack();
		if (transported == null)
			return ItemStack.EMPTY;
		amount = Math.min(amount, transported.stack.getCount());
		ItemStack extracted = simulate ? transported.stack.copy().split(amount) : transported.stack.split(amount);
		if (!simulate)
			onExtracted(transported, transported.stack.isEmpty());
		return extracted;
	}

	@Override
	public final int getSlotLimit(int slot) {
		return Math.min(getStackInSlot(slot).getMaxStackSize(), 64);
	}

	@Override
	public final boolean isItemValid(int slot, ItemStack stack) {
		return true;
	}
}
