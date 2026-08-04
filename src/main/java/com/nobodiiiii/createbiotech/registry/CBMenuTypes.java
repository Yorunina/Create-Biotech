package com.nobodiiiii.createbiotech.registry;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.content.shulkerteleporter.ShulkerTeleporterMenu;
import com.nobodiiiii.createbiotech.content.spiderassemblytable.SpiderAssemblyTableMenu;
import com.nobodiiiii.createbiotech.content.wirelessterminal.WirelessStockKeeperRequestMenu;
import com.yision.allay.block.allayport.AllayPortMenu;
import com.yision.allay.item.allaycourier.AllayCourierMenu;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CBMenuTypes {

	public static final DeferredRegister<MenuType<?>> MENU_TYPES =
		DeferredRegister.create(ForgeRegistries.MENU_TYPES, CreateBiotech.MOD_ID);

	public static final RegistryObject<MenuType<SpiderAssemblyTableMenu>> SPIDER_ASSEMBLY_TABLE =
		register("spider_assembly_table", SpiderAssemblyTableMenu::new);

	public static final RegistryObject<MenuType<WirelessStockKeeperRequestMenu>> WIRELESS_STOCK_KEEPER_REQUEST =
		register("wireless_stock_keeper_request", WirelessStockKeeperRequestMenu::new);

	public static final RegistryObject<MenuType<ShulkerTeleporterMenu>> SHULKER_TELEPORTER =
		register("shulker_teleporter", ShulkerTeleporterMenu::new);

	public static final RegistryObject<MenuType<AllayPortMenu>> ALLAY_PORT =
		register("allay_port", AllayPortMenu::new);

	public static final RegistryObject<MenuType<AllayCourierMenu>> ALLAY_COURIER =
		register("allay_courier", AllayCourierMenu::new);

	private CBMenuTypes() {}

	public static void register(IEventBus modEventBus) {
		MENU_TYPES.register(modEventBus);
	}

	private static <M extends AbstractContainerMenu> RegistryObject<MenuType<M>> register(String name,
		IContainerFactory<M> factory) {
		return MENU_TYPES.register(name, () -> IForgeMenuType.create(factory));
	}
}
