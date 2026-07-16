package com.yision.allay.block.allayport;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.nobodiiiii.createbiotech.network.CBPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.yision.allay.logistics.courier.AllayCourierReturnMode;
import com.yision.allay.logistics.courier.AllayCourierTask;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AllayPortBlockEntity extends PackagePortBlockEntity {
	private static final double DEPARTURE_OFFSET = 0.5;

	private final AllayPortInventory portInventory;
	private final AllayPortDispatchAccess dispatchAccess;
	private final AllayPortAutomation automation;
	private final AllayPortReturnQueue returnQueue;
	private final LerpedFloat flap;
	private final Set<UUID> landingCouriers = new HashSet<>();
	private AllayCourierReturnMode returnMode = AllayCourierReturnMode.DEFAULT_FOR_PORT;

	public AllayPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		itemHandler = LazyOptional.of(() -> new AllayPortAutomationInventoryWrapper(inventory, this));
		portInventory = new AllayPortInventory(this);
		dispatchAccess = new AllayPortDispatchAccess(this, portInventory);
		automation = new AllayPortAutomation(this, portInventory);
		returnQueue = new AllayPortReturnQueue(this, portInventory);
		flap = createChasingFlap();
	}

	public AllayPortBlockEntity(BlockPos pos, BlockState state) {
		this(CBBlockEntityTypes.ALLAY_PORT.get(), pos, state);
	}

	@Override
	public void tick() {
		super.tick();
		flap.tickChaser();
		if (level != null && level.isClientSide()) {
			return;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		updateLandingOpenState();
		returnQueue.tick();
		dispatchAccess.tryDispatch();
		if (serverLevel.getGameTime() % 20 == 0) {
			AllayPortTargetRegistry.update(serverLevel, worldPosition, addressFilter);
		}
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level == null || level.isClientSide()) {
			return;
		}
		automation.tick();
		dispatchAccess.tryDispatch();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
	}

	public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
		return dispatchAccess.getItemHandler(side);
	}

	@Nullable IItemHandler getAutomationItemHandler() {
		return itemHandler.orElse(null);
	}

	public Direction getLaunchSide() {
		return getBlockState().getValue(AllayPortBlock.FACING);
	}

	public Direction getPackagerSide() {
		return getLaunchSide().getOpposite();
	}

	Vec3 getCourierSpawnPosition() {
		return Vec3.atCenterOf(worldPosition);
	}

	Vec3 getCourierLaunchDirection() {
		return Vec3.atLowerCornerOf(getLaunchSide().getNormal());
	}

	AllayCourierTask prepareCourierDeparture(AllayCourierTask task) {
		return task.withInitialWaypoint(
			getCourierSpawnPosition().add(getCourierLaunchDirection().scale(DEPARTURE_OFFSET)));
	}

	public boolean tryPullFromPackagerSide() {
		if (level == null || level.isClientSide()) {
			return false;
		}
		boolean pulled = automation.tryPullingFromSide(getPackagerSide());
		if (pulled) {
			dispatchAccess.tryDispatch();
		}
		return pulled;
	}

	public boolean tryDispatch() {
		if (level == null || level.isClientSide()) {
			return false;
		}
		return dispatchAccess.tryDispatch();
	}

	public ItemStackHandler getCarrierInventory() {
		return portInventory.carrierInventory();
	}

	void markPortContentsChanged() {
		setChanged();
		if (level != null) {
			level.blockEntityChanged(worldPosition);
		}
	}

	public boolean canReceiveCourier(ItemStack box) {
		return portInventory.canReceiveCourier(box);
	}

	public boolean receiveCourier(ItemStack box) {
		return portInventory.receiveCourier(box);
	}

	public boolean canReceivePackage(ItemStack box) {
		return portInventory.canReceivePackage(box);
	}

	public boolean receivePackage(ItemStack box) {
		boolean received = portInventory.receivePackage(box);
		if (received) {
			flap(true);
		}
		return received;
	}

	public boolean canReceiveCarrier() {
		return portInventory.canReceiveCarrier();
	}

	public boolean receiveCarrier() {
		boolean received = portInventory.receiveCarrier();
		if (received) {
			flap(true);
		}
		return received;
	}

	public boolean receivePackageAndScheduleCarrierReturnToPlayer(ItemStack box, UUID playerId, int delayTicks) {
		boolean received = returnQueue.receivePackageAndScheduleCarrierReturnToPlayer(box, playerId, delayTicks);
		if (received) {
			flap(true);
		}
		return received;
	}

	public boolean receivePackageAndScheduleCarrierReturnToPlayer(ItemStack box, UUID playerId) {
		return returnQueue.receivePackageAndScheduleCarrierReturnToPlayer(
			box, playerId, AllayPortReturnQueue.RETURN_LAUNCH_DELAY_TICKS);
	}

	public boolean tryQueueReturnCarrier(@Nullable ResourceKey<net.minecraft.world.level.Level> returnDimension,
										 @Nullable BlockPos returnPos) {
		return returnQueue.tryQueueReturnCarrier(returnDimension, returnPos);
	}

	public AllayCourierReturnMode getReturnMode() {
		return returnMode;
	}

	public void setReturnMode(@Nullable AllayCourierReturnMode returnMode) {
		this.returnMode = returnMode == null ? AllayCourierReturnMode.DEFAULT_FOR_PORT : returnMode;
	}

	public enum CourierReceiveResult {
		REJECTED,
		CARRIER_STORED,
		RETURN_QUEUED,
		CARRIER_DROPPED
	}

	public CourierReceiveResult receivePackageAndHandleCarrier(ItemStack box,
		@Nullable ResourceKey<net.minecraft.world.level.Level> returnDimension, @Nullable BlockPos returnPos) {
		CourierReceiveResult result = returnQueue.receivePackageAndHandleCarrier(box, returnDimension, returnPos);
		if (result != CourierReceiveResult.REJECTED) {
			flap(true);
		}
		return result;
	}

	@Override
	protected void onOpenChange(boolean open) {
		if (level == null) {
			return;
		}
		level.playSound(null, worldPosition, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE,
			SoundSource.BLOCKS);
	}

	public float getFlap(float partialTicks) {
		return flap.getValue(partialTicks);
	}

	public void flap(boolean inward) {
		if (level == null) {
			return;
		}
		if (!level.isClientSide()) {
			CBPackets.send(packetTarget(), new AllayPortFlapPacket(this, inward));
		} else {
			flap.setValue(inward ? -1 : 1);
			AllSoundEvents.FUNNEL_FLAP.playAt(level, worldPosition, 1, 1, true);
		}
	}

	private static LerpedFloat createChasingFlap() {
		return LerpedFloat.linear()
			.startWithValue(.25f)
			.chase(0, .05f, Chaser.EXP);
	}

	public void setCourierLandingOpen(UUID courierId, boolean open) {
		boolean changed = open ? landingCouriers.add(courierId) : landingCouriers.remove(courierId);
		if (changed) {
			updateLandingOpenState();
		}
	}

	private void updateLandingOpenState() {
		if (level == null) {
			return;
		}
		boolean open = !landingCouriers.isEmpty();
		BlockState state = getBlockState();
		if (state.getValue(AllayPortBlock.OPEN) != open) {
			level.setBlockAndUpdate(worldPosition, state.setValue(AllayPortBlock.OPEN, open));
		}
	}

	@Override
	public InteractionResult use(Player player) {
		return super.use(player);
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return AllayPortMenu.create(containerId, playerInventory, this);
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.putString("ReturnMode", returnMode.serializedName());
		portInventory.write(tag);
		returnQueue.write(tag);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		returnMode = tag.contains("ReturnMode")
			? AllayCourierReturnMode.byName(tag.getString("ReturnMode"))
			: AllayCourierReturnMode.DEFAULT_FOR_PORT;
		portInventory.read(tag);
		returnQueue.read(tag);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			IItemHandler handler = getItemHandler(side);
			if (handler != null) {
				return LazyOptional.of(() -> handler).cast();
			}
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void destroy() {
		portInventory.dropAllCarriers();
		super.destroy();
	}

	@Override
	public AABB getRenderBoundingBox() {
		return super.getRenderBoundingBox().expandTowards(0, 1, 0);
	}
}
