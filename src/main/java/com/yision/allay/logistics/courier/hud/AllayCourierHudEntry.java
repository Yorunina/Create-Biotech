package com.yision.allay.logistics.courier.hud;

import java.util.Objects;
import java.util.UUID;

public record AllayCourierHudEntry(
	UUID id,
	boolean incoming,
	String address,
	int etaSeconds
) {
	public static final int MAX_ADDRESS_LENGTH = 64;

	public AllayCourierHudEntry {
		Objects.requireNonNull(id, "id");
		String normalizedAddress = address == null ? "" : address.trim();
		address = normalizedAddress.length() > MAX_ADDRESS_LENGTH
			? normalizedAddress.substring(0, MAX_ADDRESS_LENGTH)
			: normalizedAddress;
		etaSeconds = Math.max(0, etaSeconds);
	}
}
