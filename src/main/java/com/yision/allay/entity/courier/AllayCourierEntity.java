package com.yision.allay.entity.courier;

import com.nobodiiiii.createbiotech.content.cardboardbox.CapturedEntityBoxHelper;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.yision.allay.item.miniallay.MiniAllayItem;
import com.yision.allay.logistics.courier.AllayCourierDispatchService;
import com.yision.allay.logistics.courier.AllayCourierReturnMode;
import com.yision.allay.logistics.courier.AllayCourierTarget;
import com.yision.allay.logistics.courier.AllayCourierTask;
import com.yision.allay.logistics.courier.AllayCourierTaskManager;
import com.yision.allay.registry.AllEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A cargo-carrying Allay. Movement, path finding, steering and animation all come from vanilla
 * {@link Allay}; this class only adds the courier task metadata and placed-item interactions.
 */
public class AllayCourierEntity extends Allay implements Container {

	private static final EntityDataAccessor<ItemStack> DATA_PACKAGE =
		SynchedEntityData.defineId(AllayCourierEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Byte> DATA_PHASE =
		SynchedEntityData.defineId(AllayCourierEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Vector3f> DATA_LAUNCH_DIRECTION =
		SynchedEntityData.defineId(AllayCourierEntity.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Byte> DATA_MISSION =
		SynchedEntityData.defineId(AllayCourierEntity.class, EntityDataSerializers.BYTE);

	private static final int NAVIGATION_REFRESH_TICKS = 20;
	private static final double NAVIGATION_TARGET_REFRESH_DISTANCE_SQR = 4.0;

	private Vec3 launchDirection = new Vec3(0, 0, 1);
	private @Nullable Vec3 lastNavigationTarget;
	private int lastNavigationCommandTick = Integer.MIN_VALUE;
	private @Nullable Vec3 preciseFlightTarget;
	private double preciseFlightSpeed = 1.0;

	public AllayCourierEntity(EntityType<? extends AllayCourierEntity> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public static AllayCourierEntity createWaiting(Level level, ItemStack box, Vec3 launchDirection) {
		AllayCourierEntity courier = new AllayCourierEntity(AllEntityTypes.ALLAY_COURIER.get(), level);
		courier.setPackage(box);
		courier.setPhase(Phase.WAITING);
		courier.setDeltaMovement(Vec3.ZERO);
		courier.alignToDirection(launchDirection);
		return courier;
	}

	public static AllayCourierEntity createEmpty(EntityType<? extends AllayCourierEntity> type, Level level) {
		return new AllayCourierEntity(type, level);
	}

	public static AllayCourierEntity createFromTask(ServerLevel level, AllayCourierTask task) {
		AllayCourierEntity courier = new AllayCourierEntity(AllEntityTypes.ALLAY_COURIER.get(), level);
		courier.setPackage(task.box());
		courier.setMission(task.mission());
		courier.setPhase(task.phase());
		courier.setPos(task.position());
		courier.setDeltaMovement(Vec3.ZERO);
		courier.setLaunchDirection(task.launchDirection());
		courier.hasImpulse = true;
		courier.hurtMarked = true;
		return courier;
	}

	/**
	 * Feeds a destination into the vanilla Allay brain. The vanilla MoveToTargetSink creates and
	 * follows a FlyingPathNavigation path from this memory.
	 */
	public void navigateAsVanillaAllay(Vec3 target, float speedModifier) {
		preciseFlightTarget = null;
		boolean targetMoved = lastNavigationTarget == null
			|| lastNavigationTarget.distanceToSqr(target) > NAVIGATION_TARGET_REFRESH_DISTANCE_SQR;
		boolean refreshDue = tickCount - lastNavigationCommandTick >= NAVIGATION_REFRESH_TICKS;
		boolean missingTarget = !getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
		if (!targetMoved && !refreshDue && !missingTarget && !getNavigation().isDone()) {
			return;
		}

		BlockPosTracker tracker = new BlockPosTracker(target);
		getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(tracker, speedModifier, 0));
		getBrain().setMemory(MemoryModuleType.LOOK_TARGET, tracker);
		lastNavigationTarget = target;
		lastNavigationCommandTick = tickCount;
	}

	/**
	 * Uses the same vanilla FlyingMoveControl as an ordinary Allay for the final sub-block approach.
	 */
	public void approachPreciselyAsVanillaAllay(Vec3 target, double speedModifier) {
		preciseFlightTarget = target;
		preciseFlightSpeed = speedModifier;
		lastNavigationTarget = target;
		lastNavigationCommandTick = tickCount;
	}

	public void clearCourierDestination() {
		preciseFlightTarget = null;
		lastNavigationTarget = null;
		getNavigation().stop();
		getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		getBrain().eraseMemory(MemoryModuleType.PATH);
	}

	@Override
	protected void customServerAiStep() {
		if (getPhase() == Phase.WAITING) {
			clearCourierDestination();
			setSpeed(0);
			setYya(0);
			setZza(0);
			setDeltaMovement(Vec3.ZERO);
			setNoGravity(true);
			return;
		}

		if (preciseFlightTarget != null) {
			getNavigation().stop();
			getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			getBrain().eraseMemory(MemoryModuleType.PATH);
		}
		super.customServerAiStep();
		if (preciseFlightTarget != null) {
			getNavigation().stop();
			getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			getBrain().eraseMemory(MemoryModuleType.PATH);
			getMoveControl().setWantedPosition(
				preciseFlightTarget.x, preciseFlightTarget.y, preciseFlightTarget.z, preciseFlightSpeed);
		}
	}

	@Override
	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	public boolean canTakeItem(ItemStack stack) {
		return false;
	}

	@Override
	public boolean wantsToPickUp(ItemStack stack) {
		return false;
	}

	public ItemStack getPackage() {
		return getEntityData().get(DATA_PACKAGE);
	}

	public void setPackage(ItemStack box) {
		ItemStack cargo = PackageItem.isPackage(box) ? box.copy() : ItemStack.EMPTY;
		getEntityData().set(DATA_PACKAGE, cargo);
		setItemInHand(InteractionHand.MAIN_HAND, cargo.copy());
	}

	public Phase getPhase() {
		return Phase.byId(getEntityData().get(DATA_PHASE));
	}

	public void setPhase(Phase phase) {
		getEntityData().set(DATA_PHASE, (byte) phase.id);
	}

	public Mission getMission() {
		return Mission.byId(getEntityData().get(DATA_MISSION));
	}

	public void setMission(Mission mission) {
		getEntityData().set(DATA_MISSION, (byte) mission.id);
	}

	private void setLaunchDirection(Vec3 direction) {
		launchDirection = horizontalDirection(direction);
		getEntityData().set(DATA_LAUNCH_DIRECTION,
			new Vector3f((float) launchDirection.x, 0, (float) launchDirection.z));
	}

	private void alignToDirection(Vec3 direction) {
		setLaunchDirection(direction);
		float yRot = (float) (Mth.atan2(launchDirection.x, launchDirection.z) * Mth.RAD_TO_DEG);
		setYRot(yRot);
		yRotO = yRot;
		setXRot(0);
		xRotO = 0;
	}

	private static Vec3 horizontalDirection(Vec3 direction) {
		Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
		return horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : horizontal.normalize();
	}

	private boolean exposesPackageContents() {
		return getPhase() == Phase.WAITING && PackageItem.isPackage(getPackage());
	}

	@Override
	public int getContainerSize() {
		return PackageItem.SLOTS;
	}

	@Override
	public boolean isEmpty() {
		if (!exposesPackageContents()) {
			return true;
		}
		ItemStackHandler contents = CapturedEntityBoxHelper.getVisiblePackageContents(getPackage());
		for (int slot = 0; slot < contents.getSlots(); slot++) {
			if (!contents.getStackInSlot(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		if (!exposesPackageContents() || slot < 0 || slot >= PackageItem.SLOTS) {
			return ItemStack.EMPTY;
		}
		return CapturedEntityBoxHelper.getVisiblePackageContents(getPackage()).getStackInSlot(slot).copy();
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {}

	@Override
	public void setChanged() {}

	@Override
	public boolean stillValid(Player player) {
		return isAlive() && exposesPackageContents() && player.distanceToSqr(this) <= 64.0;
	}

	@Override
	public void clearContent() {}

	@Override
	public ItemStack getPickedResult(HitResult target) {
		if (getPhase() != Phase.WAITING) {
			return ItemStack.EMPTY;
		}
		ItemStack picked = MiniAllayItem.createLoaded(getPackage());
		MiniAllayItem.setHeadingAngle(picked, Math.round(getYRot()));
		return picked;
	}

	@Override
	public boolean hurt(net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
		if (getPhase() != Phase.WAITING) {
			return false;
		}
		if (level().isClientSide()) {
			return true;
		}
		if (!(damageSource.getEntity() instanceof Player player)) {
			return false;
		}

		ItemStack droppedStack = MiniAllayItem.createLoaded(getPackage());
		MiniAllayItem.setHeadingAngle(droppedStack, Math.round(getYRot()));
		ItemEntity itemEntity = new ItemEntity(level(), getX(), getY(), getZ(), droppedStack);
		Vec3 popMotion = player.getLookAngle().multiply(1, 0, 1);
		popMotion = popMotion.lengthSqr() > 1.0E-6 ? popMotion.normalize().scale(0.12) : Vec3.ZERO;
		itemEntity.setDeltaMovement(popMotion.add(0, 0.08, 0));
		level().addFreshEntity(itemEntity);
		level().playSound(null, blockPosition(), SoundEvents.ITEM_FRAME_BREAK, SoundSource.PLAYERS, 0.7f, 1.0f);
		discard();
		return false;
	}

	@Override
	public boolean isPickable() {
		return getPhase() == Phase.WAITING;
	}

	@Override
	public boolean canBeCollidedWith() {
		return getPhase() == Phase.WAITING;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (getPhase() != Phase.WAITING) {
			return InteractionResult.PASS;
		}

		ItemStack heldItem = player.getItemInHand(hand);
		if (heldItem.is(Items.FIREWORK_ROCKET)) {
			if (level().isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (!(level() instanceof ServerLevel serverLevel)) {
				return InteractionResult.CONSUME;
			}

			AllayCourierTarget target = AllayCourierDispatchService.resolvePackageTarget(
				serverLevel, getPackage(), position(), null, null);
			if (target == null) {
				player.displayClientMessage(
					Component.translatable("gui.create_biotech.mini_allay.invalid_target")
						.withStyle(ChatFormatting.RED), true);
				return InteractionResult.CONSUME;
			}

			Vec3 launchDir = horizontalDirection(launchDirection);
			Vec3 spawnPos = position().add(0, 0.01, 0);
			java.util.UUID newTaskId = java.util.UUID.randomUUID();
			AllayCourierTask task;

			if (target instanceof AllayCourierTarget.AllayPortTarget allayPortTarget) {
				task = AllayCourierTask.forPackageToAllayPort(newTaskId, getPackage(), serverLevel,
					allayPortTarget.dimension(), allayPortTarget.pos(), spawnPos, launchDir,
					null, null, player instanceof ServerPlayer sp ? sp.getUUID() : null,
					AllayCourierReturnMode.DEFAULT_FOR_PLAYER_LAUNCH);
			} else if (target instanceof AllayCourierTarget.PlayerTarget playerTarget) {
				ServerPlayer targetPlayer = serverLevel.getServer().getPlayerList().getPlayer(playerTarget.playerId());
				if (targetPlayer == null) {
					player.displayClientMessage(
						Component.translatable("gui.create_biotech.mini_allay.invalid_target")
							.withStyle(ChatFormatting.RED), true);
					return InteractionResult.CONSUME;
				}
				task = AllayCourierTask.forPackageToPlayer(newTaskId, getPackage(), serverLevel,
					playerTarget.playerId(), playerTarget.dimension(), spawnPos, launchDir,
					null, null, player instanceof ServerPlayer sp ? sp.getUUID() : null,
					AllayCourierReturnMode.DEFAULT_FOR_PLAYER_LAUNCH);
			} else {
				return InteractionResult.CONSUME;
			}

			AllayCourierTaskManager.addTask(serverLevel.getServer(), task);
			level().playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
				SoundSource.PLAYERS, 0.8f, 1.0f);
			if (!player.getAbilities().instabuild) {
				heldItem.shrink(1);
			}
			discard();
			return InteractionResult.SUCCESS;
		}

		if (!heldItem.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		ItemStack pickedUp = MiniAllayItem.createLoaded(getPackage());
		MiniAllayItem.setHeadingAngle(pickedUp, Math.round(getYRot()));
		player.setItemInHand(hand, pickedUp);
		level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f,
			0.75f + level().random.nextFloat());
		discard();
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean shouldBeSaved() {
		return getPhase() == Phase.WAITING;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_PACKAGE, ItemStack.EMPTY);
		entityData.define(DATA_PHASE, (byte) Phase.WAITING.id);
		entityData.define(DATA_LAUNCH_DIRECTION, new Vector3f(0, 0, 1));
		entityData.define(DATA_MISSION, (byte) Mission.PACKAGE_TO_PLAYER.id);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setPackage(ItemStack.of(tag.getCompound("Package")));
		if (tag.contains("LaunchDirection")) {
			CompoundTag direction = tag.getCompound("LaunchDirection");
			setLaunchDirection(new Vec3(direction.getDouble("X"), 0, direction.getDouble("Z")));
		}
		setPhase(Phase.byId(tag.getByte("Phase")));
		if (tag.contains("Mission")) {
			setMission(Mission.byId(tag.getByte("Mission")));
		}
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.put("Package", getPackage().save(new CompoundTag()));
		CompoundTag direction = new CompoundTag();
		direction.putDouble("X", launchDirection.x);
		direction.putDouble("Z", launchDirection.z);
		tag.put("LaunchDirection", direction);
		tag.putByte("Phase", (byte) getPhase().id);
		tag.putByte("Mission", (byte) getMission().id);
	}

	public enum Phase {
		WAITING(0),
		TAKEOFF(1),
		EXITING_DIMENSION(2),
		CRUISE(3),
		LANDING(4);

		private final int id;

		Phase(int id) {
			this.id = id;
		}

		public static Phase byId(byte id) {
			for (Phase phase : values()) {
				if (phase.id == id) {
					return phase;
				}
			}
			return TAKEOFF;
		}
	}

	public enum Mission {
		PACKAGE_TO_PLAYER(0),
		PACKAGE_TO_ALLAY_PORT(1),
		CARRIER_RETURN(2),
		CARRIER_RETURN_TO_PLAYER(3);

		private final int id;

		Mission(int id) {
			this.id = id;
		}

		public static Mission byId(byte id) {
			for (Mission mission : values()) {
				if (mission.id == id) {
					return mission;
				}
			}
			return PACKAGE_TO_PLAYER;
		}
	}
}
