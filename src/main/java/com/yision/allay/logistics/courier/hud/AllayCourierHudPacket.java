package com.yision.allay.logistics.courier.hud;

import com.yision.allay.client.gui.hud.AllayCourierHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.ArrayList;
import java.util.List;

public class AllayCourierHudPacket {
	public static final int MAX_VISIBLE_ENTRIES = 3;

	private final List<AllayCourierHudEntry> entries;

	public AllayCourierHudPacket(List<AllayCourierHudEntry> entries) {
		this.entries = entries == null ? List.of() : entries.stream()
			.limit(MAX_VISIBLE_ENTRIES)
			.toList();
	}

	public AllayCourierHudPacket(FriendlyByteBuf buffer) {
		int count = buffer.readVarInt();
		List<AllayCourierHudEntry> decoded =
			new ArrayList<>(Math.max(0, Math.min(count, MAX_VISIBLE_ENTRIES)));
		for (int i = 0; i < count; i++) {
			java.util.UUID id = buffer.readUUID();
			boolean incoming = buffer.readBoolean();
			String address = buffer.readUtf(AllayCourierHudEntry.MAX_ADDRESS_LENGTH);
			int seconds = buffer.readVarInt();
			if (i < MAX_VISIBLE_ENTRIES) {
				decoded.add(new AllayCourierHudEntry(id, incoming, address, seconds));
			}
		}
		entries = List.copyOf(decoded);
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(entries.size());
		for (AllayCourierHudEntry entry : entries) {
			buffer.writeUUID(entry.id());
			buffer.writeBoolean(entry.incoming());
			buffer.writeUtf(entry.address(), AllayCourierHudEntry.MAX_ADDRESS_LENGTH);
			buffer.writeVarInt(entry.etaSeconds());
		}
	}

	public void handle(Context context) {
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> AllayCourierHudOverlay.update(entries)));
	}
}
