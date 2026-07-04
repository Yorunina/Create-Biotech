package com.yision.phantom.block.phantomport;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PhantomPortWakeupHandler {
	private PhantomPortWakeupHandler() {}

	public static void tryWakeAdjacentPorts(PackagerBlockEntity packager) {
		if (packager.getLevel() == null || packager.getLevel().isClientSide()) {
			return;
		}

		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockEntity blockEntity = packager.getLevel().getBlockEntity(packager.getBlockPos().relative(direction));
			if (!(blockEntity instanceof PhantomPortBlockEntity port)) {
				continue;
			}
			if (port.getLaunchSide() != direction) {
				continue;
			}
			port.tryPullFromPackagerSide();
		}
	}
}
