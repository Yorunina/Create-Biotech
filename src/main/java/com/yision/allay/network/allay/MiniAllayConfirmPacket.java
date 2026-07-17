package com.yision.allay.network.allay;

import com.yision.allay.item.miniallay.MiniAllayItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public class MiniAllayConfirmPacket {
	private static final int MAX_ADDRESS_LENGTH = 25;

	private final InteractionHand hand;
	private final String address;

	public MiniAllayConfirmPacket(InteractionHand hand, String address) {
		this.hand = hand;
		String normalizedAddress = address == null ? "" : address.trim();
		this.address = normalizedAddress.length() > MAX_ADDRESS_LENGTH
			? normalizedAddress.substring(0, MAX_ADDRESS_LENGTH)
			: normalizedAddress;
	}

	public MiniAllayConfirmPacket(FriendlyByteBuf buffer) {
		this(buffer.readEnum(InteractionHand.class), buffer.readUtf(MAX_ADDRESS_LENGTH));
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeEnum(hand);
		buffer.writeUtf(address, MAX_ADDRESS_LENGTH);
	}

	public void handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer sender = context.getSender();
			if (sender == null) {
				return;
			}
			ItemStack heldStack = sender.getItemInHand(hand);
			if (MiniAllayItem.updateCargoAddress(heldStack, address)) {
				sender.getInventory().setChanged();
				sender.inventoryMenu.broadcastChanges();
			}
		});
	}
}
