package com.nobodiiiii.createbiotech.client;

import com.nobodiiiii.createbiotech.content.wirelessterminal.WirelessStockKeeperRequestMenu;
import com.nobodiiiii.createbiotech.mixin.StockKeeperRequestMenuAccessor;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class WirelessStockKeeperRequestMenuClient {

	private WirelessStockKeeperRequestMenuClient() {}

	public static StockTickerBlockEntity createContent(WirelessStockKeeperRequestMenu menu,
		FriendlyByteBuf extraData) {
		StockKeeperRequestMenuAccessor accessor = (StockKeeperRequestMenuAccessor) menu;
		accessor.createBiotech$setAdmin(extraData.readBoolean());
		accessor.createBiotech$setLocked(extraData.readBoolean());

		BlockPos targetPos = extraData.readBlockPos();
		CompoundTag updateTag = extraData.readNbt();
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return null;

		BlockEntity existing = level.getBlockEntity(targetPos);
		StockTickerBlockEntity stockTicker;
		if (existing instanceof StockTickerBlockEntity existingStockTicker) {
			stockTicker = existingStockTicker;
		} else {
			stockTicker = new StockTickerBlockEntity(AllBlockEntityTypes.STOCK_TICKER.get(), targetPos,
				AllBlocks.STOCK_TICKER.getDefaultState());
			stockTicker.setLevel(level);
		}

		if (updateTag != null)
			stockTicker.handleUpdateTag(updateTag);
		return stockTicker;
	}
}
