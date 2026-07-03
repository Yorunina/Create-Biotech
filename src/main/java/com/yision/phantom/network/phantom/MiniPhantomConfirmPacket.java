package com.yision.phantom.network.phantom;

import com.yision.phantom.item.miniphantom.MiniPhantomMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class MiniPhantomConfirmPacket {
	private final String address;

	public MiniPhantomConfirmPacket(String address) {
		this.address = address == null ? "" : address;
	}

	public MiniPhantomConfirmPacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf());
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(address);
	}

	public void handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer sender = context.getSender();
			if (sender == null || !(sender.containerMenu instanceof MiniPhantomMenu menu)) {
				return;
			}
			if (menu.confirm(address)) {
				sender.closeContainer();
			}
		});
	}
}
