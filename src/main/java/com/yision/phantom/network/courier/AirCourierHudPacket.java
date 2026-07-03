package com.yision.phantom.network.courier;

import com.yision.phantom.client.gui.hud.AirCourierHudOverlay;
import com.yision.phantom.logistics.courier.hud.AirCourierHudEntry;
import com.yision.phantom.logistics.courier.hud.AirCourierHudPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class AirCourierHudPacket {
	private final AirCourierHudPayload payload;

	public AirCourierHudPacket(FriendlyByteBuf buffer) {
		int count = buffer.readVarInt();
		List<AirCourierHudEntry> entries = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			entries.add(AirCourierHudEntry.read(buffer));
		}
		this.payload = new AirCourierHudPayload(entries);
	}

	private AirCourierHudPacket(AirCourierHudPayload payload) {
		this.payload = payload;
	}

	public static AirCourierHudPacket of(AirCourierHudPayload payload) {
		return new AirCourierHudPacket(payload);
	}

	public static AirCourierHudPacket hidden() {
		return new AirCourierHudPacket(AirCourierHudPayload.hidden());
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(payload.entries().size());
		for (AirCourierHudEntry entry : payload.entries()) {
			AirCourierHudEntry.write(buffer, entry);
		}
	}

	public void handle(Context context) {
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> AirCourierHudOverlay.updateState(payload)));
	}
}
