package com.nobodiiiii.createbiotech.mixin;

import java.util.Comparator;
import java.util.Optional;

import com.nobodiiiii.createbiotech.registry.CBFluids;
import com.nobodiiiii.createbiotech.registry.CBPoiTypes;

import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalForcer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {

	@Shadow
	@Final
	protected ServerLevel level;

	@Inject(method = "findPortalAround", at = @At("RETURN"), cancellable = true)
	private void createBiotech$includeTeleportationFluid(BlockPos searchOrigin, boolean isNether,
		WorldBorder worldBorder, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
		int searchRadius = isNether ? 16 : 128;
		Optional<PoiRecord> fluidPortal = level.getPoiManager()
			.getInSquare(holder -> holder.is(CBPoiTypes.TELEPORTATION_KEY), searchOrigin, searchRadius,
				PoiManager.Occupancy.ANY)
			.filter(record -> worldBorder.isWithinBounds(record.getPos()))
			.filter(record -> level.getFluidState(record.getPos())
				.getType()
				.isSame(CBFluids.TELEPORTATION.get()))
			.sorted(Comparator.<PoiRecord>comparingDouble(record -> record.getPos()
				.distSqr(searchOrigin))
				.thenComparingInt(record -> record.getPos()
					.getY()))
			.findFirst();
		if (fluidPortal.isEmpty())
			return;

		BlockPos fluidPos = fluidPortal.get()
			.getPos();
		Optional<BlockUtil.FoundRectangle> existingPortal = cir.getReturnValue();
		if (existingPortal != null
			&& existingPortal.isPresent()
			&& createBiotech$compareToRectangle(fluidPos, existingPortal.get(), searchOrigin) >= 0) {
			return;
		}

		level.getChunkSource()
			.addRegionTicket(TicketType.PORTAL, new ChunkPos(fluidPos), 3, fluidPos);
		cir.setReturnValue(Optional.of(new BlockUtil.FoundRectangle(fluidPos, 1, 1)));
	}

	@Unique
	private int createBiotech$compareToRectangle(BlockPos fluidPos, BlockUtil.FoundRectangle rectangle,
		BlockPos searchOrigin) {
		BlockPos corner = rectangle.minCorner;
		BlockState state = level.getBlockState(corner);
		Direction.Axis horizontalAxis = state.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS)
			.orElse(Direction.Axis.X);

		int closestX = corner.getX();
		int closestZ = corner.getZ();
		if (horizontalAxis == Direction.Axis.X) {
			closestX = Mth.clamp(searchOrigin.getX(), corner.getX(),
				corner.getX() + rectangle.axis1Size - 1);
		} else {
			closestZ = Mth.clamp(searchOrigin.getZ(), corner.getZ(),
				corner.getZ() + rectangle.axis1Size - 1);
		}
		int closestY = Mth.clamp(searchOrigin.getY(), corner.getY(),
			corner.getY() + rectangle.axis2Size - 1);
		BlockPos closestExistingPos = new BlockPos(closestX, closestY, closestZ);

		int distanceComparison = Double.compare(fluidPos.distSqr(searchOrigin),
			closestExistingPos.distSqr(searchOrigin));
		return distanceComparison != 0
			? distanceComparison
			: Integer.compare(fluidPos.getY(), closestExistingPos.getY());
	}
}
