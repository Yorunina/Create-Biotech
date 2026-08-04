package com.yision.allay.client;

import com.yision.allay.block.allayport.AllayPortBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class AllayPortMenuClient {

	private AllayPortMenuClient() {}

	public static AllayPortBlockEntity createContent(FriendlyByteBuf extraData) {
		BlockPos blockPos = extraData.readBlockPos();
		ClientLevel level = Minecraft.getInstance().level;
		BlockEntity blockEntity = level != null ? level.getBlockEntity(blockPos) : null;
		return blockEntity instanceof AllayPortBlockEntity allayPort ? allayPort : null;
	}
}
