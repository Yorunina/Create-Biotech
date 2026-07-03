package com.yision.phantom.config;

public final class AllConfigs {
	private static final CPClient CLIENT = new CPClient();
	private static final CPServer SERVER = new CPServer();

	private AllConfigs() {}

	public static CPClient client() {
		return CLIENT;
	}

	public static CPServer server() {
		return SERVER;
	}
}
