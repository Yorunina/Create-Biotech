package com.yision.allay.client.render;

import java.util.function.Consumer;

import com.simibubi.create.content.logistics.FlapStuffs;
import com.yision.allay.block.allayport.AllayPortBlock;
import com.yision.allay.block.allayport.AllayPortBlockEntity;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Direction;

public class AllayPortVisual extends AbstractBlockEntityVisual<AllayPortBlockEntity>
	implements SimpleDynamicVisual {

	private final FlapStuffs.Visual flaps;

	public AllayPortVisual(VisualizationContext context, AllayPortBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);

		Direction facing = blockState.getValue(AllayPortBlock.FACING);
		var commonTransform = FlapStuffs.commonTransform(getVisualPosition(), facing, 0);
		flaps = new FlapStuffs.Visual(instancerProvider(), commonTransform, AllayPortRenderer.FLAP_PIVOT,
			Models.partial(AllayPortRenderer.FLAP));
		flaps.update(blockEntity.getFlap(partialTick));
	}

	@Override
	public void beginFrame(Context ctx) {
		flaps.update(blockEntity.getFlap(ctx.partialTick()));
	}

	@Override
	public void updateLight(float partialTick) {
		flaps.updateLight(computePackedLight());
	}

	@Override
	protected void _delete() {
		flaps.delete();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		flaps.collectCrumblingInstances(consumer);
	}
}
