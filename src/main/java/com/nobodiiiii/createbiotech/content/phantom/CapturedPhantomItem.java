package com.nobodiiiii.createbiotech.content.phantom;

import com.nobodiiiii.createbiotech.foundation.item.RenderedLivingEntityItem;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;

public class CapturedPhantomItem extends RenderedLivingEntityItem<Phantom> {
	public CapturedPhantomItem(Properties properties) {
		super(properties, EntityType.PHANTOM, CapturedPhantomItem::configurePhantom, 0.85f);
	}

	private static void configurePhantom(Phantom phantom) {
		phantom.setPhantomSize(0);
	}
}
