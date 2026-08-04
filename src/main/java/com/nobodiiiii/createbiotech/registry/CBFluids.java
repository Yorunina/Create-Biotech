package com.nobodiiiii.createbiotech.registry;

import java.util.function.Consumer;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.content.buttercat.fluid.CreamBucketDispenseBehavior;
import com.nobodiiiii.createbiotech.content.buttercat.fluid.CreamFluidType;
import com.nobodiiiii.createbiotech.content.fluid.LiquidLivingSlimeBlock;
import com.nobodiiiii.createbiotech.content.fluid.LiquidLivingSlimeFluidType;
import com.nobodiiiii.createbiotech.content.fluid.TeleportationFluid;
import com.nobodiiiii.createbiotech.content.fluid.TeleportationLiquidBlock;
import com.simibubi.create.content.fluids.VirtualFluid;
import org.joml.Vector3f;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
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
	private static final ResourceLocation CREAM_STILL_TEXTURE =
		CreateBiotech.asResource("fluid/cream_still");
	private static final ResourceLocation CREAM_FLOW_TEXTURE =
		CreateBiotech.asResource("fluid/cream_flow");
	private static final Vector3f TELEPORTATION_SUBMERGED_FOG_COLOR = new Vector3f(0.72F, 0.48F, 0.86F);
	private static final float TELEPORTATION_FOG_DISTANCE_MODIFIER = 1F / 10F;

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

						@Override
						public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
							int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
							return TELEPORTATION_SUBMERGED_FOG_COLOR;
						}

						@Override
						public void modifyFogRender(Camera camera, FogMode mode, float renderDistance, float partialTick,
							float nearDistance, float farDistance, FogShape shape) {
							RenderSystem.setShaderFogShape(FogShape.CYLINDER);
							RenderSystem.setShaderFogStart(-8.0F);
							RenderSystem.setShaderFogEnd(96.0F * TELEPORTATION_FOG_DISTANCE_MODIFIER);
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

	public static final RegistryObject<LiquidLivingSlimeBlock> LIQUID_LIVING_SLIME_BLOCK =
		FLUID_BLOCKS.register("liquid_living_slime",
			() -> new LiquidLivingSlimeBlock(LIQUID_LIVING_SLIME, Block.Properties.of()
				.noCollission()
				.sound(SoundType.SLIME_BLOCK)
				.strength(-1.0F, 100.0F)
				.dynamicShape()
				.noLootTable()
				.liquid()));

	public static final RegistryObject<BucketItem> LIQUID_LIVING_SLIME_BUCKET =
		FLUID_ITEMS.register("liquid_living_slime_bucket",
			() -> new BucketItem(LIQUID_LIVING_SLIME, new Item.Properties()
				.craftRemainder(Items.BUCKET)
				.stacksTo(1)));

	public static final RegistryObject<CreamFluidType> CREAM_TYPE = FLUID_TYPES.register("cream",
		() -> new CreamFluidType(FluidType.Properties.create()
			.viscosity(100)
			.canSwim(false)
			.canPushEntity(false), CREAM_STILL_TEXTURE, CREAM_FLOW_TEXTURE));

	public static final RegistryObject<ForgeFlowingFluid.Source> CREAM = FLUIDS.register("cream",
		() -> new ForgeFlowingFluid.Source(creamProperties()));

	public static final RegistryObject<ForgeFlowingFluid.Flowing> CREAM_FLOWING = FLUIDS.register("flowing_cream",
		() -> new ForgeFlowingFluid.Flowing(creamProperties()));

	public static final RegistryObject<LiquidBlock> CREAM_BLOCK = FLUID_BLOCKS.register("cream",
		() -> new LiquidBlock(CREAM, Block.Properties.copy(Blocks.WATER)
			.mapColor(MapColor.TERRACOTTA_WHITE)));

	public static final RegistryObject<BucketItem> CREAM_BUCKET = FLUID_ITEMS.register("cream_bucket",
		() -> new BucketItem(CREAM, new Item.Properties()
			.craftRemainder(Items.BUCKET)
			.stacksTo(1)));

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

	private static ForgeFlowingFluid.Properties creamProperties() {
		return new ForgeFlowingFluid.Properties(CREAM_TYPE, CREAM, CREAM_FLOWING)
			.bucket(CREAM_BUCKET)
			.block(CREAM_BLOCK)
			.levelDecreasePerBlock(2)
			.tickRate(60)
			.slopeFindDistance(2)
			.explosionResistance(50F);
	}

	private CBFluids() {}

	public static void register(IEventBus modEventBus) {
		FLUID_TYPES.register(modEventBus);
		FLUIDS.register(modEventBus);
		FLUID_BLOCKS.register(modEventBus);
		FLUID_ITEMS.register(modEventBus);
	}

	public static void registerCreamDispenseBehavior() {
		DispenserBlock.registerBehavior(CREAM_BUCKET.get(), new CreamBucketDispenseBehavior());
	}
}
