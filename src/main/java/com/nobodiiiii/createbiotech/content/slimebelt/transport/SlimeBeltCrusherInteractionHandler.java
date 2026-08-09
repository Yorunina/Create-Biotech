package com.nobodiiiii.createbiotech.content.slimebelt.transport;

import com.nobodiiiii.createbiotech.content.beltsurface.CrusherInteractionCore;
import com.nobodiiiii.createbiotech.content.slimebelt.SlimeBeltLoopGeometry.Track;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;

/** Horizontal crushing-wheel interaction for the slime belt's FRONT track only. */
public final class SlimeBeltCrusherInteractionHandler {

	private SlimeBeltCrusherInteractionHandler() {}

	public static boolean checkForCrushers(SlimeBeltInventory beltInventory, TransportedItemStack currentItem,
		float nextOffset) {
		return CrusherInteractionCore.check(new SlimeBeltSurfaceTickContext(beltInventory, Track.FRONT),
			currentItem, nextOffset);
	}
}
