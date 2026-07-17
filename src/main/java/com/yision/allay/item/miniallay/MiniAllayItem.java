package com.yision.allay.item.miniallay;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.yision.allay.client.render.MiniAllayItemRenderer;
import com.yision.allay.entity.courier.AllayCourierEntity;
import com.yision.allay.registry.AllItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MiniAllayItem extends Item {
	private static final int EMPTY_CARRIER_MAX_STACK_SIZE = 64;
	private static final String CARGO_KEY = "Cargo";
	private static final String HEADING_KEY = "Heading";
	private static final double PLACED_COURIER_Y_OFFSET = 0.01d + 2.0d / 16.0d;

	public MiniAllayItem(Properties properties) {
		super(properties);
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return isPlainCarrier(stack) ? EMPTY_CARRIER_MAX_STACK_SIZE : 1;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
		ItemStack stack = player.getItemInHand(usedHand);
		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
			openMenu(serverPlayer, stack, usedHand);
		}
		return InteractionResultHolder.success(stack);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		if (!hasCargo(stack) || context.getClickedFace() != Direction.UP) {
			return InteractionResult.PASS;
		}

		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}

		Vec3 spawnPos = Vec3.atBottomCenterOf(context.getClickedPos().above())
			.add(0, PLACED_COURIER_Y_OFFSET, 0);
		Vec3 facingDirection = player.getLookAngle().multiply(1, 0, 1);
		if (facingDirection.lengthSqr() < 1.0E-6) {
			facingDirection = Vec3.directionFromRotation(0, player.getYRot()).multiply(-1, 0, -1);
		}
		facingDirection = facingDirection.normalize();
		AllayCourierEntity courier = AllayCourierEntity.createWaiting(level, copyCargoPackage(stack), facingDirection);
		courier.setPos(spawnPos);

		if (!level.noCollision(courier, courier.getBoundingBox())) {
			return InteractionResult.FAIL;
		}

		if (!level.isClientSide()) {
			level.addFreshEntity(courier);
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
		TooltipFlag tooltipFlag) {
		ItemStack cargoPackage = copyCargoPackage(stack);
		if (!cargoPackage.isEmpty()) {
			cargoPackage.getItem().appendHoverText(cargoPackage, level, tooltipComponents, tooltipFlag);
		}
	}

	protected static void openMenu(ServerPlayer serverPlayer, ItemStack stack, InteractionHand usedHand) {
		LegacyMiniAllayClipboardData.recoverClipboard(serverPlayer);
		NetworkHooks.openScreen(serverPlayer,
			new SimpleMenuProvider((id, inv, p) -> MiniAllayMenu.create(id, inv, stack, usedHand),
				Component.translatable("item.create_biotech.mini_allay")),
			buffer -> {
				buffer.writeItem(stack);
				buffer.writeEnum(usedHand);
			});
	}

	public static ItemStack createLoaded(ItemStack packageStack) {
		ItemStack allay = AllItems.MINI_ALLAY.asStack();
		loadCargo(allay, packageStack);
		return allay;
	}

	public static boolean loadCargo(ItemStack allay, ItemStack packageStack) {
		MiniAllayCargo cargo = new MiniAllayCargo(packageStack);
		if (!cargo.isValid()) {
			remove(allay, CARGO_KEY);
			return false;
		}

		allay.getOrCreateTag().put(CARGO_KEY, cargo.packageCopy().save(new CompoundTag()));
		return true;
	}

	public static ItemStack copyCargoPackage(ItemStack allay) {
		CompoundTag tag = allay.getTag();
		if (tag == null || !tag.contains(CARGO_KEY, Tag.TAG_COMPOUND)) {
			return ItemStack.EMPTY;
		}
		ItemStack packageStack = ItemStack.of(tag.getCompound(CARGO_KEY));
		return PackageItem.isPackage(packageStack) ? packageStack.copy() : ItemStack.EMPTY;
	}

	public static boolean hasCargo(ItemStack allay) {
		return !copyCargoPackage(allay).isEmpty();
	}

	public static boolean isPlainCarrier(ItemStack stack) {
		return stack.is(AllItems.MINI_ALLAY.get())
			&& !hasCargo(stack);
	}

	public static void clearCargo(ItemStack allay) {
		remove(allay, CARGO_KEY);
	}

	public static void setHeadingAngle(ItemStack stack, int headingAngle) {
		stack.getOrCreateTag().putInt(HEADING_KEY, Math.floorMod(headingAngle, 360));
	}

	public static boolean hasHeadingAngle(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(HEADING_KEY, Tag.TAG_INT);
	}

	public static int getHeadingAngle(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : Math.floorMod(tag.getInt(HEADING_KEY), 360);
	}

	private static void remove(ItemStack stack, String key) {
		CompoundTag tag = stack.getTag();
		if (tag != null) {
			tag.remove(key);
		}
	}

	@SuppressWarnings("removal")
	@Override
	@OnlyIn(Dist.CLIENT)
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(SimpleCustomRenderer.create(this, new MiniAllayItemRenderer()));
	}
}
