package com.nobodiiiii.createbiotech.content.wirelessterminal;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class WirelessStockKeeperRequestScreen extends StockKeeperRequestScreen {

	public WirelessStockKeeperRequestScreen(StockKeeperRequestMenu container, Inventory inv, Component title) {
		super(container, inv, title);
	}
}
