package com.yision.phantom.config;

public class CPClient {
	public final EnumValue<AirCourierHudPlacement> courierHudPlacement =
		new EnumValue<>(AirCourierHudPlacement.TOP_RIGHT);
	public final FloatValue courierHudScale = new FloatValue(0.65f);

	public enum AirCourierHudPlacement {
		TOP_RIGHT,
		TOP_LEFT,
		HIDDEN
	}

	public record EnumValue<T extends Enum<T>>(T value) {
		public T get() {
			return value;
		}
	}

	public record FloatValue(float value) {
		public float getF() {
			return value;
		}
	}
}
