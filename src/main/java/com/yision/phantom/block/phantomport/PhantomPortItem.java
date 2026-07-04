package com.yision.phantom.block.phantomport;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PhantomPortItem extends BlockItem {

	public PhantomPortItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level world, Player player, ItemStack stack,
		BlockState state) {
		boolean updated = super.updateCustomBlockEntityTag(pos, world, player, stack, state);
		if (!world.isClientSide) {
			BeltBlockEntity belt = BeltHelper.getSegmentBE(world, pos.below());
			if (belt != null && belt.casing == CasingType.NONE) {
				belt.setCasingType(CasingType.ANDESITE);
			}
		}
		return updated;
	}
}
