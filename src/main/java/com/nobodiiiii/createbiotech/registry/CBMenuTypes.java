package com.nobodiiiii.createbiotech.registry;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.content.shulkerteleporter.ShulkerTeleporterMenu;
import com.nobodiiiii.createbiotech.content.spiderassemblytable.SpiderAssemblyTableMenu;
import com.nobodiiiii.createbiotech.content.wirelessterminal.WirelessStockKeeperRequestMenu;
import com.yision.allay.block.allayport.AllayPortMenu;
import com.yision.allay.item.miniallay.MiniAllayMenu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CBMenuTypes {

	public static final DeferredRegister<MenuType<?>> MENU_TYPES =
		DeferredRegister.create(ForgeRegistries.MENU_TYPES, CreateBiotech.MOD_ID);

	public static final RegistryObject<MenuType<SpiderAssemblyTableMenu>> SPIDER_ASSEMBLY_TABLE =
		MENU_TYPES.register("spider_assembly_table", () -> IForgeMenuType.create(SpiderAssemblyTableMenu::new));

	public static final RegistryObject<MenuType<WirelessStockKeeperRequestMenu>> WIRELESS_STOCK_KEEPER_REQUEST =
		MENU_TYPES.register("wireless_stock_keeper_request",
			() -> IForgeMenuType.create(WirelessStockKeeperRequestMenu::new));

	public static final RegistryObject<MenuType<ShulkerTeleporterMenu>> SHULKER_TELEPORTER =
		MENU_TYPES.register("shulker_teleporter", () -> IForgeMenuType.create(ShulkerTeleporterMenu::new));

	public static final RegistryObject<MenuType<AllayPortMenu>> ALLAY_PORT =
		MENU_TYPES.register("allay_port", () -> IForgeMenuType.create(AllayPortMenu::new));

	public static final RegistryObject<MenuType<MiniAllayMenu>> MINI_ALLAY =
		MENU_TYPES.register("mini_allay", () -> IForgeMenuType.create(MiniAllayMenu::new));

	private CBMenuTypes() {}

	public static void register(IEventBus modEventBus) {
		MENU_TYPES.register(modEventBus);
	}
}
