package net.wafflingpenguin.wynnhorse.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.wafflingpenguin.wynnhorse.WynnHorseConfig;
import net.wafflingpenguin.wynnhorse.waypoint.Waypoint;
import net.wafflingpenguin.wynnhorse.waypoint.WaypointRoute;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class NavigationController {
    private float travelTargetPitch = Float.NaN;
    private int travelPitchRetargetTicksRemaining;

    public NavigationOutcome tick(final Minecraft minecraft, final WaypointRoute route) {
        if (minecraft.screen != null) {
            return NavigationOutcome.paused();
        }

        LocalPlayer player = minecraft.player;
        if (player == null) {
            return NavigationOutcome.noTarget();
        }

        List<Waypoint> waypoints = route.getWaypoints();
        int activeIndex = route.getActiveIndex();
        if (activeIndex < 0 || activeIndex >= waypoints.size()) {
            return NavigationOutcome.noTarget();
        }

        Waypoint waypoint = waypoints.get(activeIndex);
        double reachDistance = WynnHorseConfig.getWaypointReachedDistance();
        double currentDistance = horizontalDistance(player.position(), waypoint.position());
        if (currentDistance <= reachDistance) {
            return NavigationOutcome.reached(waypoint);
        }

        Waypoint nextWaypoint = this.resolveNextWaypoint(waypoints, activeIndex);
        SteeringProfile steeringProfile = this.resolveSteeringProfile(player.position(), waypoint.position(), nextWaypoint == null ? null : nextWaypoint.position());
        this.steerPlayer(player, steeringProfile.targetPosition(), steeringProfile.yawStepDegrees());
        return NavigationOutcome.navigating(waypoint);
    }

    public DirectNavigationOutcome tickTowardsPosition(final Minecraft minecraft, final Vec3 targetPosition, final double reachDistance) {
        return this.tickTowardsPosition(minecraft, targetPosition, reachDistance, WynnHorseConfig.getSteeringYawStepDegrees());
    }

    public DirectNavigationOutcome tickTowardsPosition(final Minecraft minecraft, final Vec3 targetPosition, final double reachDistance, final double yawStepDegrees) {
        if (minecraft.screen != null) {
            return DirectNavigationOutcome.paused();
        }

        LocalPlayer player = minecraft.player;
        if (player == null || targetPosition == null) {
            return DirectNavigationOutcome.noTarget();
        }

        if (horizontalDistance(player.position(), targetPosition) <= reachDistance) {
            return DirectNavigationOutcome.reached();
        }

        this.steerPlayer(player, targetPosition, yawStepDegrees);
        return DirectNavigationOutcome.navigating();
    }

    public void reset() {
        this.travelTargetPitch = Float.NaN;
        this.travelPitchRetargetTicksRemaining = 0;
    }

    private Waypoint resolveNextWaypoint(final List<Waypoint> waypoints, final int activeIndex) {
        if (waypoints.size() < 2) {
            return null;
        }

        return waypoints.get((activeIndex + 1) % waypoints.size());
    }

    private SteeringProfile resolveSteeringProfile(final Vec3 playerPosition, final Vec3 currentWaypointPosition, final Vec3 nextWaypointPosition) {
        if (nextWaypointPosition == null) {
            return new SteeringProfile(currentWaypointPosition, WynnHorseConfig.getSteeringYawStepDegrees());
        }

        double nextSegmentLength = horizontalDistance(currentWaypointPosition, nextWaypointPosition);
        double cornerAngleDegrees = cornerAngleDegrees(playerPosition, currentWaypointPosition, nextWaypointPosition);
        double dynamicStartDistance = this.dynamicTurnStartDistance(cornerAngleDegrees, nextSegmentLength);
        double dynamicYawStep = this.dynamicTurnYawStepDegrees(cornerAngleDegrees, nextSegmentLength);

        double cornerRadius = Math.max(dynamicStartDistance, WynnHorseConfig.getWaypointReachedDistance());
        double currentDistance = horizontalDistance(playerPosition, currentWaypointPosition);
        if (currentDistance >= cornerRadius) {
            return new SteeringProfile(currentWaypointPosition, dynamicYawStep);
        }

        Vec3 currentDirection = horizontalDirection(playerPosition, currentWaypointPosition);
        Vec3 nextDirection = horizontalDirection(currentWaypointPosition, nextWaypointPosition);
        if (currentDirection == null || nextDirection == null) {
            return new SteeringProfile(currentWaypointPosition, dynamicYawStep);
        }

        double blend = Mth.clamp(1.0D - currentDistance / cornerRadius, 0.0D, 1.0D);
        blend *= blend;

        Vec3 blendedDirection = currentDirection.scale(1.0D - blend).add(nextDirection.scale(blend));
        if (blendedDirection.lengthSqr() < 1.0E-6D) {
            return new SteeringProfile(currentWaypointPosition, dynamicYawStep);
        }

        Vec3 normalizedBlendedDirection = blendedDirection.normalize();
        double projectionDistance = Math.max(currentDistance, 4.0D);
        return new SteeringProfile(playerPosition.add(normalizedBlendedDirection.scale(projectionDistance)), dynamicYawStep);
    }

    private void steerPlayer(final LocalPlayer player, final Vec3 waypointPosition, final double yawStepDegrees) {
        Vec3 playerPosition = player.position();
        double deltaX = waypointPosition.x - playerPosition.x;
        double deltaZ = waypointPosition.z - playerPosition.z;
        if (Mth.lengthSquared(deltaX, deltaZ) < 1.0E-6D) {
            return;
        }

        float desiredYaw = (float) Math.toDegrees(Mth.atan2(deltaZ, deltaX)) - 90.0F;
        float yawErrorBeforeUpdate = Mth.degreesDifferenceAbs(player.getYRot(), desiredYaw);
        float updatedYaw = Mth.approachDegrees(player.getYRot(), desiredYaw, (float) yawStepDegrees);
        player.setYRot(updatedYaw);
        player.setYHeadRot(updatedYaw);
        player.setYBodyRot(updatedYaw);
        this.applyTravelPitch(player, yawErrorBeforeUpdate);
    }

    private void applyTravelPitch(final LocalPlayer player, final float yawErrorDegrees) {
        float defaultPitch = (float) WynnHorseConfig.getTravelDefaultPitchDegrees();
        float maximumDeviation = (float) WynnHorseConfig.getTravelPitchMaxDeviationDegrees();
        float minimumPitch = defaultPitch - maximumDeviation;
        float maximumPitch = defaultPitch + maximumDeviation;
        float currentPitch = player.getXRot();

        if (currentPitch < minimumPitch || currentPitch > maximumPitch) {
            float clampedTarget = Mth.clamp(currentPitch, minimumPitch, maximumPitch);
            float recoveredPitch = Mth.approach(
                    currentPitch,
                    clampedTarget,
                    (float) WynnHorseConfig.getTravelPitchRecoveryStepDegrees()
            );
            player.setXRot(Mth.clamp(recoveredPitch, minimumPitch, maximumPitch));
            this.travelTargetPitch = player.getXRot();
            return;
        }

        if (yawErrorDegrees <= 2.0F) {
            this.travelTargetPitch = currentPitch;
            this.travelPitchRetargetTicksRemaining = 0;
            return;
        }

        if (Float.isNaN(this.travelTargetPitch)) {
            this.travelTargetPitch = defaultPitch;
        }

        if (this.travelPitchRetargetTicksRemaining <= 0) {
            this.travelTargetPitch = this.randomTravelPitchTarget(defaultPitch, maximumDeviation);
            this.travelPitchRetargetTicksRemaining = this.nextTravelPitchRetargetTicks();
        } else {
            this.travelPitchRetargetTicksRemaining--;
        }

        this.travelTargetPitch = Mth.clamp(this.travelTargetPitch, minimumPitch, maximumPitch);
        float updatedPitch = Mth.approach(
                currentPitch,
                this.travelTargetPitch,
                (float) WynnHorseConfig.getTravelPitchPullStepDegrees()
        );
        player.setXRot(Mth.clamp(updatedPitch, minimumPitch, maximumPitch));
    }

    private float randomTravelPitchTarget(final float defaultPitch, final float maximumDeviation) {
        double minimum = WynnHorseConfig.getTravelPitchJitterMinDegrees();
        double maximum = Math.max(minimum, WynnHorseConfig.getTravelPitchJitterMaxDegrees());
        double magnitude = ThreadLocalRandom.current().nextDouble(minimum, maximum + 1.0E-6D);
        double signedOffset = ThreadLocalRandom.current().nextBoolean() ? magnitude : -magnitude;
        return Mth.clamp((float) (defaultPitch + signedOffset), defaultPitch - maximumDeviation, defaultPitch + maximumDeviation);
    }

    private int nextTravelPitchRetargetTicks() {
        int baseInterval = Math.max(1, WynnHorseConfig.getTravelPitchRetargetIntervalTicks());
        int jitter = Math.max(1, baseInterval / 3);
        return ThreadLocalRandom.current().nextInt(Math.max(1, baseInterval - jitter), baseInterval + jitter + 1);
    }

    private static Vec3 horizontalDirection(final Vec3 from, final Vec3 to) {
        Vec3 direction = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        if (direction.lengthSqr() < 1.0E-6D) {
            return null;
        }

        return direction.normalize();
    }

    private static double horizontalDistance(final Vec3 first, final Vec3 second) {
        return Math.sqrt(Mth.lengthSquared(second.x - first.x, second.z - first.z));
    }

    private static double cornerAngleDegrees(final Vec3 playerPosition, final Vec3 currentWaypointPosition, final Vec3 nextWaypointPosition) {
        Vec3 incoming = horizontalDirection(playerPosition, currentWaypointPosition);
        Vec3 outgoing = horizontalDirection(currentWaypointPosition, nextWaypointPosition);
        if (incoming == null || outgoing == null) {
            return 0.0D;
        }

        double dot = Mth.clamp(incoming.dot(outgoing), -1.0D, 1.0D);
        return Math.toDegrees(Math.acos(dot));
    }

    private double dynamicTurnStartDistance(final double cornerAngleDegrees, final double nextSegmentLength) {
        double sharpness = Mth.clamp(cornerAngleDegrees / 180.0D, 0.0D, 1.0D);
        double gentleness = 1.0D - sharpness;
        double lengthCap = Math.max(1.0D, WynnHorseConfig.getDynamicTurnNextSegmentLengthCap());
        double nextLengthFactor = Mth.clamp(nextSegmentLength / lengthCap, 0.0D, 1.0D);
        double earlyTurnFactor = Mth.clamp((gentleness * 0.75D) + (nextLengthFactor * 0.25D), 0.0D, 1.0D);

        return Mth.lerp(
                earlyTurnFactor,
                WynnHorseConfig.getDynamicTurnMinStartDistance(),
                WynnHorseConfig.getDynamicTurnMaxStartDistance()
        );
    }

    private double dynamicTurnYawStepDegrees(final double cornerAngleDegrees, final double nextSegmentLength) {
        double sharpness = Mth.clamp(cornerAngleDegrees / 180.0D, 0.0D, 1.0D);
        double lengthCap = Math.max(1.0D, WynnHorseConfig.getDynamicTurnNextSegmentLengthCap());
        double nextLengthFactor = Mth.clamp(nextSegmentLength / lengthCap, 0.0D, 1.0D);
        double decisiveTurnFactor = Mth.clamp((sharpness * 0.75D) + ((1.0D - nextLengthFactor) * 0.25D), 0.0D, 1.0D);

        return Mth.lerp(
                decisiveTurnFactor,
                WynnHorseConfig.getDynamicTurnMinYawStepDegrees(),
                WynnHorseConfig.getDynamicTurnMaxYawStepDegrees()
        );
    }

    public record NavigationOutcome(Status status, Waypoint waypoint) {
        public static NavigationOutcome navigating(final Waypoint waypoint) {
            return new NavigationOutcome(Status.NAVIGATING, waypoint);
        }

        public static NavigationOutcome reached(final Waypoint waypoint) {
            return new NavigationOutcome(Status.REACHED, waypoint);
        }

        public static NavigationOutcome paused() {
            return new NavigationOutcome(Status.PAUSED, null);
        }

        public static NavigationOutcome noTarget() {
            return new NavigationOutcome(Status.NO_TARGET, null);
        }
    }

    public record DirectNavigationOutcome(Status status) {
        public static DirectNavigationOutcome navigating() {
            return new DirectNavigationOutcome(Status.NAVIGATING);
        }

        public static DirectNavigationOutcome reached() {
            return new DirectNavigationOutcome(Status.REACHED);
        }

        public static DirectNavigationOutcome paused() {
            return new DirectNavigationOutcome(Status.PAUSED);
        }

        public static DirectNavigationOutcome noTarget() {
            return new DirectNavigationOutcome(Status.NO_TARGET);
        }
    }

    public enum Status {
        NAVIGATING,
        REACHED,
        PAUSED,
        NO_TARGET
    }

    private record SteeringProfile(Vec3 targetPosition, double yawStepDegrees) {
    }
}
