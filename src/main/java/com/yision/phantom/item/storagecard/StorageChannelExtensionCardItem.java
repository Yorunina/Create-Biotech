package com.yision.phantom.item.storagecard;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.clipboard.ClipboardBlockEntity;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class StorageChannelExtensionCardItem extends Item {

	private static final String NETWORK_TAG = "PhantomStorageChannelFreq";
	private static final String CATEGORIES_TAG = "PhantomStorageChannelCategories";
	private static final String ADDRESSES_TAG = "PhantomStorageChannelAddresses";

	public StorageChannelExtensionCardItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public boolean isFoil(@NotNull ItemStack stack) {
		return isLinked(stack);
	}

	public static boolean isLinked(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.hasUUID(NETWORK_TAG);
	}

	@Nullable
	public static UUID networkFromStack(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.hasUUID(NETWORK_TAG))
			return null;
		return tag.getUUID(NETWORK_TAG);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
		@NotNull TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);

		List<String> addresses = loadAddressesFromStack(stack);
		if (!addresses.isEmpty()) {
			tooltip.add(Component.translatable("item.create_biotech.storage_channel_extension_card.address_count",
				addresses.size()).withStyle(FontHelper.Palette.STANDARD_CREATE.highlight()));
		}

		if (!isLinked(stack))
			return;

		CreateLang.translate("logistically_linked.tooltip")
			.style(ChatFormatting.GOLD)
			.addTo(tooltip);
		CreateLang.translate("logistically_linked.tooltip_clear")
			.style(ChatFormatting.GRAY)
			.addTo(tooltip);
	}

	@Override
	public @NotNull InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null)
			return InteractionResult.FAIL;

		ItemStack stack = context.getItemInHand();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		if (level.getBlockEntity(pos) instanceof ClipboardBlockEntity clipboard) {
			if (level.isClientSide)
				return InteractionResult.SUCCESS;
			int written = saveAddressesFromClipboard(stack, clipboard.dataContainer);
			player.displayClientMessage(
				Component.translatable("item.create_biotech.storage_channel_extension_card.address_count", written),
				true);
			return InteractionResult.SUCCESS;
		}

		LogisticallyLinkedBehaviour link = BlockEntityBehaviour.get(level, pos, LogisticallyLinkedBehaviour.TYPE);
		if (link == null)
			return InteractionResult.PASS;

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		if (!link.mayInteractMessage(player))
			return InteractionResult.SUCCESS;

		UUID oldNetwork = networkFromStack(stack);
		UUID newNetwork = link.freqId;
		saveCategoriesIfAvailable(stack, level, pos, oldNetwork, newNetwork);
		assignFrequency(stack, player, newNetwork);
		return InteractionResult.SUCCESS;
	}

	public static void assignFrequency(ItemStack stack, Player player, UUID frequency) {
		stack.getOrCreateTag()
			.putUUID(NETWORK_TAG, frequency);
		player.displayClientMessage(CreateLang.translate("logistically_linked.tuned")
			.component(), true);
	}

	private static void saveCategoriesIfAvailable(ItemStack stack, Level level, BlockPos pos, @Nullable UUID oldNetwork,
		UUID newNetwork) {
		if (level.getBlockEntity(pos) instanceof StockTickerBlockEntity stockTicker) {
			CompoundTag tag = stockTicker.saveWithFullMetadata();
			List<ItemStack> categories = NBTHelper.readItemList(tag.getList("Categories", Tag.TAG_COMPOUND));
			saveCategoriesToStack(stack, categories);
			return;
		}

		if (oldNetwork != null && !oldNetwork.equals(newNetwork))
			clearTag(stack, CATEGORIES_TAG);
	}

	public static void saveCategoriesToStack(ItemStack stack, List<ItemStack> categories) {
		if (categories == null || categories.isEmpty()) {
			clearTag(stack, CATEGORIES_TAG);
			return;
		}

		stack.getOrCreateTag()
			.put(CATEGORIES_TAG, NBTHelper.writeItemList(categories));
	}

	public static List<ItemStack> loadCategoriesFromStack(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(CATEGORIES_TAG, Tag.TAG_LIST))
			return new ArrayList<>();

		List<ItemStack> categories = NBTHelper.readItemList(tag.getList(CATEGORIES_TAG, Tag.TAG_COMPOUND));
		categories.removeIf(itemStack -> !itemStack.isEmpty() && !(itemStack.getItem() instanceof FilterItem));
		return categories;
	}

	public static int saveAddressesFromClipboard(ItemStack stack, ItemStack clipboard) {
		List<String> addresses = extractAddresses(clipboard);
		saveAddressesToStack(stack, addresses);
		return addresses.size();
	}

	public static void saveAddressesToStack(ItemStack stack, List<String> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			clearTag(stack, ADDRESSES_TAG);
			return;
		}

		ListTag listTag = new ListTag();
		for (String address : addresses)
			listTag.add(StringTag.valueOf(address));
		stack.getOrCreateTag()
			.put(ADDRESSES_TAG, listTag);
	}

	public static List<String> loadAddressesFromStack(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(ADDRESSES_TAG, Tag.TAG_LIST))
			return new ArrayList<>();

		ListTag listTag = tag.getList(ADDRESSES_TAG, Tag.TAG_STRING);
		List<String> addresses = new ArrayList<>(listTag.size());
		for (int i = 0; i < listTag.size(); i++) {
			String address = listTag.getString(i);
			if (!address.isBlank())
				addresses.add(address);
		}
		return addresses;
	}

	public static List<String> extractAddresses(ItemStack clipboard) {
		Set<String> added = new LinkedHashSet<>();
		for (List<ClipboardEntry> page : ClipboardEntry.readAll(clipboard)) {
			for (ClipboardEntry entry : page) {
				if (entry.checked)
					continue;
				String text = entry.text.getString();
				if (!text.startsWith("#") || text.length() == 1)
					continue;
				String address = text.substring(1)
					.trim();
				if (!address.isBlank())
					added.add(address);
			}
		}
		return List.copyOf(added);
	}

	private static void clearTag(ItemStack stack, String key) {
		CompoundTag tag = stack.getTag();
		if (tag == null)
			return;
		tag.remove(key);
		if (tag.isEmpty())
			stack.setTag(null);
	}
}
