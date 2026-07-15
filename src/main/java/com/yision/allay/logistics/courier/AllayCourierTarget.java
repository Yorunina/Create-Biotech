package com.yision.allay.logistics.courier;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public sealed interface AllayCourierTarget permits AllayCourierTarget.PlayerTarget, AllayCourierTarget.AllayPortTarget {
	ResourceKey<Level> dimension();

	record PlayerTarget(UUID playerId, ResourceKey<Level> dimension) implements AllayCourierTarget {}
	record AllayPortTarget(ResourceKey<Level> dimension, BlockPos pos) implements AllayCourierTarget {}
}
