package com.yision.allay.config;

public final class AllConfigs {
	private static final AllayServerConfig SERVER = new AllayServerConfig();

	private AllConfigs() {}

	public static AllayServerConfig server() {
		return SERVER;
	}
}
