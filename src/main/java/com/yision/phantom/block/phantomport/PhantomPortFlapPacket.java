package com.yision.phantom.block.phantomport;

import com.simibubi.create.foundation.networking.BlockEntityDataPacket;

import net.minecraft.network.FriendlyByteBuf;

public class PhantomPortFlapPacket extends BlockEntityDataPacket<PhantomPortBlockEntity> {

	private final boolean inwards;

	public PhantomPortFlapPacket(FriendlyByteBuf buffer) {
		super(buffer);
		inwards = buffer.readBoolean();
	}

	public PhantomPortFlapPacket(PhantomPortBlockEntity blockEntity, boolean inwards) {
		super(blockEntity.getBlockPos());
		this.inwards = inwards;
	}

	@Override
	protected void writeData(FriendlyByteBuf buffer) {
		buffer.writeBoolean(inwards);
	}

	@Override
	protected void handlePacket(PhantomPortBlockEntity blockEntity) {
		blockEntity.flap(inwards);
	}
}
