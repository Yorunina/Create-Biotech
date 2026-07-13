package com.yision.phantom.logistics.courier.flight;

import com.yision.phantom.block.phantomport.PhantomPortBlock;
import com.yision.phantom.block.phantomport.PhantomPortBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class AirCourierFlightTargets {

	private AirCourierFlightTargets() {}

	public static Vec3 cruiseTarget(AirCourierFlightProfile profile,
		@Nullable PhantomPortBlockEntity phantomPort, @Nullable ServerPlayer player) {
		if (phantomPort != null) {
			return Vec3.atCenterOf(phantomPort.getBlockPos()).add(0, profile.phantomPortCruiseHeight(), 0);
		}
		if (player != null) {
			return playerDeliveryTarget(profile, player).add(0, profile.playerCruiseHeight() - profile.playerTargetHeight(), 0);
		}
		return Vec3.ZERO;
	}

	public static Vec3 landingTarget(AirCourierFlightProfile profile,
		@Nullable PhantomPortBlockEntity phantomPort, @Nullable ServerPlayer player) {
		return landingTarget(profile, phantomPort, player, false);
	}

	public static Vec3 landingTarget(AirCourierFlightProfile profile,
		@Nullable PhantomPortBlockEntity phantomPort, @Nullable ServerPlayer player,
		boolean useFacingFaceCenter) {
		if (phantomPort != null) {
			if (useFacingFaceCenter) {
				return phantomPortFacingFaceCenter(phantomPort);
			}
			return Vec3.atCenterOf(phantomPort.getBlockPos()).add(0, profile.phantomPortLandingHeight(), 0);
		}
		if (player != null) {
			return playerDeliveryTarget(profile, player);
		}
		return Vec3.ZERO;
	}

	public static Vec3 phantomPortFacingFaceCenter(PhantomPortBlockEntity phantomPort) {
		Direction facing = phantomPortFacing(phantomPort);
		return Vec3.atCenterOf(phantomPort.getBlockPos())
			.add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.5));
	}

	public static Direction phantomPortFacing(PhantomPortBlockEntity phantomPort) {
		return phantomPort.getBlockState().getValue(PhantomPortBlock.FACING);
	}

	public static double completionDistance(AirCourierFlightProfile profile,
		@Nullable PhantomPortBlockEntity phantomPort, @Nullable ServerPlayer player) {
		if (phantomPort != null) {
			return profile.phantomPortCompletionDistance();
		}
		return profile.playerCompletionDistance();
	}

	public static Vec3 playerDeliveryTarget(AirCourierFlightProfile profile, ServerPlayer player) {
		Vec3 horizontalLook = player.getLookAngle().multiply(1, 0, 1);
		if (horizontalLook.lengthSqr() > 1.0E-6) {
			horizontalLook = horizontalLook.normalize();
		} else {
			float bodyYaw = player.yBodyRot;
			float yawRad = bodyYaw * Mth.DEG_TO_RAD;
			horizontalLook = new Vec3(-Mth.sin(yawRad), 0, Mth.cos(yawRad));
		}
		return player.position()
			.add(0, profile.playerTargetHeight(), 0)
			.add(horizontalLook.scale(profile.playerForwardOffset()));
	}

	public static Vec3 approachGate(AirCourierFlightProfile profile, Vec3 currentPos,
		Vec3 currentMotion, Vec3 landingTarget, boolean playerTarget) {
		return approachGate(profile, currentPos, currentMotion, landingTarget, playerTarget, null);
	}

	public static Vec3 approachGate(AirCourierFlightProfile profile, Vec3 currentPos,
		Vec3 currentMotion, Vec3 landingTarget, boolean playerTarget,
		@Nullable Direction horizontalEntryFacing) {
		if (horizontalEntryFacing != null) {
			return horizontalEntryGate(profile, currentPos, landingTarget, horizontalEntryFacing);
		}

		double dx = currentPos.x - landingTarget.x;
		double dz = currentPos.z - landingTarget.z;
		double horizontalDist = Math.sqrt(dx * dx + dz * dz);
		Vec3 approachDir;
		if (horizontalDist < 1.0E-4) {
			Vec3 hMotion = new Vec3(currentMotion.x, 0, currentMotion.z);
			if (hMotion.lengthSqr() > 1.0E-6) {
				approachDir = hMotion.normalize();
			} else {
				approachDir = new Vec3(0, 0, 1);
			}
		} else {
			approachDir = new Vec3(dx / horizontalDist, 0, dz / horizontalDist);
		}

		double approachDistance = Mth.clamp(horizontalDist * 0.22, 6.0, 18.0);
		double approachHeight = playerTarget ? profile.playerApproachHeight() : profile.phantomPortApproachHeight();

		return landingTarget.add(approachDir.scale(approachDistance)).add(0, approachHeight, 0);
	}

	private static Vec3 horizontalEntryGate(AirCourierFlightProfile profile, Vec3 currentPos,
		Vec3 landingTarget, Direction facing) {
		Vec3 facingNormal = Vec3.atLowerCornerOf(facing.getNormal());
		Vec3 approachDir = new Vec3(facingNormal.x, 0, facingNormal.z);
		if (approachDir.lengthSqr() < 1.0E-6) {
			return landingTarget;
		}
		double horizontalDist = AirCourierFlightMath.horizontalDistance(currentPos, landingTarget);
		double approachDistance = Mth.clamp(horizontalDist * 0.22, 6.0, 18.0);
		return landingTarget.add(approachDir.normalize().scale(approachDistance))
			.add(0, profile.phantomPortApproachHeight(), 0);
	}
}
