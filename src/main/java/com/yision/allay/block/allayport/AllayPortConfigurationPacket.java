package com.yision.allay.block.allayport;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import com.yision.allay.logistics.courier.AllayCourierReturnMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class AllayPortConfigurationPacket extends BlockEntityConfigurationPacket<AllayPortBlockEntity> {

	private String newFilter;
	private boolean acceptPackages;
	private AllayCourierReturnMode returnMode;

	public AllayPortConfigurationPacket(BlockPos pos, String newFilter, boolean acceptPackages,
		AllayCourierReturnMode returnMode) {
		super(pos);
		this.newFilter = newFilter == null ? "" : newFilter;
		this.acceptPackages = acceptPackages;
		this.returnMode = returnMode == null ? AllayCourierReturnMode.DEFAULT_FOR_PORT : returnMode;
	}

	public AllayPortConfigurationPacket(FriendlyByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeSettings(FriendlyByteBuf buffer) {
		buffer.writeBoolean(acceptPackages);
		buffer.writeUtf(newFilter);
		buffer.writeVarInt(returnMode.id());
	}

	@Override
	protected void readSettings(FriendlyByteBuf buffer) {
		acceptPackages = buffer.readBoolean();
		newFilter = buffer.readUtf();
		returnMode = AllayCourierReturnMode.byId(buffer.readVarInt());
	}

	@Override
	protected void applySettings(AllayPortBlockEntity be) {
		boolean filterChanged = !be.addressFilter.equals(newFilter) || be.acceptsPackages != acceptPackages;
		boolean modeChanged = be.getReturnMode() != returnMode;
		if (!filterChanged && !modeChanged) {
			return;
		}

		if (filterChanged) {
			be.addressFilter = newFilter;
			be.acceptsPackages = acceptPackages;
			be.filterChanged();
		}
		if (modeChanged) {
			be.setReturnMode(returnMode);
		}
		be.notifyUpdate();
	}
}
