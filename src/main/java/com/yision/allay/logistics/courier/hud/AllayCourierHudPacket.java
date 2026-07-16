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

	private final List<Integer> etaSeconds;

	public AllayCourierHudPacket(List<Integer> etaSeconds) {
		this.etaSeconds = etaSeconds == null ? List.of() : etaSeconds.stream()
			.limit(MAX_VISIBLE_ENTRIES)
			.map(seconds -> Math.max(0, seconds))
			.toList();
	}

	public AllayCourierHudPacket(FriendlyByteBuf buffer) {
		int count = buffer.readVarInt();
		List<Integer> decoded = new ArrayList<>(Math.max(0, Math.min(count, MAX_VISIBLE_ENTRIES)));
		for (int i = 0; i < count; i++) {
			int seconds = buffer.readVarInt();
			if (i < MAX_VISIBLE_ENTRIES) {
				decoded.add(Math.max(0, seconds));
			}
		}
		etaSeconds = List.copyOf(decoded);
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(etaSeconds.size());
		etaSeconds.forEach(buffer::writeVarInt);
	}

	public void handle(Context context) {
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> AllayCourierHudOverlay.update(etaSeconds)));
	}
}
