package com.nobodiiiii.createbiotech.registry;

import java.util.function.Consumer;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.content.buttercat.register.ModFluids;
import com.nobodiiiii.createbiotech.content.fluid.LiquidLivingSlimeFluidType;
import com.nobodiiiii.createbiotech.content.fluid.TeleportationFluid;
import com.nobodiiiii.createbiotech.content.fluid.TeleportationLiquidBlock;
import com.simibubi.create.content.fluids.VirtualFluid;
import com.tterrag.registrate.util.entry.FluidEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CBFluids {

	public static final DeferredRegister<FluidType> FLUID_TYPES =
		DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, CreateBiotech.MOD_ID);

	public static final DeferredRegister<Fluid> FLUIDS =
		DeferredRegister.create(ForgeRegistries.FLUIDS, CreateBiotech.MOD_ID);

	public static final DeferredRegister<Block> FLUID_BLOCKS =
		DeferredRegister.create(ForgeRegistries.BLOCKS, CreateBiotech.MOD_ID);

	public static final DeferredRegister<Item> FLUID_ITEMS =
		DeferredRegister.create(ForgeRegistries.ITEMS, CreateBiotech.MOD_ID);

	private static final ResourceLocation EXPERIENCE_STILL_TEXTURE =
		CreateBiotech.asResource("fluid/experience_still");
	private static final ResourceLocation EXPERIENCE_FLOW_TEXTURE =
		CreateBiotech.asResource("fluid/experience_flow");
	private static final ResourceLocation NETHER_PORTAL_TEXTURE =
		new ResourceLocation("minecraft", "block/nether_portal");

	public static final RegistryObject<FluidType> EXPERIENCE_TYPE =
		FLUID_TYPES.register("experience",
			() -> new FluidType(FluidType.Properties.create()
				.lightLevel(15)) {
				@Override
				public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
					consumer.accept(new IClientFluidTypeExtensions() {
						@Override
						public ResourceLocation getStillTexture() {
							return EXPERIENCE_STILL_TEXTURE;
						}

						@Override
						public ResourceLocation getFlowingTexture() {
							return EXPERIENCE_FLOW_TEXTURE;
						}
					});
				}
			});

	public static final RegistryObject<VirtualFluid> EXPERIENCE =
		FLUIDS.register("experience", () -> VirtualFluid.createSource(experienceProperties()));

	public static final RegistryObject<VirtualFluid> EXPERIENCE_FLOWING =
		FLUIDS.register("flowing_experience", () -> VirtualFluid.createFlowing(experienceProperties()));

	public static final RegistryObject<FluidType> TELEPORTATION_TYPE =
		FLUID_TYPES.register("teleportation",
			() -> new FluidType(FluidType.Properties.create()
				.sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
				.sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
				.density(3000)
				.viscosity(6000)
				.lightLevel(11)) {
				@Override
				public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
					consumer.accept(new IClientFluidTypeExtensions() {
						@Override
						public ResourceLocation getStillTexture() {
							return NETHER_PORTAL_TEXTURE;
						}

						@Override
						public ResourceLocation getFlowingTexture() {
							return NETHER_PORTAL_TEXTURE;
						}
					});
				}
			});

	public static final RegistryObject<TeleportationFluid.Source> TELEPORTATION =
		FLUIDS.register("teleportation", () -> new TeleportationFluid.Source(teleportationProperties()));

	public static final RegistryObject<TeleportationFluid.Flowing> TELEPORTATION_FLOWING =
		FLUIDS.register("flowing_teleportation", () -> new TeleportationFluid.Flowing(teleportationProperties()));

	public static final RegistryObject<TeleportationLiquidBlock> TELEPORTATION_BLOCK =
		FLUID_BLOCKS.register("teleportation",
			() -> new TeleportationLiquidBlock(TELEPORTATION, Block.Properties.of()
				.mapColor(MapColor.COLOR_PURPLE)
				.replaceable()
				.noCollission()
				.strength(100.0F)
				.lightLevel(state -> 11)
				.pushReaction(PushReaction.DESTROY)
				.noLootTable()
				.liquid()
				.sound(SoundType.EMPTY)));

	public static final RegistryObject<BucketItem> TELEPORTATION_BUCKET =
		FLUID_ITEMS.register("teleportation_bucket",
			() -> new BucketItem(TELEPORTATION, new Item.Properties()
				.craftRemainder(Items.BUCKET)
				.stacksTo(1)));

	public static final RegistryObject<LiquidLivingSlimeFluidType> LIQUID_LIVING_SLIME_TYPE =
		FLUID_TYPES.register("liquid_living_slime",
			() -> new LiquidLivingSlimeFluidType(FluidType.Properties.create()
				.motionScale(0.004D)
				.fallDistanceModifier(0F)
				.sound(SoundActions.BUCKET_FILL, SoundEvents.SLIME_JUMP)
				.sound(SoundActions.BUCKET_EMPTY, SoundEvents.SLIME_JUMP)
				.viscosity(5000)
				.density(1400)));

	public static final RegistryObject<ForgeFlowingFluid.Source> LIQUID_LIVING_SLIME =
		FLUIDS.register("liquid_living_slime",
			() -> new ForgeFlowingFluid.Source(CBFluids.liquidLivingSlimeProperties()));

	public static final RegistryObject<ForgeFlowingFluid.Flowing> LIQUID_LIVING_SLIME_FLOWING =
		FLUIDS.register("liquid_living_slime_flowing",
			() -> new ForgeFlowingFluid.Flowing(CBFluids.liquidLivingSlimeProperties()));

	public static final RegistryObject<LiquidBlock> LIQUID_LIVING_SLIME_BLOCK =
		FLUID_BLOCKS.register("liquid_living_slime",
			() -> new LiquidBlock(LIQUID_LIVING_SLIME, Block.Properties.of()
				.noCollission()
				.sound(SoundType.SLIME_BLOCK)
				.strength(100f)
				.noLootTable()
				.liquid()));

	public static final RegistryObject<BucketItem> LIQUID_LIVING_SLIME_BUCKET =
		FLUID_ITEMS.register("liquid_living_slime_bucket",
			() -> new BucketItem(LIQUID_LIVING_SLIME, new Item.Properties()
				.craftRemainder(Items.BUCKET)
				.stacksTo(1)));

	// Butter Cat content is registered through the shared ButterCat registrate, and re-exported
	// here so the project's primary fluid registry remains the place to inspect mod fluids.
	public static final FluidEntry<ForgeFlowingFluid.Flowing> CREAM = ModFluids.CREAM;

	private static ForgeFlowingFluid.Properties experienceProperties() {
		return new ForgeFlowingFluid.Properties(EXPERIENCE_TYPE, EXPERIENCE, EXPERIENCE_FLOWING);
	}

	private static ForgeFlowingFluid.Properties teleportationProperties() {
		return new ForgeFlowingFluid.Properties(TELEPORTATION_TYPE, TELEPORTATION, TELEPORTATION_FLOWING)
			.bucket(TELEPORTATION_BUCKET)
			.block(TELEPORTATION_BLOCK)
			.levelDecreasePerBlock(2)
			.tickRate(30)
			.slopeFindDistance(2)
			.explosionResistance(100f);
	}

	private static ForgeFlowingFluid.Properties liquidLivingSlimeProperties() {
		return new ForgeFlowingFluid.Properties(
			LIQUID_LIVING_SLIME_TYPE,
			LIQUID_LIVING_SLIME,
			LIQUID_LIVING_SLIME_FLOWING)
			.bucket(LIQUID_LIVING_SLIME_BUCKET)
			.block(LIQUID_LIVING_SLIME_BLOCK)
			.levelDecreasePerBlock(2)
			.tickRate(60)
			.slopeFindDistance(4)
			.explosionResistance(100f);
	}

	private CBFluids() {}

	public static void register(IEventBus modEventBus) {
		FLUID_TYPES.register(modEventBus);
		FLUIDS.register(modEventBus);
		FLUID_BLOCKS.register(modEventBus);
		FLUID_ITEMS.register(modEventBus);
	}
}
