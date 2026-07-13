package com.yision.phantom.logistics.courier.flight;

import com.yision.phantom.block.phantomport.PhantomPortBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class AirCourierFlightEstimate {

	private AirCourierFlightEstimate() {}

	public static int cruiseAndLandingTicks(AirCourierFlightProfile profile,
		Vec3 from, Vec3 cruiseTarget, Vec3 landingTarget, double completionDistance, boolean playerTarget) {
		return cruiseAndLandingTicks(profile, from, cruiseTarget, landingTarget, completionDistance,
			playerTarget, null);
	}

	public static int cruiseAndLandingTicks(AirCourierFlightProfile profile,
		Vec3 from, Vec3 cruiseTarget, Vec3 landingTarget, double completionDistance, boolean playerTarget,
		@Nullable Direction horizontalEntryFacing) {
		Vec3 initialMotion = cruiseTarget.subtract(from);
		Vec3 approachGate = AirCourierFlightTargets.approachGate(profile, from, initialMotion, landingTarget,
			playerTarget, horizontalEntryFacing);
		boolean horizontalEntry = horizontalEntryFacing != null;
		boolean alreadyLanding = from.distanceTo(approachGate) < profile.approachGateNearDistance()
			|| (!horizontalEntry && AirCourierFlightMath.horizontalDistance(from, landingTarget) < profile.approachGateHorizontalThreshold()
				&& from.y > landingTarget.y)
			|| (playerTarget && from.distanceTo(landingTarget) < profile.playerCompletionDistance());
		if (alreadyLanding) {
			return landingTicks(profile, from, landingTarget, completionDistance, horizontalEntry);
		}

		double cruiseDistance = Math.max(0, from.distanceTo(approachGate) - profile.approachGateNearDistance());
		int cruiseTicks = Mth.ceil(cruiseDistance / profile.cruiseSpeed());
		return cruiseTicks + landingTicks(profile, approachGate, landingTarget, completionDistance, horizontalEntry);
	}

	public static int landingTicks(AirCourierFlightProfile profile,
		Vec3 from, Vec3 landingTarget, double completionDistance) {
		return landingTicks(profile, from, landingTarget, completionDistance, false);
	}

	public static int landingTicks(AirCourierFlightProfile profile,
		Vec3 from, Vec3 landingTarget, double completionDistance, boolean horizontalEntry) {
		double radius = horizontalEntry
			? AirCourierFlightPlanner.faceEntryDockingRadius(completionDistance)
			: completionDistance;
		double landingDistance = Math.max(0, from.distanceTo(landingTarget) - radius);
		return Mth.ceil(landingDistance / profile.landingSpeed());
	}

	public static int estimateInboundTicks(AirCourierFlightProfile profile,
		Vec3 reentryPos, @Nullable PhantomPortBlockEntity targetPhantomPort, @Nullable ServerPlayer targetPlayer) {
		Vec3 cruiseTarget = AirCourierFlightTargets.cruiseTarget(profile, targetPhantomPort, targetPlayer);
		Vec3 landingTarget = AirCourierFlightTargets.landingTarget(profile, targetPhantomPort, targetPlayer,
			targetPhantomPort != null);
		double completionDistance = AirCourierFlightTargets.completionDistance(profile, targetPhantomPort, targetPlayer);
		return cruiseAndLandingTicks(profile, reentryPos, cruiseTarget, landingTarget, completionDistance,
			targetPlayer != null,
			targetPhantomPort != null ? AirCourierFlightTargets.phantomPortFacing(targetPhantomPort) : null);
	}

}
