package com.yision.allay.block.allayport;

import com.simibubi.create.foundation.networking.BlockEntityDataPacket;

import net.minecraft.network.FriendlyByteBuf;

public class AllayPortFlapPacket extends BlockEntityDataPacket<AllayPortBlockEntity> {

	private final boolean inwards;

	public AllayPortFlapPacket(FriendlyByteBuf buffer) {
		super(buffer);
		inwards = buffer.readBoolean();
	}

	public AllayPortFlapPacket(AllayPortBlockEntity blockEntity, boolean inwards) {
		super(blockEntity.getBlockPos());
		this.inwards = inwards;
	}

	@Override
	protected void writeData(FriendlyByteBuf buffer) {
		buffer.writeBoolean(inwards);
	}

	@Override
	protected void handlePacket(AllayPortBlockEntity blockEntity) {
		blockEntity.flap(inwards);
	}
}
