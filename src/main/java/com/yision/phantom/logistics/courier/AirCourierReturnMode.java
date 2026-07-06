package com.yision.phantom.logistics.courier;

import org.jetbrains.annotations.Nullable;

public enum AirCourierReturnMode {
	ALWAYS_DOCK(0, "always_dock"),
	ALWAYS_RETURN(1, "always_return"),
	RETURN_WHEN_UNABLE(2, "return_when_unable");

	public static final AirCourierReturnMode DEFAULT_FOR_PORT = ALWAYS_RETURN;
	public static final AirCourierReturnMode DEFAULT_FOR_PLAYER_LAUNCH = ALWAYS_DOCK;

	private final int id;
	private final String serializedName;

	AirCourierReturnMode(int id, String serializedName) {
		this.id = id;
		this.serializedName = serializedName;
	}

	public int id() {
		return id;
	}

	public String serializedName() {
		return serializedName;
	}

	public static AirCourierReturnMode byId(int id) {
		for (AirCourierReturnMode mode : values()) {
			if (mode.id == id) {
				return mode;
			}
		}
		return DEFAULT_FOR_PORT;
	}

	public static AirCourierReturnMode byName(@Nullable String name) {
		if (name == null) {
			return DEFAULT_FOR_PORT;
		}
		for (AirCourierReturnMode mode : values()) {
			if (mode.serializedName.equals(name)) {
				return mode;
			}
		}
		return DEFAULT_FOR_PORT;
	}
}
