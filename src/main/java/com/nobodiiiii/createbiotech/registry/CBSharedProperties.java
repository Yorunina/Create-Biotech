package com.nobodiiiii.createbiotech.registry;

import com.simibubi.create.foundation.data.SharedProperties;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Material baselines used by Create: Biotech blocks.
 *
 * <p>Every method returns a fresh mutable properties object. Tool tags remain
 * data-generated and are intentionally not part of this class.</p>
 */
public final class CBSharedProperties {

	private CBSharedProperties() {}

	public static Block.Properties createWooden() {
		return Block.Properties.copy(SharedProperties.wooden());
	}

	public static Block.Properties createStone() {
		return Block.Properties.copy(SharedProperties.stone());
	}

	public static Block.Properties createSoftMetal() {
		return Block.Properties.copy(SharedProperties.softMetal());
	}

	public static Block.Properties createCopperMetal() {
		return Block.Properties.copy(SharedProperties.copperMetal());
	}

	public static Block.Properties enchantingTable() {
		return Block.Properties.copy(Blocks.ENCHANTING_TABLE);
	}

	public static Block.Properties buddingExperience() {
		return Block.Properties.copy(Blocks.BUDDING_AMETHYST);
	}

	public static Block.Properties smallExperienceBud() {
		return Block.Properties.copy(Blocks.SMALL_AMETHYST_BUD);
	}

	public static Block.Properties mediumExperienceBud() {
		return Block.Properties.copy(Blocks.MEDIUM_AMETHYST_BUD);
	}

	public static Block.Properties largeExperienceBud() {
		return Block.Properties.copy(Blocks.LARGE_AMETHYST_BUD);
	}

	public static Block.Properties experienceCluster() {
		return Block.Properties.copy(Blocks.AMETHYST_CLUSTER);
	}

	public static Block.Properties vanillaGlass() {
		return Block.Properties.copy(Blocks.GLASS);
	}

	public static Block.Properties withObsidianDurability(Block.Properties properties) {
		return properties.strength(50.0f, 1200.0f)
			.requiresCorrectToolForDrops();
	}
}
