package com.nobodiiiii.createbiotech.client;

import com.nobodiiiii.createbiotech.content.shulkerteleporter.ShulkerTeleporterScreen;
import com.nobodiiiii.createbiotech.content.spiderassemblytable.SpiderAssemblyTableScreen;
import com.nobodiiiii.createbiotech.content.wirelessterminal.WirelessStockKeeperRequestScreen;
import com.nobodiiiii.createbiotech.registry.CBMenuTypes;
import com.yision.allay.block.allayport.AllayPortScreen;
import com.yision.allay.item.allaycourier.AllayCourierScreen;

import net.minecraft.client.gui.screens.MenuScreens;

public final class CBMenuScreens {

	private CBMenuScreens() {}

	public static void register() {
		MenuScreens.register(CBMenuTypes.SPIDER_ASSEMBLY_TABLE.get(), SpiderAssemblyTableScreen::new);
		MenuScreens.register(CBMenuTypes.WIRELESS_STOCK_KEEPER_REQUEST.get(), WirelessStockKeeperRequestScreen::new);
		MenuScreens.register(CBMenuTypes.SHULKER_TELEPORTER.get(), ShulkerTeleporterScreen::new);
		MenuScreens.register(CBMenuTypes.ALLAY_PORT.get(), AllayPortScreen::new);
		MenuScreens.register(CBMenuTypes.ALLAY_COURIER.get(), AllayCourierScreen::new);
	}
}
