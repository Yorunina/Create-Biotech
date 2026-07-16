package com.yision.allay.logistics.courier.hud;

import com.nobodiiiii.createbiotech.network.CBPackets;
import com.yision.allay.logistics.courier.AllayCourierTask;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AllayCourierHudSync {
	private static final int SYNC_INTERVAL_TICKS = 10;
	private static final Map<UUID, List<Integer>> LAST_SENT_BY_PLAYER = new HashMap<>();

	private AllayCourierHudSync() {}

	public static void clear() {
		LAST_SENT_BY_PLAYER.clear();
	}

	public static void sync(MinecraftServer server, List<AllayCourierTask> tasks) {
		if (server.getTickCount() % SYNC_INTERVAL_TICKS != 0) {
			return;
		}

		Map<UUID, List<Integer>> etaByPlayer = new HashMap<>();
		for (AllayCourierTask task : tasks) {
			if (task.isRemoved()) {
				continue;
			}
			UUID playerId = task.hudTrackingPlayerId();
			if (playerId == null) {
				continue;
			}
			int remainingTicks = task.estimateRemainingTicks(server);
			if (remainingTicks < 0) {
				continue;
			}
			int etaSeconds = Math.max(0, (remainingTicks + 19) / 20);
			etaByPlayer.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(etaSeconds);
		}

		Set<UUID> onlinePlayers = new HashSet<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID playerId = player.getUUID();
			onlinePlayers.add(playerId);
			List<Integer> etaSeconds = new ArrayList<>(etaByPlayer.getOrDefault(playerId, List.of()));
			etaSeconds.sort(Integer::compareTo);
			if (etaSeconds.size() > AllayCourierHudPacket.MAX_VISIBLE_ENTRIES) {
				etaSeconds = List.copyOf(etaSeconds.subList(0, AllayCourierHudPacket.MAX_VISIBLE_ENTRIES));
			} else {
				etaSeconds = List.copyOf(etaSeconds);
			}

			if (!etaSeconds.equals(LAST_SENT_BY_PLAYER.get(playerId))) {
				CBPackets.sendToPlayer(new AllayCourierHudPacket(etaSeconds), player);
				LAST_SENT_BY_PLAYER.put(playerId, etaSeconds);
			}
		}

		LAST_SENT_BY_PLAYER.keySet().removeIf(playerId -> !onlinePlayers.contains(playerId));
	}
}
