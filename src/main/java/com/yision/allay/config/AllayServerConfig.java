package com.yision.allay.config;

public class AllayServerConfig {
	public final BoolValue allowCrossDimensionDelivery = new BoolValue(true);

	public record BoolValue(boolean value) {
		public boolean get() {
			return value;
		}
	}
}
