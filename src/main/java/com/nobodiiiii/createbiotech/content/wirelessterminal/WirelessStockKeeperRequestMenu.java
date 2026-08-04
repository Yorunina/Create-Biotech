package com.nobodiiiii.createbiotech.content.wirelessterminal;

import com.nobodiiiii.createbiotech.client.WirelessStockKeeperRequestMenuClient;
import com.nobodiiiii.createbiotech.registry.CBMenuTypes;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class WirelessStockKeeperRequestMenu extends StockKeeperRequestMenu {

	public WirelessStockKeeperRequestMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		this(CBMenuTypes.WIRELESS_STOCK_KEEPER_REQUEST.get(), id, inv, extraData);
	}

	public WirelessStockKeeperRequestMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public WirelessStockKeeperRequestMenu(MenuType<?> type, int id, Inventory inv, StockTickerBlockEntity contentHolder) {
		super(type, id, inv, contentHolder);
	}

	public static AbstractContainerMenu create(int containerId, Inventory playerInventory,
		StockTickerBlockEntity stockTickerBlockEntity) {
		return new WirelessStockKeeperRequestMenu(CBMenuTypes.WIRELESS_STOCK_KEEPER_REQUEST.get(), containerId,
			playerInventory, stockTickerBlockEntity);
	}

	@Override
	protected StockTickerBlockEntity createOnClient(FriendlyByteBuf extraData) {
		return WirelessStockKeeperRequestMenuClient.createContent(this, extraData);
	}

	@Override
	public boolean stillValid(Player player) {
		if (contentHolder == null || contentHolder.isRemoved())
			return false;
		return player.level().isClientSide || contentHolder.isKeeperPresent();
	}
}
