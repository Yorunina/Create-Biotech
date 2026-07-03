package com.yision.phantom.logistics.courier.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record AirCourierHudEntry(
	AirCourierHudStatus status,
	int etaSeconds,
	List<ItemStack> displayStacks
) {
	public AirCourierHudEntry {
		displayStacks = AirCourierPackagePreview.copyDisplayStacks(displayStacks);
	}

	public static AirCourierHudEntry read(FriendlyByteBuf buffer) {
		AirCourierHudStatus status = AirCourierHudStatus.byId(buffer.readVarInt());
		int etaSeconds = buffer.readVarInt();
		int size = buffer.readVarInt();
		List<ItemStack> stacks = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			stacks.add(buffer.readItem());
		}
		return new AirCourierHudEntry(status, etaSeconds, stacks);
	}

	public static void write(FriendlyByteBuf buffer, AirCourierHudEntry entry) {
		buffer.writeVarInt(entry.status().ordinal());
		buffer.writeVarInt(entry.etaSeconds());
		buffer.writeVarInt(entry.displayStacks().size());
		for (ItemStack stack : entry.displayStacks()) {
			buffer.writeItem(stack);
		}
	}
}
