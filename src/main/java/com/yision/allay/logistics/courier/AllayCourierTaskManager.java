package com.yision.allay.logistics.courier;

import com.yision.allay.entity.courier.AllayCourierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AllayCourierTaskManager {

	private static @Nullable AllayCourierTaskSavedData savedData;
	private static final Map<UUID, AllayCourierEntity> courierEntities = new HashMap<>();

	private AllayCourierTaskManager() {}

	public static void onServerStarting(MinecraftServer server) {
		courierEntities.values().forEach(entity -> {
			if (entity.isAlive()) {
				entity.discard();
			}
		});
		courierEntities.clear();
		savedData = AllayCourierTaskSavedData.getOrCreate(server);
	}

	public static void onServerTick(ServerTickEvent event) {
		if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
			return;
		}
		MinecraftServer server = event.getServer();
		if (savedData == null) {
			savedData = AllayCourierTaskSavedData.getOrCreate(server);
		}

		List<AllayCourierTask> tasks = savedData.getTasks();
		List<AllayCourierTask> completed = new ArrayList<>();

		for (AllayCourierTask task : tasks) {
			ServerLevel level = server.getLevel(task.currentDimension());
			AllayCourierEntity courier = findActiveCourier(task, level);
			if (courier == null && level != null && canShowEntity(level, task.position())) {
				courier = spawnCourier(level, task);
			}

			task.tick(server, courier);

			if (task.isRemoved()) {
				removeCourier(task);
				completed.add(task);
				continue;
			}
			if (task.relocatedThisTick()) {
				removeCourier(task);
				courier = null;
			}

			level = server.getLevel(task.currentDimension());
			if (level == null) {
				removeCourier(task);
				continue;
			}

			courier = findActiveCourier(task, level);
			if (canShowEntity(level, task.position())) {
				if (courier == null) {
					courier = spawnCourier(level, task);
				}
				if (courier != null) {
					syncCourierMetadata(courier, task);
				}
			} else {
				removeCourier(task);
			}
		}

		if (!completed.isEmpty()) {
			savedData.removeCompleted();
		} else if (!tasks.isEmpty()) {
			savedData.markDirty();
		}
	}

	public static void addTask(MinecraftServer server, AllayCourierTask task) {
		if (savedData == null) {
			savedData = AllayCourierTaskSavedData.getOrCreate(server);
		}
		savedData.addTask(task);
		ServerLevel level = server.getLevel(task.currentDimension());
		if (level != null && canShowEntity(level, task.position())) {
			spawnCourier(level, task);
		}
	}

	private static boolean canShowEntity(ServerLevel level, net.minecraft.world.phys.Vec3 pos) {
		return level.isPositionEntityTicking(BlockPos.containing(pos));
	}

	private static @Nullable AllayCourierEntity findActiveCourier(AllayCourierTask task,
		@Nullable ServerLevel level) {
		AllayCourierEntity existing = courierEntities.get(task.id());
		if (existing == null) {
			return null;
		}
		if (!existing.isAlive() || existing.level() != level) {
			if (existing.isAlive()) {
				existing.discard();
			}
			courierEntities.remove(task.id());
			return null;
		}
		return existing;
	}

	private static @Nullable AllayCourierEntity spawnCourier(ServerLevel level, AllayCourierTask task) {
		removeCourier(task);
		AllayCourierEntity courier = AllayCourierEntity.createFromTask(level, task);
		if (level.addFreshEntity(courier)) {
			courierEntities.put(task.id(), courier);
			return courier;
		}
		return null;
	}

	private static void removeCourier(AllayCourierTask task) {
		AllayCourierEntity entity = courierEntities.remove(task.id());
		if (entity != null && entity.isAlive()) {
			entity.discard();
		}
	}

	private static void syncCourierMetadata(AllayCourierEntity entity, AllayCourierTask task) {
		entity.setPackage(task.box());
		entity.setPhase(task.phase());
		entity.setMission(task.mission());
	}
}
