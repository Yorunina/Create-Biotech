package com.yision.phantom.config;

public final class AllConfigs {
	private static final CPServer SERVER = new CPServer();

	private AllConfigs() {}

	public static CPServer server() {
		return SERVER;
	}
}
