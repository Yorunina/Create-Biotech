package com.nobodiiiii.createbiotech.content.slimebelt;

import static com.simibubi.create.content.kinetics.belt.BeltPart.MIDDLE;
import static com.simibubi.create.content.kinetics.belt.BeltSlope.HORIZONTAL;
import static net.minecraft.core.Direction.AxisDirection.NEGATIVE;
import static net.minecraft.core.Direction.AxisDirection.POSITIVE;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.nobodiiiii.createbiotech.content.beltsurface.BeltSurface;
import com.nobodiiiii.createbiotech.content.beltsurface.BeltSurfaceHost;
import com.nobodiiiii.createbiotech.content.beltsurface.BeltTunnelCapabilityInvalidator;
import com.nobodiiiii.createbiotech.content.beltsurface.StandardItemBeltPort;
import com.nobodiiiii.createbiotech.content.slimebelt.SlimeBeltLoopGeometry.Track;
import com.nobodiiiii.createbiotech.content.slimebelt.transport.SlimeBeltInventory;
import com.nobodiiiii.createbiotech.content.slimebelt.transport.SlimeItemHandlerBeltSegment;
import com.nobodiiiii.createbiotech.content.slimebelt.transport.SlimeBeltMovementHandler;
import com.nobodiiiii.createbiotech.content.slimebelt.transport.SlimeBeltMovementHandler.TransportedEntityInfo;
import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.nobodiiiii.createbiotech.registry.CBBlocks;
import com.nobodiiiii.createbiotech.registry.CBConfigs;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public class SlimeBeltBlockEntity extends KineticBlockEntity implements BeltSurfaceHost, StandardItemBeltPort, Clearable {

	/** Ticks to wait before re-attempting a chain init that already failed once. */
	private static final int INIT_RETRY_INTERVAL = 20;
	/** Track.values() clones its array on every call; surface lookups run per funnel per tick. */
	private static final Track[] TRACKS = Track.values();

	public Map<Entity, TransportedEntityInfo> passengers;
	public int beltLength;
	public int index;
	protected BlockPos controller;
	protected SlimeBeltInventory inventory;
	public VersionedInventoryTrackerBehaviour invVersionTracker;
	public CompoundTag trackerUpdateTag;

	private final Map<Direction, LazyOptional<IItemHandler>> sidedHandlers;
	private LazyOptional<IItemHandler> nullSideHandler;
	private SlimeBeltLoopGeometry loopGeometry;
	private int initRetryCooldown;

	public SlimeBeltBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		controller = BlockPos.ZERO;
		sidedHandlers = new EnumMap<>(Direction.class);
		nullSideHandler = LazyOptional.empty();
	}

	public SlimeBeltBlockEntity(BlockPos pos, BlockState state) {
		this(CBBlockEntityTypes.SLIME_BELT.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(this::canInsertFrom)
			.allowingBeltFunnels()
			.setInsertionHandler(this::tryInsertingFromSide)
			.considerOccupiedWhen(this::isOccupied));
		behaviours.add(new TransportedItemStackHandlerBehaviour(this, this::applyToAllItems)
			.withStackPlacement(this::getWorldPositionOf));
		behaviours.add(invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
	}

	@Override
	public void tick() {
		if (beltLength == 0)
			tryInitBelt();

		super.tick();

		if (!getBlockState().is(CBBlocks.SLIME_BELT.get()))
			return;

		if (!isController())
			return;

		invalidateRenderBoundingBox();
		getInventory().tick();

		if (getSpeed() == 0)
			return;

		if (level.isClientSide)
			spawnSlimeParticles();

		if (passengers == null)
			passengers = new HashMap<>();

		List<Entity> toRemove = new ArrayList<>();
		passengers.forEach((entity, info) -> {
			boolean canBeTransported = SlimeBeltMovementHandler.canBeTransported(entity);
			boolean leftTheBelt =
				info.getTicksSinceLastCollision() > (getBlockState().getValue(SlimeBeltBlock.SLOPE) != HORIZONTAL ? 3 : 1);
			if (!canBeTransported || leftTheBelt) {
				toRemove.add(entity);
				return;
			}

			info.tick();
			SlimeBeltMovementHandler.transportEntity(this, entity, info);
		});
		toRemove.forEach(passengers::remove);
	}

	private void tryInitBelt() {
		if (initRetryCooldown > 0) {
			initRetryCooldown--;
			return;
		}
		SlimeBeltBlock.initBelt(level, worldPosition);
		if (beltLength == 0)
			initRetryCooldown = INIT_RETRY_INTERVAL;
	}

	@Override
	public float calculateStressApplied() {
		return isController() ? super.calculateStressApplied() : 0;
	}

	@Override
	public AABB createRenderBoundingBox() {
		return isController() ? super.createRenderBoundingBox().inflate(beltLength + 1) : super.createRenderBoundingBox();
	}

	public IItemHandler getItemCapability(Direction side) {
		if (!SlimeBeltBlock.canTransportObjects(getBlockState()))
			return null;
		return getItemHandler(side).orElse(null);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (!isItemHandlerCap(cap) || !SlimeBeltBlock.canTransportObjects(getBlockState()))
			return super.getCapability(cap, side);
		return getItemHandler(side).cast();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (isController())
			getInventory().ejectAll();
	}

	@Override
	public void clearContent() {
		if (inventory != null)
			inventory.getTransportedItems().clear();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		invalidateItemHandlers();
	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		if (controller != null)
			compound.put("Controller", NbtUtils.writeBlockPos(controller));
		compound.putBoolean("IsController", isController());
		compound.putInt("Length", beltLength);
		compound.putInt("Index", index);

		if (isController())
			compound.put("Inventory", getInventory().write());

		super.write(compound, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		super.read(compound, clientPacket);

		if (compound.getBoolean("IsController"))
			controller = worldPosition;

		if (!wasMoved) {
			if (!isController())
				controller = compound.contains("Controller")
					? NbtUtils.readBlockPos(compound.getCompound("Controller")) : worldPosition;
			trackerUpdateTag = compound;
			index = compound.getInt("Index");
			beltLength = compound.getInt("Length");
		}

		if (isController())
			getInventory().read(compound.getCompound("Inventory"));
	}

	@Override
	public void clearKineticInformation() {
		super.clearKineticInformation();
		beltLength = 0;
		index = 0;
		controller = null;
		trackerUpdateTag = new CompoundTag();
		initRetryCooldown = 0;
		// Keep passengers across KineticBlockEntity sync reads. Chain rewiring clears them explicitly.
		invalidateItemHandlers();
	}

	public SlimeBeltBlockEntity getControllerBE() {
		if (controller == null || !level.isLoaded(controller))
			return null;
		BlockEntity be = level.getBlockEntity(controller);
		return be instanceof SlimeBeltBlockEntity slimeBelt ? slimeBelt : null;
	}

	public void setController(BlockPos controller) {
		this.controller = controller;
	}

	public BlockPos getController() {
		return controller == null ? worldPosition : controller;
	}

	public boolean isController() {
		return controller != null && worldPosition.equals(controller);
	}

	/**
	 * Loop geometry anchored at this belt's controller. Cheap to call: rebuilt only when
	 * the captured blockstate reference or belt length changes, which covers chain
	 * rebuilds, slicing, rotation, NBT/client sync and structure moves without any
	 * explicit invalidation. Non-controller segments delegate to their controller.
	 */
	public SlimeBeltLoopGeometry getLoop() {
		if (!isController()) {
			SlimeBeltBlockEntity controllerBE = getControllerBE();
			if (controllerBE != null && controllerBE != this)
				return controllerBE.getLoop();
		}
		BlockState state = getBlockState();
		if (loopGeometry == null || !loopGeometry.matches(state, beltLength))
			loopGeometry = SlimeBeltLoopGeometry.of(state, worldPosition, beltLength);
		return loopGeometry;
	}

	public float getBeltMovementSpeed() {
		return getSpeed() / 480f;
	}

	public float getDirectionAwareBeltMovementSpeed() {
		int offset = getBeltFacing().getAxisDirection()
			.getStep();
		if (getBeltFacing().getAxis() == Axis.X)
			offset *= -1;
		return getBeltMovementSpeed() * offset;
	}

	private void spawnSlimeParticles() {
		if (beltLength <= 0)
			return;

		RandomSource random = level.random;
		CBConfigs.BeltParticles particles = CBConfigs.CLIENT.beltParticles;
		float chance = (float) Math.min(particles.slimeBeltMaxChance.get(),
			particles.slimeBeltBaseChance.get() + beltLength * particles.slimeBeltLengthChance.get()
				+ Math.abs(getBeltMovementSpeed()) * particles.slimeBeltSpeedChance.get());
		if (random.nextFloat() >= chance)
			return;

		int count = random.nextFloat() < .2f ? 2 : 1;
		for (int i = 0; i < count; i++)
			spawnSlimeParticle(random);
	}

	private void spawnSlimeParticle(RandomSource random) {
		float frontOffset = random.nextFloat() * beltLength;
		Vec3 surface = SlimeBeltHelper.getVectorForOffset(this, frontOffset);
		Vec3 normal = SlimeBeltHelper.getTrackNormal(this, frontOffset)
			.normalize();
		Vec3 across = SlimeBeltHelper.getPathAxis(this)
			.cross(normal);
		if (across.lengthSqr() > 1.0E-6d)
			across = across.normalize()
				.scale((random.nextDouble() - .5d) * .55d);
		else
			across = Vec3.ZERO;

		Vec3 position = surface.add(across)
			.add(normal.scale(.04d + random.nextDouble() * .04d));
		double pop = .025d + random.nextDouble() * .045d;
		double drift = .015d;
		Vec3 motion = normal.scale(pop)
			.add((random.nextDouble() - .5d) * drift, random.nextDouble() * .035d,
				(random.nextDouble() - .5d) * drift);
		level.addParticle(ParticleTypes.ITEM_SLIME, position.x, position.y, position.z, motion.x, motion.y, motion.z);
	}

	public boolean hasPulley() {
		return CBBlocks.SLIME_BELT.get().equals(getBlockState().getBlock())
			&& getBlockState().getValue(SlimeBeltBlock.PART) != MIDDLE;
	}

	public Vec3i getMovementDirection(boolean firstHalf) {
		return getMovementDirection(firstHalf, false);
	}

	public Vec3i getBeltChainDirection() {
		return getMovementDirection(true, true);
	}

	protected Vec3i getMovementDirection(boolean firstHalf, boolean ignoreHalves) {
		if (getSpeed() == 0)
			return BlockPos.ZERO;

		BlockState blockState = getBlockState();
		Direction beltFacing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
		BeltSlope slope = blockState.getValue(SlimeBeltBlock.SLOPE);
		if (slope == BeltSlope.VERTICAL) {
			int chainStep = beltFacing.getAxisDirection()
				.getStep();
			int y = getDirectionAwareBeltMovementSpeed() > 0 ? chainStep : -chainStep;
			return new Vec3i(0, y, 0);
		}
		BeltPart part = blockState.getValue(SlimeBeltBlock.PART);
		Axis axis = beltFacing.getAxis();

		Direction movementFacing = Direction.get(axis == Axis.X ? NEGATIVE : POSITIVE, axis);
		boolean notHorizontal = blockState.getValue(SlimeBeltBlock.SLOPE) != HORIZONTAL;
		if (getSpeed() < 0)
			movementFacing = movementFacing.getOpposite();
		Vec3i movement = movementFacing.getNormal();

		boolean slopeBeforeHalf = (part == BeltPart.END) == (beltFacing.getAxisDirection() == POSITIVE);
		boolean onSlope = notHorizontal && (part == MIDDLE || slopeBeforeHalf == firstHalf || ignoreHalves);
		boolean movingUp = onSlope && slope == (movementFacing == beltFacing ? BeltSlope.UPWARD : BeltSlope.DOWNWARD);

		if (!onSlope)
			return movement;

		return new Vec3i(movement.getX(), movingUp ? 1 : -1, movement.getZ());
	}

	public Direction getMovementFacing() {
		if (getBlockState().getValue(SlimeBeltBlock.SLOPE) == BeltSlope.VERTICAL)
			return getDirectionAwareBeltMovementSpeed() > 0 ? Direction.UP : Direction.DOWN;
		Axis axis = getBeltFacing().getAxis();
		return Direction.fromAxisAndDirection(axis, getBeltMovementSpeed() < 0 ^ axis == Axis.X ? NEGATIVE : POSITIVE);
	}

	public Direction getBeltFacing() {
		return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
	}

	public SlimeBeltInventory getInventory() {
		if (!isController()) {
			SlimeBeltBlockEntity controllerBE = getControllerBE();
			return controllerBE == null ? null : controllerBE.getInventory();
		}
		if (inventory == null)
			inventory = new SlimeBeltInventory(this);
		return inventory;
	}

	@Override
	public BlockPos createBiotech$getBlockPos() {
		return worldPosition;
	}

	@Override
	public boolean createBiotech$isHorizontalItemPort() {
		return getBlockState().getValue(SlimeBeltBlock.SLOPE) == BeltSlope.HORIZONTAL
			&& SlimeBeltBlock.canTransportObjects(getBlockState());
	}

	@Override
	public boolean createBiotech$addressesItemPort(Direction side) {
		return canTunnelAddressFront(side);
	}

	@Override
	public boolean createBiotech$canInsertIntoItemPort(Direction side) {
		return canTunnelInsertIntoFront(side);
	}

	@Override
	public ItemStack createBiotech$insertIntoItemPort(ItemStack stack, Direction side, boolean simulate) {
		return insertFromTunnelIntoFront(stack, side, simulate);
	}

	@Override
	public IItemHandler createBiotech$getItemHandler() {
		return getItemCapability(Direction.UP);
	}

	@Override
	public Direction createBiotech$getMovementFacing() {
		return getMovementFacing();
	}

	@Override
	public float createBiotech$getSpeed() {
		return getSpeed();
	}

	@Override
	public float createBiotech$getDirectionAwareSpeed() {
		return getDirectionAwareBeltMovementSpeed();
	}

	@Override
	public Vec3 createBiotech$getEjectionPosition() {
		SlimeBeltBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return Vec3.atCenterOf(worldPosition);
		int additionalOffset = getDirectionAwareBeltMovementSpeed() > 0 ? 1 : 0;
		return SlimeBeltHelper.getVectorForOffset(controllerBE, index + additionalOffset);
	}

	public void invalidateItemHandlers() {
		sidedHandlers.values().forEach(LazyOptional::invalidate);
		sidedHandlers.clear();
		nullSideHandler.invalidate();
		nullSideHandler = LazyOptional.empty();
		invalidateCaps();
		if (level != null)
			BeltTunnelCapabilityInvalidator.invalidate(level, worldPosition.above());
	}

	private LazyOptional<IItemHandler> getItemHandler(Direction side) {
		// Don't construct (and cache) a handler before the controller's inventory is ready — otherwise
		// constructing a handler too early would create a SlimeItemHandlerBeltSegment whose
		// beltInventory field is null, and subsequent getStackInSlot/insertItem/extractItem calls would NPE.
		// This happens at world-load time when a neighbouring funnel ticks before the belt chain is wired up.
		// Note: this preserves the per-side (FRONT/BACK track) routing — once the inventory is ready, the
		// cached handler still holds a stable inventory ref, and `side` keeps directing each request to its track.
		SlimeBeltInventory inv = getInventory();
		if (inv == null)
			return LazyOptional.empty();
		if (side == null) {
			if (!nullSideHandler.isPresent())
				nullSideHandler = LazyOptional.of(() -> new SlimeItemHandlerBeltSegment(inv, index, Direction.UP));
			return nullSideHandler;
		}
		return sidedHandlers.computeIfAbsent(side,
			dir -> LazyOptional.of(() -> new SlimeItemHandlerBeltSegment(inv, index, dir)));
	}

	private boolean canInsertFrom(Direction side) {
		return canInsertFrom(side, null);
	}

	public boolean canTunnelAddressFront(Direction side) {
		side = SlimeBeltInsertionPlanner.resolvePhysicalSide(this, side);
		SlimeBeltBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return false;
		return SlimeBeltHelper.resolveIOTrack(controllerBE, index, side) == Track.FRONT;
	}

	public boolean canTunnelInsertIntoFront(Direction side) {
		return canInsertFrom(side, Track.FRONT);
	}

	public ItemStack insertFromTunnelIntoFront(ItemStack stack, Direction side, boolean simulate) {
		return tryInsertingFromSide(new TransportedItemStack(stack), side, simulate, Track.FRONT);
	}

	private boolean canInsertFrom(Direction side, Track requiredTrack) {
		side = SlimeBeltInsertionPlanner.resolvePhysicalSide(this, side);
		if (getSpeed() == 0)
			return false;
		SlimeBeltBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return false;
		Track target = SlimeBeltHelper.resolveIOTrack(controllerBE, index, side);
		if (target == null || requiredTrack != null && target != requiredTrack)
			return false;
		return SlimeBeltInsertionPlanner.isCompatibleAdjacentChainInput(this, side, controllerBE, target);
	}

	private boolean isOccupied(Direction side) {
		side = SlimeBeltInsertionPlanner.resolvePhysicalSide(this, side);
		SlimeBeltInventory beltInventory = getInventory();
		boolean verticalHorizontalBeltInput = SlimeBeltInsertionPlanner.isVerticalHorizontalBeltInput(this, side);
		return beltInventory == null || getSpeed() == 0
			|| !beltInventory.canInsertAtFromSide(index, side, verticalHorizontalBeltInput);
	}

	private ItemStack tryInsertingFromSide(TransportedItemStack transportedStack, Direction side, boolean simulate) {
		return tryInsertingFromSide(transportedStack, side, simulate, null);
	}

	private ItemStack tryInsertingFromSide(TransportedItemStack transportedStack, Direction side, boolean simulate,
		Track requiredTrack) {
		side = SlimeBeltInsertionPlanner.resolvePhysicalSide(this, side);
		SlimeBeltInventory beltInventory = getInventory();
		boolean verticalHorizontalBeltInput = SlimeBeltInsertionPlanner.isVerticalHorizontalBeltInput(this, side);
		if (!SlimeBeltBlock.canTransportObjects(getBlockState()) || beltInventory == null)
			return transportedStack.stack;
		if (!canInsertFrom(side, requiredTrack))
			return transportedStack.stack;
		SlimeBeltInsertionPlanner.InsertionPlan plan =
			beltInventory.planInsertion(index, side, verticalHorizontalBeltInput, transportedStack);
		if (plan == null || requiredTrack != null && plan.track() != requiredTrack || !beltInventory.canInsert(plan))
			return transportedStack.stack;
		if (simulate)
			return ItemStack.EMPTY;

		TransportedItemStack copied = transportedStack.copy();
		beltInventory.applyInsertion(copied, plan);
		beltInventory.addItem(copied);
		setChanged();
		sendData();
		return ItemStack.EMPTY;
	}

	private void applyToAllItems(float maxDistanceFromCenter,
		Function<TransportedItemStack, TransportedResult> processFunction) {
		SlimeBeltBlockEntity controller = getControllerBE();
		if (controller == null)
			return;
		SlimeBeltInventory beltInventory = controller.getInventory();
		if (beltInventory != null)
			beltInventory.applyToEachWithin(index + .5f, maxDistanceFromCenter, processFunction);
	}

	private Vec3 getWorldPositionOf(TransportedItemStack transported) {
		SlimeBeltBlockEntity controllerBE = getControllerBE();
		return controllerBE == null ? Vec3.ZERO : SlimeBeltHelper.getVectorForOffset(controllerBE, transported.beltPosition);
	}

	@Override
	protected boolean canPropagateDiagonally(IRotate block, BlockState state) {
		return state.hasProperty(SlimeBeltBlock.SLOPE)
			&& (state.getValue(SlimeBeltBlock.SLOPE) == BeltSlope.UPWARD
				|| state.getValue(SlimeBeltBlock.SLOPE) == BeltSlope.DOWNWARD);
	}

	@Override
	public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff,
		boolean connectedViaAxes, boolean connectedViaCogs) {
		if (target instanceof SlimeBeltBlockEntity belt && !connectedViaAxes)
			return getController().equals(belt.getController()) ? 1 : 0;
		return 0;
	}

	public boolean shouldRenderNormally() {
		if (level == null)
			return isController();
		BlockState state = getBlockState();
		return state != null && state.hasProperty(SlimeBeltBlock.PART) && state.getValue(SlimeBeltBlock.PART) == BeltPart.START;
	}

	@Override
	public List<BeltSurface> surfaces() {
		SlimeBeltBlockEntity controller = surfaceController();
		if (controller == null)
			return List.of();
		List<BeltSurface> result = new ArrayList<>(2);
		for (Track track : TRACKS) {
			BeltSurface surface = surfaceOn(controller, track);
			if (surface != null)
				result.add(surface);
		}
		return result;
	}

	@Override
	public BeltSurface surfaceFor(Direction outwardNormal) {
		SlimeBeltBlockEntity controller = surfaceController();
		if (controller == null)
			return null;
		for (Track track : TRACKS) {
			if (SlimeBeltHelper.getRepresentativeSideForTrack(controller, index, track) != outwardNormal)
				continue;
			BeltSurface surface = surfaceOn(controller, track);
			if (surface != null)
				return surface;
		}
		return null;
	}

	private SlimeBeltBlockEntity surfaceController() {
		if (level == null)
			return null;
		SlimeBeltBlockEntity controller = getControllerBE();
		return controller == null || controller.beltLength == 0 ? null : controller;
	}

	private BeltSurface surfaceOn(SlimeBeltBlockEntity controller, Track track) {
		Direction outwardNormal = SlimeBeltHelper.getRepresentativeSideForTrack(controller, index, track);
		Direction movementFacing = SlimeBeltHelper.getMovementFacingForTrack(controller, track);
		if (outwardNormal.getAxis() == movementFacing.getAxis())
			return null;
		return BeltSurface.of(this, worldPosition, index, outwardNormal, movementFacing);
	}
}
