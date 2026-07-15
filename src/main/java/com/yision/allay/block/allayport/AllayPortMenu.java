package com.yision.allay.block.allayport;

import com.simibubi.create.content.logistics.packagePort.PackagePortMenu;
import com.yision.allay.registry.AllMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class AllayPortMenu extends PackagePortMenu {
	public AllayPortMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		this(AllMenuTypes.ALLAY_PORT.get(), id, inv, extraData);
	}

	public AllayPortMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public AllayPortMenu(MenuType<?> type, int id, Inventory inv, AllayPortBlockEntity blockEntity) {
		super(type, id, inv, blockEntity);
	}

	@Override
	protected AllayPortBlockEntity createOnClient(FriendlyByteBuf extraData) {
		BlockPos readBlockPos = extraData.readBlockPos();
		ClientLevel world = Minecraft.getInstance().level;
		BlockEntity blockEntity = world != null ? world.getBlockEntity(readBlockPos) : null;
		if (blockEntity instanceof AllayPortBlockEntity allayPortBlockEntity) {
			return allayPortBlockEntity;
		}
		return null;
	}

	public static AllayPortMenu create(int id, Inventory inv, AllayPortBlockEntity blockEntity) {
		return new AllayPortMenu(AllMenuTypes.ALLAY_PORT.get(), id, inv, blockEntity);
	}

	@Override
	protected void addSlots() {
		super.addSlots();
		if (contentHolder instanceof AllayPortBlockEntity allayPortBlockEntity) {
			addSlot(new SlotItemHandler(allayPortBlockEntity.getCarrierInventory(), 0, 12, 60) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return AllayPortInventory.isEmptyCarrier(stack);
				}
			});
		}
	}

	@Override
	public @NotNull ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return super.quickMoveStack(player, index);
		}

		ItemStack stack = slot.getItem();
		int carrierSlotIndex = slots.size() - 1;

		if (index == carrierSlotIndex) {
			int originalCount = stack.getCount();
			if (moveItemStackTo(stack, 18, carrierSlotIndex, false)) {
				int moved = originalCount - stack.getCount();
				if (stack.isEmpty()) {
					slot.setByPlayer(ItemStack.EMPTY);
				} else {
					slot.setByPlayer(stack.copy());
				}
				ItemStack result = stack.copy();
				result.setCount(moved);
				return result;
			}
		} else if (AllayPortInventory.isEmptyCarrier(stack)) {
			Slot carrierSlot = slots.get(carrierSlotIndex);
			ItemStack targetStack = carrierSlot.getItem();

			int maxStackSize = stack.getMaxStackSize();
			int space = maxStackSize - (targetStack.isEmpty() ? 0 : targetStack.getCount());

			if (space > 0) {
				int toMove = Math.min(space, stack.getCount());
				if (targetStack.isEmpty()) {
					ItemStack moved = stack.copy();
					moved.setCount(toMove);
					carrierSlot.setByPlayer(moved);
				} else {
					targetStack.grow(toMove);
					carrierSlot.setByPlayer(targetStack.copy());
				}
				stack.shrink(toMove);
				if (stack.isEmpty()) {
					slot.setByPlayer(ItemStack.EMPTY);
				} else {
					slot.setByPlayer(stack.copy());
				}
				ItemStack result = stack.copy();
				result.setCount(toMove);
				return result;
			}
			return ItemStack.EMPTY;
		}

		return super.quickMoveStack(player, index);
	}

}
