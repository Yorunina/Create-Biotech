package com.yision.phantom.item.miniphantom;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record MiniPhantomReturnTarget(ResourceKey<Level> dimension, BlockPos pos) {
	private static final String DIMENSION_KEY = "Dimension";
	private static final String POS_KEY = "Pos";

	public CompoundTag write() {
		CompoundTag tag = new CompoundTag();
		tag.putString(DIMENSION_KEY, dimension.location().toString());
		tag.putLong(POS_KEY, pos.asLong());
		return tag;
	}

	public static Optional<MiniPhantomReturnTarget> read(CompoundTag tag) {
		ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(DIMENSION_KEY));
		if (dimensionId == null || !tag.contains(POS_KEY)) {
			return Optional.empty();
		}
		return Optional.of(new MiniPhantomReturnTarget(
			ResourceKey.create(Registries.DIMENSION, dimensionId),
			BlockPos.of(tag.getLong(POS_KEY))));
	}
}
