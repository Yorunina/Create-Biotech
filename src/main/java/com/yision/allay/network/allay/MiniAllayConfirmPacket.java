package com.yision.allay.network.allay;

import com.yision.allay.item.miniallay.MiniAllayMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class MiniAllayConfirmPacket {
	private final String address;

	public MiniAllayConfirmPacket(String address) {
		this.address = address == null ? "" : address;
	}

	public MiniAllayConfirmPacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf());
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(address);
	}

	public void handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer sender = context.getSender();
			if (sender == null || !(sender.containerMenu instanceof MiniAllayMenu menu)) {
				return;
			}
			if (menu.confirm(address)) {
				sender.closeContainer();
			}
		});
	}
}
