package com.nobodiiiii.createbiotech.foundation.feature;

import com.nobodiiiii.createbiotech.CreateBiotech;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateBiotech.MOD_ID)
public final class CBFeaturePlacementHandler {
	private CBFeaturePlacementHandler() {}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;
		CBFeature feature = CBFeature.forBlock(event.getPlacedBlock().getBlock());
		if (feature != null && !feature.isEnabled())
			event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		CBFeature feature = CBFeature.forPlaceableItem(event.getItemStack().getItem());
		if (feature != null && !feature.isEnabled()) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.FAIL);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		CBFeature feature = CBFeature.forPlaceableItem(event.getItemStack().getItem());
		if (feature != null && !feature.isEnabled()) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.FAIL);
		}
	}
}
