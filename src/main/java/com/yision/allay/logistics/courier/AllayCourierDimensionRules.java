package com.yision.allay.logistics.courier;

import com.yision.allay.config.AllConfigs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class AllayCourierDimensionRules {
	private AllayCourierDimensionRules() {}

	public static boolean allowCrossDimensionDelivery() {
		return AllConfigs.server().allowCrossDimensionDelivery.get();
	}

	public static boolean canTarget(ServerLevel originLevel, ResourceKey<Level> targetDimension) {
		return allowCrossDimensionDelivery() || originLevel.dimension().equals(targetDimension);
	}
}
