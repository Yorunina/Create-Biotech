package com.yision.allay.logistics.courier.hud;

import com.nobodiiiii.createbiotech.network.CBPackets;
import com.yision.allay.logistics.courier.AllayCourierTask;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AllayCourierHudSync {
	private static final int SYNC_INTERVAL_TICKS = 10;
	private static final Map<UUID, List<AllayCourierHudEntry>> LAST_SENT_BY_PLAYER = new HashMap<>();

	private AllayCourierHudSync() {}

	public static void clear() {
		LAST_SENT_BY_PLAYER.clear();
	}

	public static void sync(MinecraftServer server, List<AllayCourierTask> tasks) {
		if (server.getTickCount() % SYNC_INTERVAL_TICKS != 0) {
			return;
		}

		Map<UUID, List<AllayCourierHudEntry>> entriesByPlayer = new HashMap<>();
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
			AllayCourierHudEntry entry =
				new AllayCourierHudEntry(task.id(), task.hudIncoming(), task.hudCounterpartyAddress(), etaSeconds);
			entriesByPlayer.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(entry);
		}

		Set<UUID> onlinePlayers = new HashSet<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID playerId = player.getUUID();
			onlinePlayers.add(playerId);
			List<AllayCourierHudEntry> entries =
				new ArrayList<>(entriesByPlayer.getOrDefault(playerId, List.of()));
			entries.sort(Comparator.comparingInt(AllayCourierHudEntry::etaSeconds));
			if (entries.size() > AllayCourierHudPacket.MAX_VISIBLE_ENTRIES) {
				entries = List.copyOf(entries.subList(0, AllayCourierHudPacket.MAX_VISIBLE_ENTRIES));
			} else {
				entries = List.copyOf(entries);
			}

			if (!entries.equals(LAST_SENT_BY_PLAYER.get(playerId))) {
				CBPackets.sendToPlayer(new AllayCourierHudPacket(entries), player);
				LAST_SENT_BY_PLAYER.put(playerId, entries);
			}
		}

		LAST_SENT_BY_PLAYER.keySet().removeIf(playerId -> !onlinePlayers.contains(playerId));
	}
}
