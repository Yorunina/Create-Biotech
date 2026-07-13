package com.yision.phantom.block.phantomport;

import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.yision.phantom.logistics.courier.AirCourierReturnMode;
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

public class PhantomPortBlockEntity extends PackagePortBlockEntity {

	private final PhantomPortInventory portInventory;
	private final PhantomPortBeltAccess beltAccess;
	private final PhantomPortDispatchAccess dispatchAccess;
	private final PhantomPortAutomation automation;
	private final PhantomPortReturnQueue returnQueue;
	private final LerpedFloat flap;
	private final Set<UUID> landingCouriers = new HashSet<>();
	private AirCourierReturnMode returnMode = AirCourierReturnMode.DEFAULT_FOR_PORT;
	private boolean wasOpen;

	public PhantomPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		itemHandler = LazyOptional.of(() -> new PhantomPortAutomationInventoryWrapper(inventory, this));
		portInventory = new PhantomPortInventory(this);
		beltAccess = new PhantomPortBeltAccess(this);
		dispatchAccess = new PhantomPortDispatchAccess(this, portInventory, beltAccess);
		automation = new PhantomPortAutomation(this, portInventory, beltAccess);
		returnQueue = new PhantomPortReturnQueue(this, portInventory, beltAccess);
		flap = createChasingFlap();
		wasOpen = state.getValue(PhantomPortBlock.OPEN);
	}

	public PhantomPortBlockEntity(BlockPos pos, BlockState state) {
		this(CBBlockEntityTypes.PHANTOMPORT.get(), pos, state);
	}

	@Override
	public void tick() {
		super.tick();
		if (level != null && level.isClientSide()) {
			tickFlap();
			return;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		updateLandingOpenState();
		returnQueue.tick();
		dispatchAccess.tryDispatchToLaunchBelt();
		if (serverLevel.getGameTime() % 20 == 0) {
			PhantomPortTargetRegistry.update(serverLevel, worldPosition, addressFilter);
		}
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level == null || level.isClientSide()) {
			return;
		}
		automation.tick();
		dispatchAccess.tryDispatchToLaunchBelt();
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
		return beltAccess.specialSide();
	}

	public Direction getPackagerSide() {
		return beltAccess.packagerSide();
	}

	public boolean tryPullFromPackagerSide() {
		if (level == null || level.isClientSide()) {
			return false;
		}
		boolean pulled = automation.tryPullingFromSide(getPackagerSide());
		if (pulled) {
			dispatchAccess.tryDispatchToLaunchBelt();
		}
		return pulled;
	}

	public boolean tryDispatchToLaunchBelt() {
		if (level == null || level.isClientSide()) {
			return false;
		}
		return dispatchAccess.tryDispatchToLaunchBelt();
	}

	public ItemStackHandler getCarrierInventory() {
		return portInventory.carrierInventory();
	}

	void markPortContentsChanged() {
		dispatchAccess.clearPendingHudEntries();
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
		return portInventory.receivePackage(box);
	}

	public boolean canReceiveCarrier() {
		return portInventory.canReceiveCarrier();
	}

	public boolean receiveCarrier() {
		return portInventory.receiveCarrier();
	}

	public boolean receivePackageAndScheduleCarrierReturnToPlayer(ItemStack box, UUID playerId, int delayTicks) {
		return returnQueue.receivePackageAndScheduleCarrierReturnToPlayer(box, playerId, delayTicks);
	}

	public boolean receivePackageAndScheduleCarrierReturnToPlayer(ItemStack box, UUID playerId) {
		return returnQueue.receivePackageAndScheduleCarrierReturnToPlayer(
			box, playerId, PhantomPortReturnQueue.RETURN_LAUNCH_DELAY_TICKS);
	}

	public boolean tryQueueReturnCarrier(@Nullable ResourceKey<net.minecraft.world.level.Level> returnDimension,
										 @Nullable BlockPos returnPos) {
		return returnQueue.tryQueueReturnCarrier(returnDimension, returnPos);
	}

	public AirCourierReturnMode getReturnMode() {
		return returnMode;
	}

	public void setReturnMode(@Nullable AirCourierReturnMode returnMode) {
		this.returnMode = returnMode == null ? AirCourierReturnMode.DEFAULT_FOR_PORT : returnMode;
	}

	public enum CourierReceiveResult {
		REJECTED,
		CARRIER_STORED,
		RETURN_QUEUED,
		CARRIER_DROPPED
	}

	public CourierReceiveResult receivePackageAndHandleCarrier(ItemStack box,
		@Nullable ResourceKey<net.minecraft.world.level.Level> returnDimension, @Nullable BlockPos returnPos) {
		return returnQueue.receivePackageAndHandleCarrier(box, returnDimension, returnPos);
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

	private void tickFlap() {
		boolean open = getBlockState().getValue(PhantomPortBlock.OPEN);
		if (open != wasOpen) {
			flap.setValue(open ? 1 : -1);
			wasOpen = open;
		}
		flap.tickChaser();
	}

	private static LerpedFloat createChasingFlap() {
		return LerpedFloat.linear()
			.startWithValue(0)
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
		if (state.getValue(PhantomPortBlock.OPEN) != open) {
			level.setBlockAndUpdate(worldPosition, state.setValue(PhantomPortBlock.OPEN, open));
		}
	}

	@Override
	public InteractionResult use(Player player) {
		return super.use(player);
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return PhantomPortMenu.create(containerId, playerInventory, this);
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
			? AirCourierReturnMode.byName(tag.getString("ReturnMode"))
			: AirCourierReturnMode.DEFAULT_FOR_PORT;
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
}
