package com.yision.allay.logistics.courier;

import com.yision.allay.block.allayport.AllayPortBlock;
import com.yision.allay.block.allayport.AllayPortBlockEntity;
import com.yision.allay.entity.courier.AllayCourierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class AllayCourierTask {

	public static final int TELEPORT_AFTER_TICKS = 300;
	public static final int FORCE_ARRIVAL_TICKS = 600;

	private static final int TAKEOFF_PHASE_TICKS = 20;
	private static final double PRECISE_APPROACH_DISTANCE = 4.0;
	private static final float VANILLA_ALLAY_CRUISE_SPEED = 2.25f;
	private static final double VANILLA_ALLAY_APPROACH_SPEED = 1.5;
	private static final double ALLAY_PORT_COMPLETION_DISTANCE = 0.2;
	private static final double ALLAY_PORT_ENTRY_OFFSET = 0.5;
	private static final int ALLAY_PORT_ENTRY_TICKS = 5;
	private static final double PLAYER_COMPLETION_DISTANCE = 1.5;
	private static final double PLAYER_TARGET_HEIGHT = 1.2;
	private static final double PLAYER_FORWARD_OFFSET = 0.15;

	private final UUID id;
	private ItemStack box;
	private ResourceKey<Level> currentDimension;
	private ResourceKey<Level> targetDimension;
	private @Nullable BlockPos sourceAllayPortPos;
	private @Nullable BlockPos targetAllayPortPos;
	private @Nullable UUID targetPlayerId;
	private @Nullable UUID sourcePlayerId;
	private @Nullable ResourceKey<Level> sourceDimension;
	private AllayCourierReturnMode returnMode;
	private AllayCourierEntity.Mission mission;
	private AllayCourierEntity.Phase phase;
	private Vec3 position;
	private Vec3 launchDirection;
	private int phaseTicks;
	private int allayPortEntryTicks = -1;
	private int deliveryElapsedTicks;
	private boolean teleportedNearTarget;
	private boolean relocatedThisTick;
	private boolean removed;

	private AllayCourierTask(
		UUID id, ItemStack box,
		ResourceKey<Level> currentDimension, ResourceKey<Level> targetDimension,
		@Nullable BlockPos sourceAllayPortPos, @Nullable BlockPos targetAllayPortPos,
		@Nullable UUID targetPlayerId,
		@Nullable UUID sourcePlayerId, @Nullable ResourceKey<Level> sourceDimension,
		AllayCourierReturnMode returnMode,
		AllayCourierEntity.Mission mission, Vec3 position, Vec3 launchDirection
	) {
		this.id = id;
		this.box = box.copy();
		this.currentDimension = currentDimension;
		this.targetDimension = targetDimension;
		this.sourceAllayPortPos = sourceAllayPortPos != null ? sourceAllayPortPos.immutable() : null;
		this.targetAllayPortPos = targetAllayPortPos != null ? targetAllayPortPos.immutable() : null;
		this.targetPlayerId = targetPlayerId;
		this.sourcePlayerId = sourcePlayerId;
		this.sourceDimension = sourceDimension;
		this.returnMode = returnMode == null ? defaultReturnMode(sourceAllayPortPos, sourcePlayerId) : returnMode;
		this.mission = mission;
		this.phase = AllayCourierEntity.Phase.TAKEOFF;
		this.position = position;
		this.launchDirection = horizontalDirection(launchDirection);
	}

	public static AllayCourierTask forPackageToAllayPort(
		UUID id, ItemStack box,
		ServerLevel spawnLevel, ResourceKey<Level> targetDimension, BlockPos targetAllayPortPos,
		Vec3 spawnPos, Vec3 launchDirection,
		@Nullable ResourceKey<Level> sourceDimension, @Nullable BlockPos sourceAllayPortPos,
		@Nullable UUID sourcePlayerId,
		AllayCourierReturnMode returnMode
	) {
		return new AllayCourierTask(id, box, spawnLevel.dimension(), targetDimension,
			sourceAllayPortPos, targetAllayPortPos, null,
			sourcePlayerId, sourceDimension, returnMode, AllayCourierEntity.Mission.PACKAGE_TO_ALLAY_PORT,
			spawnPos, launchDirection);
	}

	public static AllayCourierTask forPackageToPlayer(
		UUID id, ItemStack box,
		ServerLevel spawnLevel, UUID targetPlayerId, ResourceKey<Level> targetDimension,
		Vec3 spawnPos, Vec3 launchDirection,
		@Nullable ResourceKey<Level> sourceDimension, @Nullable BlockPos sourceAllayPortPos,
		@Nullable UUID sourcePlayerId,
		AllayCourierReturnMode returnMode
	) {
		return new AllayCourierTask(id, box, spawnLevel.dimension(), targetDimension,
			sourceAllayPortPos, null, targetPlayerId,
			sourcePlayerId, sourceDimension, returnMode, AllayCourierEntity.Mission.PACKAGE_TO_PLAYER,
			spawnPos, launchDirection);
	}

	public static AllayCourierTask forCarrierReturn(
		UUID id, ServerLevel spawnLevel,
		ResourceKey<Level> targetDimension, BlockPos targetAllayPortPos,
		Vec3 spawnPos, Vec3 launchDirection
	) {
		return new AllayCourierTask(id, ItemStack.EMPTY, spawnLevel.dimension(), targetDimension,
			null, targetAllayPortPos, null,
			null, null, AllayCourierReturnMode.DEFAULT_FOR_PORT, AllayCourierEntity.Mission.CARRIER_RETURN,
			spawnPos, launchDirection);
	}

	public static AllayCourierTask forCarrierReturnToPlayer(
		UUID id, ServerLevel spawnLevel, UUID targetPlayerId,
		ResourceKey<Level> targetDimension,
		Vec3 spawnPos, Vec3 launchDirection
	) {
		return new AllayCourierTask(id, ItemStack.EMPTY, spawnLevel.dimension(), targetDimension,
			null, null, targetPlayerId,
			null, null, AllayCourierReturnMode.DEFAULT_FOR_PORT,
			AllayCourierEntity.Mission.CARRIER_RETURN_TO_PLAYER, spawnPos, launchDirection);
	}

	public void tick(MinecraftServer server, @Nullable AllayCourierEntity entity) {
		if (removed) {
			return;
		}
		relocatedThisTick = false;

		ServerLevel currentLevel = server.getLevel(currentDimension);
		if (currentLevel == null) {
			markRemoved();
			return;
		}

		boolean hasActiveEntity = entity != null && entity.isAlive() && entity.level() == currentLevel;
		if (hasActiveEntity) {
			position = entity.position();
		}

		deliveryElapsedTicks++;
		if (deliveryElapsedTicks > FORCE_ARRIVAL_TICKS && !isEnteringAllayPort()) {
			forceArrive(server, currentLevel);
			return;
		}

		if (!teleportedNearTarget && deliveryElapsedTicks >= TELEPORT_AFTER_TICKS) {
			teleportNearTarget(server);
			return;
		}

		ResolvedTarget target = resolveTarget(server);
		if (target == null) {
			doFail(server, currentLevel);
			return;
		}

		if (!target.level.dimension().equals(currentDimension)) {
			tickCrossDimensionExit(entity, hasActiveEntity);
			return;
		}

		tickTowardTarget(server, currentLevel, target, entity, hasActiveEntity);
	}

	private void tickCrossDimensionExit(@Nullable AllayCourierEntity entity, boolean hasActiveEntity) {
		phaseTicks++;
		if (phaseTicks >= TAKEOFF_PHASE_TICKS) {
			phase = AllayCourierEntity.Phase.EXITING_DIMENSION;
		}
		if (hasActiveEntity) {
			Vec3 exitTarget = position.add(launchDirection.scale(24.0)).add(0, 6.0, 0);
			entity.flyDirectlyAsVanillaAllay(exitTarget, VANILLA_ALLAY_CRUISE_SPEED);
		}
	}

	private void tickTowardTarget(MinecraftServer server, ServerLevel currentLevel, ResolvedTarget target,
		@Nullable AllayCourierEntity entity, boolean hasActiveEntity) {
		phaseTicks++;
		Vec3 landingTarget = landingTarget(target.allayPort, target.player);
		double distance = position.distanceTo(landingTarget);
		if (target.allayPort != null && isEnteringAllayPort()) {
			tickAllayPortEntry(server, currentLevel, target.allayPort, entity, hasActiveEntity);
			return;
		}

		if (hasReached(target.allayPort, target.player, landingTarget)) {
			if (target.allayPort != null) {
				beginAllayPortEntry(target.allayPort);
				tickAllayPortEntry(server, currentLevel, target.allayPort, entity, hasActiveEntity);
			} else {
				doFinishDeliveryAt(server, currentLevel);
			}
			return;
		}

		boolean preciseApproach = distance <= PRECISE_APPROACH_DISTANCE;
		if (preciseApproach) {
			if (phase != AllayCourierEntity.Phase.LANDING) {
				phaseTicks = 0;
			}
			phase = AllayCourierEntity.Phase.LANDING;
			setLandingOpen(target.allayPort, true);
		} else if (phase == AllayCourierEntity.Phase.TAKEOFF && phaseTicks >= TAKEOFF_PHASE_TICKS) {
			phase = AllayCourierEntity.Phase.CRUISE;
			phaseTicks = 0;
		}

		if (!hasActiveEntity) {
			return;
		}
		if (preciseApproach) {
			entity.approachPreciselyAsVanillaAllay(landingTarget, VANILLA_ALLAY_APPROACH_SPEED);
		} else {
			entity.flyDirectlyAsVanillaAllay(landingTarget, VANILLA_ALLAY_CRUISE_SPEED);
		}
	}

	private void beginAllayPortEntry(AllayPortBlockEntity allayPort) {
		allayPortEntryTicks = 0;
		teleportedNearTarget = true;
		phase = AllayCourierEntity.Phase.LANDING;
		phaseTicks = 0;
		setLandingOpen(allayPort, true);
	}

	private void tickAllayPortEntry(MinecraftServer server, ServerLevel currentLevel,
		AllayPortBlockEntity allayPort, @Nullable AllayCourierEntity entity, boolean hasActiveEntity) {
		phase = AllayCourierEntity.Phase.LANDING;
		setLandingOpen(allayPort, true);
		if (allayPortEntryTicks >= ALLAY_PORT_ENTRY_TICKS) {
			doFinishDeliveryAt(server, currentLevel);
			return;
		}
		if (hasActiveEntity) {
			entity.approachPreciselyAsVanillaAllay(allayPortCenter(allayPort), VANILLA_ALLAY_APPROACH_SPEED);
		}
		allayPortEntryTicks++;
	}

	private void teleportNearTarget(MinecraftServer server) {
		ResolvedTarget target = resolveTarget(server);
		if (target == null) {
			return;
		}
		if (targetAllayPortPos != null) {
			target.level.getChunkAt(targetAllayPortPos);
		}

		Vec3 landingTarget = landingTarget(target.allayPort, target.player);
		Vec3 waypoint = nearTargetWaypoint(target.allayPort, target.player);
		Vec3 preferredSpawn = computeNearTargetSpawn(landingTarget, waypoint, target.player != null);
		position = findTickingPosTowardTarget(target.level, preferredSpawn, waypoint);
		currentDimension = target.level.dimension();
		if (target.player != null) {
			targetDimension = target.player.serverLevel().dimension();
		}
		phase = AllayCourierEntity.Phase.CRUISE;
		phaseTicks = 0;
		teleportedNearTarget = true;
		relocatedThisTick = true;
	}

	private Vec3 computeNearTargetSpawn(Vec3 landingTarget, Vec3 waypoint, boolean playerTarget) {
		Vec3 away = new Vec3(position.x - landingTarget.x, 0, position.z - landingTarget.z);
		if (away.lengthSqr() < 1.0E-6) {
			away = launchDirection.scale(-1);
		}
		if (away.lengthSqr() < 1.0E-6) {
			away = new Vec3(0, 0, 1);
		}
		away = away.normalize();

		double distance = playerTarget ? 32.0 : 56.0;
		double yOffset = playerTarget ? 4.0 : 12.0;
		return waypoint.add(away.scale(distance)).add(0, yOffset, 0);
	}

	private Vec3 findTickingPosTowardTarget(ServerLevel level, Vec3 preferredSpawn, Vec3 waypoint) {
		Vec3 path = waypoint.subtract(preferredSpawn);
		if (path.lengthSqr() < 1.0E-6) {
			return preferredSpawn;
		}

		Vec3 step = path.normalize().scale(8.0);
		Vec3 candidate = preferredSpawn;
		int iterations = Math.max(1, Mth.ceil(path.length() / 8.0));
		for (int i = 0; i <= iterations; i++) {
			if (level.isPositionEntityTicking(BlockPos.containing(candidate))) {
				return candidate;
			}
			candidate = candidate.add(step);
		}
		return preferredSpawn;
	}

	private void forceArrive(MinecraftServer server, ServerLevel fallbackLevel) {
		ResolvedTarget target = resolveTarget(server);
		if (target == null) {
			doFail(server, fallbackLevel);
			return;
		}
		if (targetAllayPortPos != null) {
			target.level.getChunkAt(targetAllayPortPos);
		}
		position = landingTarget(target.allayPort, target.player);
		currentDimension = target.level.dimension();
		if (target.allayPort != null) {
			beginAllayPortEntry(target.allayPort);
			relocatedThisTick = true;
			return;
		}
		doFinishDeliveryAt(server, target.level);
	}

	private void doFinishDeliveryAt(MinecraftServer server, @Nullable ServerLevel level) {
		if (level == null) {
			markRemoved();
			return;
		}

		ResolvedTarget target = resolveTarget(server);
		Vec3 landingTarget = target != null ? landingTarget(target.allayPort, target.player) : position;
		setLandingOpen(target != null ? target.allayPort : null, false);

		AllayCourierDeliveryService.DeliveryResult result = AllayCourierDeliveryService.finishDelivery(
			server, box, mission, returnMode,
			targetDimension, targetAllayPortPos, targetPlayerId,
			level, position, landingTarget);

		if (result.handled()) {
			AllayCourierDeliveryService.spawnDeliveryParticles(level, position);
			if (result.returnCarrier()) {
				startCarrierReturn(server);
				return;
			}
		}
		markRemoved();
	}

	private void doFail(MinecraftServer server, @Nullable ServerLevel currentLevel) {
		if (currentLevel == null) {
			markRemoved();
			return;
		}
		ResolvedTarget target = resolveTarget(server);
		Vec3 dropTarget = target != null ? landingTarget(target.allayPort, null) : position;
		Vec3 dropPos = target != null && target.allayPort != null ? dropTarget : position;
		setLandingOpen(target != null ? target.allayPort : null, false);
		AllayCourierDeliveryService.failAndDrop(box, mission, currentLevel, dropPos);
		markRemoved();
	}

	private void startCarrierReturn(MinecraftServer server) {
		if (sourceAllayPortPos != null && sourceDimension != null) {
			targetAllayPortPos = sourceAllayPortPos;
			targetDimension = sourceDimension;
			targetPlayerId = null;
			resetForReturn(AllayCourierEntity.Mission.CARRIER_RETURN);
		} else if (sourcePlayerId != null) {
			ServerPlayer sourcePlayer = server.getPlayerList().getPlayer(sourcePlayerId);
			if (sourcePlayer != null && sourcePlayer.isAlive()) {
				targetAllayPortPos = null;
				targetPlayerId = sourcePlayerId;
				targetDimension = sourcePlayer.serverLevel().dimension();
				resetForReturn(AllayCourierEntity.Mission.CARRIER_RETURN_TO_PLAYER);
			} else {
				AllayCourierDeliveryService.dropCarrierOnly(server.getLevel(currentDimension), position);
				markRemoved();
			}
		} else {
			AllayCourierDeliveryService.dropCarrierOnly(server.getLevel(currentDimension), position);
			markRemoved();
		}
	}

	private void resetForReturn(AllayCourierEntity.Mission nextMission) {
		box = ItemStack.EMPTY;
		mission = nextMission;
		phase = AllayCourierEntity.Phase.TAKEOFF;
		phaseTicks = 0;
		allayPortEntryTicks = -1;
		deliveryElapsedTicks = 0;
		teleportedNearTarget = false;
	}

	private @Nullable ServerLevel resolveTargetLevel(MinecraftServer server) {
		if (targetAllayPortPos != null) {
			return server.getLevel(targetDimension);
		}
		ServerPlayer player = resolveTargetPlayer(server);
		return player != null ? player.serverLevel() : server.getLevel(targetDimension);
	}

	private @Nullable AllayPortBlockEntity resolveTargetAllayPort(@Nullable ServerLevel level) {
		return AllayCourierDeliveryService.resolveTargetAllayPort(level, targetAllayPortPos);
	}

	private @Nullable ServerPlayer resolveTargetPlayer(MinecraftServer server) {
		return AllayCourierDeliveryService.resolvePlayer(server, targetPlayerId);
	}

	private Vec3 landingTarget(@Nullable AllayPortBlockEntity allayPort, @Nullable ServerPlayer player) {
		if (allayPort != null) {
			Direction facing = allayPort.getBlockState().getValue(AllayPortBlock.FACING);
			return allayPortCenter(allayPort)
				.add(Vec3.atLowerCornerOf(facing.getNormal()).scale(ALLAY_PORT_ENTRY_OFFSET));
		}
		return player != null ? playerDeliveryTarget(player) : position;
	}

	private Vec3 allayPortCenter(AllayPortBlockEntity allayPort) {
		return Vec3.atCenterOf(allayPort.getBlockPos());
	}

	private Vec3 nearTargetWaypoint(@Nullable AllayPortBlockEntity allayPort, @Nullable ServerPlayer player) {
		if (allayPort != null) {
			return Vec3.atCenterOf(allayPort.getBlockPos()).add(0, 4.0, 0);
		}
		return player != null ? playerDeliveryTarget(player).add(0, 0.6, 0) : position;
	}

	private Vec3 playerDeliveryTarget(ServerPlayer player) {
		Vec3 horizontalLook = player.getLookAngle().multiply(1, 0, 1);
		if (horizontalLook.lengthSqr() > 1.0E-6) {
			horizontalLook = horizontalLook.normalize();
		} else {
			float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
			horizontalLook = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
		}
		return player.position().add(0, PLAYER_TARGET_HEIGHT, 0)
			.add(horizontalLook.scale(PLAYER_FORWARD_OFFSET));
	}

	private boolean hasReached(@Nullable AllayPortBlockEntity allayPort, @Nullable ServerPlayer player,
		Vec3 landingTarget) {
		if (allayPort != null) {
			return position.distanceTo(landingTarget) <= ALLAY_PORT_COMPLETION_DISTANCE;
		}
		return player != null && (player.getBoundingBox().inflate(0.45, 0.6, 0.45).contains(position)
			|| position.distanceTo(landingTarget) <= PLAYER_COMPLETION_DISTANCE);
	}

	private boolean isEnteringAllayPort() {
		return allayPortEntryTicks >= 0;
	}

	private void setLandingOpen(@Nullable AllayPortBlockEntity allayPort, boolean open) {
		if (allayPort != null) {
			allayPort.setCourierLandingOpen(id, open);
		}
	}

	private @Nullable ResolvedTarget resolveTarget(MinecraftServer server) {
		ServerLevel level = resolveTargetLevel(server);
		if (level == null) {
			return null;
		}
		AllayPortBlockEntity allayPort = resolveTargetAllayPort(level);
		ServerPlayer player = allayPort == null ? resolveTargetPlayer(server) : null;
		return allayPort == null && player == null ? null : new ResolvedTarget(level, allayPort, player);
	}

	private static Vec3 horizontalDirection(Vec3 direction) {
		Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
		return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : horizontal.normalize();
	}

	private static AllayCourierReturnMode defaultReturnMode(@Nullable BlockPos sourceAllayPortPos,
		@Nullable UUID sourcePlayerId) {
		return sourcePlayerId != null && sourceAllayPortPos == null
			? AllayCourierReturnMode.DEFAULT_FOR_PLAYER_LAUNCH
			: AllayCourierReturnMode.DEFAULT_FOR_PORT;
	}

	private record ResolvedTarget(
		ServerLevel level,
		@Nullable AllayPortBlockEntity allayPort,
		@Nullable ServerPlayer player
	) {}

	public UUID id() { return id; }
	public ItemStack box() { return box; }
	public ResourceKey<Level> currentDimension() { return currentDimension; }
	public AllayCourierEntity.Mission mission() { return mission; }
	public AllayCourierEntity.Phase phase() { return phase; }
	public Vec3 position() { return position; }
	public Vec3 launchDirection() { return launchDirection; }
	public boolean relocatedThisTick() { return relocatedThisTick; }
	public boolean isRemoved() { return removed; }
	public void markRemoved() { removed = true; }

	public CompoundTag save(CompoundTag tag) {
		tag.putUUID("Id", id);
		tag.put("Box", box.save(new CompoundTag()));
		tag.putString("CurrentDimension", currentDimension.location().toString());
		tag.putString("TargetDimension", targetDimension.location().toString());
		if (sourceDimension != null) tag.putString("SourceDimension", sourceDimension.location().toString());
		if (sourceAllayPortPos != null) tag.put("SourceAllayPortPos", NbtUtils.writeBlockPos(sourceAllayPortPos));
		if (targetAllayPortPos != null) tag.put("TargetAllayPortPos", NbtUtils.writeBlockPos(targetAllayPortPos));
		if (targetPlayerId != null) tag.putUUID("TargetPlayer", targetPlayerId);
		if (sourcePlayerId != null) tag.putUUID("SourcePlayer", sourcePlayerId);
		tag.putString("ReturnMode", returnMode.serializedName());
		tag.putByte("Mission", (byte) mission.ordinal());
		tag.putByte("Phase", (byte) phase.ordinal());
		tag.put("Position", vecToTag(position));
		tag.put("LaunchDirection", vecToTag(launchDirection));
		tag.putInt("PhaseTicks", phaseTicks);
		tag.putInt("AllayPortEntryTicks", allayPortEntryTicks);
		tag.putInt("DeliveryElapsedTicks", deliveryElapsedTicks);
		tag.putBoolean("TeleportedNearTarget", teleportedNearTarget);
		return tag;
	}

	public static AllayCourierTask load(CompoundTag tag) {
		UUID id = tag.getUUID("Id");
		ItemStack box = ItemStack.of(tag.getCompound("Box"));
		ResourceKey<Level> currentDimension = dimensionKey(tag.getString("CurrentDimension"));
		ResourceKey<Level> targetDimension = dimensionKey(tag.getString("TargetDimension"));
		ResourceKey<Level> sourceDimension = tag.contains("SourceDimension")
			? dimensionKey(tag.getString("SourceDimension")) : null;
		BlockPos sourceAllayPort = tag.contains("SourceAllayPortPos")
			? NbtUtils.readBlockPos(tag.getCompound("SourceAllayPortPos")) : null;
		BlockPos targetAllayPort = tag.contains("TargetAllayPortPos")
			? NbtUtils.readBlockPos(tag.getCompound("TargetAllayPortPos")) : null;
		UUID targetPlayer = tag.hasUUID("TargetPlayer") ? tag.getUUID("TargetPlayer") : null;
		UUID sourcePlayer = tag.hasUUID("SourcePlayer") ? tag.getUUID("SourcePlayer") : null;
		AllayCourierReturnMode returnMode = tag.contains("ReturnMode")
			? AllayCourierReturnMode.byName(tag.getString("ReturnMode"))
			: defaultReturnMode(sourceAllayPort, sourcePlayer);
		AllayCourierEntity.Mission mission = AllayCourierEntity.Mission.values()[tag.getByte("Mission")];
		AllayCourierEntity.Phase phase = AllayCourierEntity.Phase.values()[tag.getByte("Phase")];

		AllayCourierTask task = new AllayCourierTask(id, box, currentDimension, targetDimension,
			sourceAllayPort, targetAllayPort, targetPlayer,
			sourcePlayer, sourceDimension, returnMode, mission,
			vecFromTag(tag, "Position"), vecFromTag(tag, "LaunchDirection"));
		task.phase = phase;
		task.phaseTicks = tag.getInt("PhaseTicks");
		task.allayPortEntryTicks = tag.contains("AllayPortEntryTicks")
			? tag.getInt("AllayPortEntryTicks") : -1;
		task.deliveryElapsedTicks = tag.getInt("DeliveryElapsedTicks");
		task.teleportedNearTarget = tag.getBoolean("TeleportedNearTarget");
		return task;
	}

	private static ResourceKey<Level> dimensionKey(String id) {
		return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(id));
	}

	private static CompoundTag vecToTag(Vec3 vector) {
		CompoundTag tag = new CompoundTag();
		tag.putDouble("X", vector.x);
		tag.putDouble("Y", vector.y);
		tag.putDouble("Z", vector.z);
		return tag;
	}

	private static Vec3 vecFromTag(CompoundTag tag, String key) {
		CompoundTag vector = tag.getCompound(key);
		return new Vec3(vector.getDouble("X"), vector.getDouble("Y"), vector.getDouble("Z"));
	}
}
