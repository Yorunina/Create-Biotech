package com.yision.allay.item.miniallay;

import java.util.function.Consumer;

import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.yision.allay.client.render.MiniAllayItemRenderer;

import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class IncompleteMiniAllayItem extends Item {
	public IncompleteMiniAllayItem(Properties properties) {
		super(properties);
	}

	@SuppressWarnings("removal")
	@Override
	@OnlyIn(Dist.CLIENT)
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(SimpleCustomRenderer.create(this, new MiniAllayItemRenderer(false)));
	}
}
