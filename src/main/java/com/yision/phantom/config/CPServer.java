package com.yision.phantom.config;

public class CPServer {
	public final BoolValue allowCrossDimensionDelivery = new BoolValue(true);

	public record BoolValue(boolean value) {
		public boolean get() {
			return value;
		}
	}
}
