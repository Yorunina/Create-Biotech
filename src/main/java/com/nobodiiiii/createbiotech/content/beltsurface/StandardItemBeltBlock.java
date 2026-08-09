package com.nobodiiiii.createbiotech.content.beltsurface;

import com.simibubi.create.content.kinetics.belt.BeltSlope;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** A Biotech belt variant that exposes a Create-compatible item surface. */
public interface StandardItemBeltBlock {

	Property<BeltSlope> createBiotech$slopeProperty();

	boolean createBiotech$canTransportItems(BlockState state);

	/** Whether the block can physically support a belt tunnel in its current state. */
	boolean createBiotech$canSupportTunnel(BlockState state);

	default boolean createBiotech$isHorizontalItemBelt(BlockState state) {
		return state.getBlock() == this
			&& state.getValue(createBiotech$slopeProperty()) == BeltSlope.HORIZONTAL
			&& createBiotech$canTransportItems(state);
	}
}
