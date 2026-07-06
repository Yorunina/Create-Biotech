package com.yision.phantom.block.phantomport;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.yision.phantom.item.miniphantom.MiniPhantomItem;
import com.yision.phantom.logistics.address.PhantomAddressRules;
import com.yision.phantom.logistics.courier.AirCourierDispatchService;
import com.yision.phantom.logistics.courier.AirCourierHelper;
import com.yision.phantom.logistics.courier.AirCourierTarget;
import com.yision.phantom.logistics.courier.hud.AirCourierHudSync;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class PhantomPortDispatchAccess {

	private final PhantomPortBlockEntity port;
	private final PhantomPortInventory inventory;
	private final PhantomPortBeltAccess beltAccess;

	private final Map<Integer, PendingHudEntry> pendingHudEntries = new HashMap<>();

	PhantomPortDispatchAccess(PhantomPortBlockEntity port,
							  PhantomPortInventory inventory,
							  PhantomPortBeltAccess beltAccess) {
		this.port = port;
		this.inventory = inventory;
		this.beltAccess = beltAccess;
	}

	@Nullable IItemHandler getItemHandler(@Nullable net.minecraft.core.Direction side) {
		return inventory.combinedHandler();
	}

	void clearPendingHudEntries() {
		pendingHudEntries.clear();
	}

	boolean tryDispatchToLaunchBelt() {
		if (!(port.getLevel() instanceof ServerLevel serverLevel)) {
			return false;
		}
		if (!beltAccess.canLaunchFromBelt() || !inventory.hasStoredCarrier()) {
			return false;
		}

		DispatchCandidate candidate = findDispatchCandidate(serverLevel);
		if (candidate == null) {
			return false;
		}
		ItemStack phantomStack = candidate.phantomStack().copy();
		if (!beltAccess.canAcceptLaunchStack(phantomStack)) {
			return false;
		}

		ItemStack extractedPackage = port.inventory.extractItem(candidate.packageSlot(), 1, false);
		ItemStack extractedCarrier = inventory.extractOneCarrier(false);
		if (extractedPackage.isEmpty() || extractedCarrier.isEmpty()) {
			restoreFailedDispatch(candidate.packageSlot(), extractedPackage, extractedCarrier);
			return false;
		}
		if (!beltAccess.insertToLaunchBelt(phantomStack)) {
			restoreFailedDispatch(candidate.packageSlot(), extractedPackage, extractedCarrier);
			return false;
		}

		notifyPreparingPlayer(serverLevel, candidate, phantomStack);
		pendingHudEntries.remove(candidate.packageSlot());
		port.markPortContentsChanged();
		return true;
	}

	private void restoreFailedDispatch(int packageSlot, ItemStack extractedPackage, ItemStack extractedCarrier) {
		if (!extractedPackage.isEmpty()) {
			ItemStack remainder = port.inventory.insertItem(packageSlot, extractedPackage, false);
			if (!remainder.isEmpty()) {
				port.drop(remainder);
			}
		}
		if (!extractedCarrier.isEmpty()) {
			inventory.returnCarrier(extractedCarrier);
		}
		port.markPortContentsChanged();
	}

	private void notifyPreparingPlayer(ServerLevel serverLevel, DispatchCandidate candidate, ItemStack phantomStack) {
		if (!(candidate.target() instanceof AirCourierTarget.PlayerTarget playerTarget)) {
			return;
		}
		ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(playerTarget.playerId());
		UUID hudEntryId = candidate.hudEntryId();
		if (player == null || hudEntryId == null) {
			return;
		}
		AirCourierHudSync.onCourierPreparing(player, MiniPhantomItem.copyCargoPackage(phantomStack), hudEntryId);
	}

	private @Nullable DispatchCandidate findDispatchCandidate(ServerLevel serverLevel) {
		Direction heading = beltAccess.resolveBeltHeading();
		int headingAngle = AirCourierHelper.getHeadingAngle(heading);
		String filterString = port.getFilterString();
		for (int slot = 0; slot < port.inventory.getSlots(); slot++) {
			ItemStack packageInSlot = port.inventory.getStackInSlot(slot);
			if (packageInSlot.isEmpty() || !PackageItem.isPackage(packageInSlot)) {
				continue;
			}
			if (filterString != null && !PhantomAddressRules.isBlank(filterString)
				&& PhantomAddressRules.matchesPackage(packageInSlot, filterString)) {
				continue;
			}
			AirCourierTarget target = AirCourierDispatchService.resolvePackageTarget(serverLevel, packageInSlot,
				Vec3.atCenterOf(port.getBlockPos()), serverLevel.dimension(), port.getBlockPos());
			if (target == null) {
				continue;
			}

			ItemStack singlePackage = packageInSlot.copy();
			singlePackage.setCount(1);
			ItemStack phantomStack = MiniPhantomItem.createLoadedWithHeading(singlePackage, headingAngle);
			MiniPhantomItem.setReturnTarget(phantomStack, serverLevel.dimension(), port.getBlockPos());
			MiniPhantomItem.setReturnMode(phantomStack, port.getReturnMode());

			UUID hudEntryId = null;
			if (target instanceof AirCourierTarget.PlayerTarget) {
				hudEntryId = getOrCreatePendingHudEntryId(slot, singlePackage);
				MiniPhantomItem.setHudEntryId(phantomStack, hudEntryId);
			}
			return new DispatchCandidate(slot, phantomStack, target, hudEntryId);
		}
		return null;
	}

	private UUID getOrCreatePendingHudEntryId(int slot, ItemStack packageStack) {
		PendingHudEntry existing = pendingHudEntries.get(slot);
		if (existing != null && ItemStack.matches(existing.packageSnapshot(), packageStack)) {
			return existing.hudEntryId();
		}
		UUID newId = UUID.randomUUID();
		pendingHudEntries.put(slot, new PendingHudEntry(newId, packageStack.copy()));
		return newId;
	}

	private record PendingHudEntry(UUID hudEntryId, ItemStack packageSnapshot) {}

	private record DispatchCandidate(int packageSlot, ItemStack phantomStack,
									 @Nullable AirCourierTarget target, @Nullable UUID hudEntryId) {}
}
