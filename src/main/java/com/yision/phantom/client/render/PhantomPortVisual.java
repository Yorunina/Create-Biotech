package com.yision.phantom.client.render;

import java.util.function.Consumer;

import com.simibubi.create.content.logistics.FlapStuffs;
import com.yision.phantom.block.phantomport.PhantomPortBlock;
import com.yision.phantom.block.phantomport.PhantomPortBlockEntity;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Direction;

public class PhantomPortVisual extends AbstractBlockEntityVisual<PhantomPortBlockEntity>
	implements SimpleDynamicVisual {

	private final FlapStuffs.Visual flaps;

	public PhantomPortVisual(VisualizationContext context, PhantomPortBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);

		Direction facing = blockState.getValue(PhantomPortBlock.FACING);
		var commonTransform = FlapStuffs.commonTransform(getVisualPosition(), facing, 0);
		flaps = new FlapStuffs.Visual(instancerProvider(), commonTransform, PhantomPortRenderer.FLAP_PIVOT,
			Models.partial(PhantomPortRenderer.FLAP));
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
