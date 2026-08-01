package com.nobodiiiii.createbiotech.mixin;

import javax.annotation.Nullable;

import com.nobodiiiii.createbiotech.content.fluid.NetherPortalFluidBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin implements EntityBlock {
	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		if (!state.is(Blocks.NETHER_PORTAL))
			return null;

		return new NetherPortalFluidBlockEntity(pos, state);
	}
}
