package com.nobodiiiii.createbiotech.foundation.utility;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Same-world fallback for the 1.21 Sable sub-level bridge.
 *
 * <p>Sable has no 1.20.1 release, so positions in this backport always belong to the owning
 * vanilla level. Unsupported sub-level addresses fail closed.</p>
 */
public final class SubLevelCompat {
	private SubLevelCompat() {}

	public static Level getContaining(Level level, BlockPos pos) {
		return level;
	}

	@Nullable
	public static UUID getSpaceId(Level level, BlockPos pos) {
		return null;
	}

	public static boolean isValidSpacePosition(Level level, BlockPos pos) {
		return true;
	}

	public static boolean matchesSpace(Level level, BlockPos pos, @Nullable UUID expectedSubLevelId) {
		return expectedSubLevelId == null;
	}

	@Nullable
	public static BlockPos resolveRawPosition(Level level, BlockPos rawPos, @Nullable UUID subLevelId) {
		return subLevelId == null ? rawPos : null;
	}

	@Nullable
	public static BlockEntity resolveBlockEntityFast(Level level, BlockPos rawPos, @Nullable UUID subLevelId) {
		return subLevelId == null ? getLoadedBlockEntity(level, rawPos) : null;
	}

	@Nullable
	public static BlockEntity getLoadedBlockEntity(Level level, BlockPos pos) {
		return level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
	}

	public static boolean sameSpace(Level level, BlockPos first, BlockPos second) {
		return true;
	}

	public static boolean sameSpace(@Nullable Level first, @Nullable Level second) {
		return first == second;
	}

	public static Vec3 toWorld(Level level, BlockPos spaceAnchor, Position localPos) {
		return asVec3(localPos);
	}

	public static Vec3 toRenderWorld(@Nullable Level space, Position localPos, float partialTick) {
		return asVec3(localPos);
	}

	public static Vec3 toRenderLocal(@Nullable Level space, Position worldPos, float partialTick) {
		return asVec3(worldPos);
	}

	public static Vec3 localNormalToWorld(Level level, BlockPos spaceAnchor, Vec3 localNormal) {
		return localNormal;
	}

	public static Vec3 renderNormalToWorld(@Nullable Level space, Vec3 localNormal, float partialTick) {
		return localNormal;
	}

	public static Vec3 getWorldVelocity(Level level, BlockPos spaceAnchor, Position localPos) {
		return Vec3.ZERO;
	}

	public static double distanceSquared(Level level, Position first, Position second) {
		double x = first.x() - second.x();
		double y = first.y() - second.y();
		double z = first.z() - second.z();
		return x * x + y * y + z * z;
	}

	private static Vec3 asVec3(Position position) {
		return position instanceof Vec3 vec ? vec : new Vec3(position.x(), position.y(), position.z());
	}
}
