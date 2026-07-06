package com.yision.phantom.block.phantomport;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import com.yision.phantom.logistics.courier.AirCourierReturnMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class PhantomPortConfigurationPacket extends BlockEntityConfigurationPacket<PhantomPortBlockEntity> {

	private String newFilter;
	private boolean acceptPackages;
	private AirCourierReturnMode returnMode;

	public PhantomPortConfigurationPacket(BlockPos pos, String newFilter, boolean acceptPackages,
		AirCourierReturnMode returnMode) {
		super(pos);
		this.newFilter = newFilter == null ? "" : newFilter;
		this.acceptPackages = acceptPackages;
		this.returnMode = returnMode == null ? AirCourierReturnMode.DEFAULT_FOR_PORT : returnMode;
	}

	public PhantomPortConfigurationPacket(FriendlyByteBuf buffer) {
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
		returnMode = AirCourierReturnMode.byId(buffer.readVarInt());
	}

	@Override
	protected void applySettings(PhantomPortBlockEntity be) {
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
