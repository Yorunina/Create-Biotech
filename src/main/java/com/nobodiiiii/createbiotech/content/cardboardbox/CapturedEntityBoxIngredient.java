package com.nobodiiiii.createbiotech.content.cardboardbox;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import com.nobodiiiii.createbiotech.registry.CBItems;

public class CapturedEntityBoxIngredient extends AbstractIngredient {

	private final Set<Item> items;
	private final EntityType<?> entityType;

	@Nullable
	private ItemStack[] displayStacks;
	@Nullable
	private IntList stackingIds;

	private CapturedEntityBoxIngredient(Set<Item> items, EntityType<?> entityType) {
		this.items = Collections.unmodifiableSet(items);
		this.entityType = entityType;
	}

	public static CapturedEntityBoxIngredient of(EntityType<?> entityType) {
		return new CapturedEntityBoxIngredient(defaultItems(), entityType);
	}

	public static CapturedEntityBoxIngredient of(EntityType<?> entityType, Item... items) {
		return new CapturedEntityBoxIngredient(Arrays.stream(items).collect(Collectors.toSet()), entityType);
	}

	public EntityType<?> getEntityType() {
		return entityType;
	}

	private static Set<Item> defaultItems() {
		return Set.of(CBItems.CARDBOARD_BOX.get(), CBItems.LARGE_CARDBOARD_BOX.get());
	}

	private void dissolve() {
		if (displayStacks != null)
			return;

		displayStacks = items.stream()
			.map(item -> CapturedEntityBoxHelper.createFilledBox(item, entityType))
			.toArray(ItemStack[]::new);
	}

	@Override
	public ItemStack[] getItems() {
		dissolve();
		return displayStacks;
	}

	@Override
	public boolean test(@Nullable ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return false;
		if (!items.contains(stack.getItem()))
			return false;
		return CapturedEntityBoxHelper.containsEntityType(stack, entityType);
	}

	@Override
	public IntList getStackingIds() {
		if (stackingIds == null || checkInvalidation()) {
			markValid();
			dissolve();
			stackingIds = new IntArrayList(displayStacks.length);
			for (ItemStack stack : displayStacks)
				stackingIds.add(StackedContents.getStackingIndex(stack));
			stackingIds.sort(IntComparators.NATURAL_COMPARATOR);
		}
		return stackingIds;
	}

	@Override
	protected void invalidate() {
		displayStacks = null;
		stackingIds = null;
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public IIngredientSerializer<? extends Ingredient> getSerializer() {
		return Serializer.INSTANCE;
	}

	@Override
	public JsonElement toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("type", CraftingHelper.getID(Serializer.INSTANCE).toString());
		ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
		if (entityId != null)
			json.addProperty("entity", entityId.toString());

		if (items.size() == 1) {
			ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(items.iterator().next());
			if (itemId != null)
				json.addProperty("item", itemId.toString());
		} else {
			JsonArray itemArray = new JsonArray();
			items.stream()
				.map(ForgeRegistries.ITEMS::getKey)
				.filter(Objects::nonNull)
				.sorted()
				.forEach(id -> itemArray.add(id.toString()));
			json.add("items", itemArray);
		}

		return json;
	}

	public static class Serializer implements IIngredientSerializer<CapturedEntityBoxIngredient> {
		public static final Serializer INSTANCE = new Serializer();

		@Override
		public CapturedEntityBoxIngredient parse(JsonObject json) {
			ResourceLocation entityId = new ResourceLocation(GsonHelper.getAsString(json, "entity"));
			EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
			if (entityType == null)
				throw new JsonSyntaxException("Unknown entity type: " + entityId);

			Set<Item> items = parseItems(json);
			return new CapturedEntityBoxIngredient(items, entityType);
		}

		@Override
		public CapturedEntityBoxIngredient parse(FriendlyByteBuf buffer) {
			Set<Item> items = Stream.generate(() -> buffer.readRegistryIdUnsafe(ForgeRegistries.ITEMS))
				.limit(buffer.readVarInt())
				.collect(Collectors.toSet());
			ResourceLocation entityId = buffer.readResourceLocation();
			EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
			if (entityType == null)
				throw new IllegalArgumentException("Unknown entity type: " + entityId);
			return new CapturedEntityBoxIngredient(items, entityType);
		}

		@Override
		public void write(FriendlyByteBuf buffer, CapturedEntityBoxIngredient ingredient) {
			buffer.writeVarInt(ingredient.items.size());
			for (Item item : ingredient.items)
				buffer.writeRegistryIdUnsafe(ForgeRegistries.ITEMS, item);

			ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(ingredient.entityType);
			if (entityId == null)
				throw new IllegalArgumentException("Unregistered entity type: " + ingredient.entityType);
			buffer.writeResourceLocation(entityId);
		}

		private static Set<Item> parseItems(JsonObject json) {
			if (json.has("item"))
				return Set.of(requireBoxItem(CraftingHelper.getItem(GsonHelper.getAsString(json, "item"), true)));

			if (json.has("items")) {
				ImmutableSet.Builder<Item> builder = ImmutableSet.builder();
				JsonArray itemArray = GsonHelper.getAsJsonArray(json, "items");
				for (int i = 0; i < itemArray.size(); i++) {
					builder.add(requireBoxItem(
						CraftingHelper.getItem(GsonHelper.convertToString(itemArray.get(i), "items[" + i + "]"), true)));
				}
				return builder.build();
			}

			return defaultItems();
		}

		private static Item requireBoxItem(Item item) {
			if (!(item instanceof CapturedEntityBoxItem))
				throw new JsonSyntaxException("Item " + ForgeRegistries.ITEMS.getKey(item)
					+ " is not a captured entity box item");
			return item;
		}
	}
}
