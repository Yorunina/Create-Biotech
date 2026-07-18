package com.nobodiiiii.createbiotech.content.fluid;

import javax.annotation.Nullable;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.nobodiiiii.createbiotech.registry.CBFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class NetherPortalFluidBlockEntity extends BlockEntity {
	public static final int CAPACITY = 250;

	private final PortalFluidHandler fluidHandler = new PortalFluidHandler();
	private final LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> fluidHandler);
	private int remainingFluid = CAPACITY;

	public NetherPortalFluidBlockEntity(BlockPos pos, BlockState state) {
		super(CBBlockEntityTypes.NETHER_PORTAL_FLUID.get(), pos, state);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("RemainingFluid", remainingFluid);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		remainingFluid = tag.contains("RemainingFluid", Tag.TAG_INT)
			? Mth.clamp(tag.getInt("RemainingFluid"), 0, CAPACITY)
			: CAPACITY;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		destroyPortalIfEmpty();
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER)
			return fluidCapability.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		fluidCapability.invalidate();
	}

	private FluidStack drain(int requestedAmount, IFluidHandler.FluidAction action) {
		int drainedAmount = Math.min(Math.max(requestedAmount, 0), remainingFluid);
		if (drainedAmount == 0)
			return FluidStack.EMPTY;

		FluidStack drained = new FluidStack(CBFluids.TELEPORTATION.get(), drainedAmount);
		if (action.execute()) {
			remainingFluid -= drainedAmount;
			setChanged();
			destroyPortalIfEmpty();
		}
		return drained;
	}

	private void destroyPortalIfEmpty() {
		if (remainingFluid > 0 || level == null || level.isClientSide || isRemoved())
			return;
		if (level.getBlockState(worldPosition)
			.is(Blocks.NETHER_PORTAL))
			level.destroyBlock(worldPosition, false);
	}

	private class PortalFluidHandler implements IFluidHandler {
		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return tank == 0 && remainingFluid > 0
				? new FluidStack(CBFluids.TELEPORTATION.get(), remainingFluid)
				: FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			return tank == 0 ? CAPACITY : 0;
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return 0;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			if (resource.isEmpty() || resource.getFluid() != CBFluids.TELEPORTATION.get())
				return FluidStack.EMPTY;
			return NetherPortalFluidBlockEntity.this.drain(resource.getAmount(), action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return NetherPortalFluidBlockEntity.this.drain(maxDrain, action);
		}
	}
}
