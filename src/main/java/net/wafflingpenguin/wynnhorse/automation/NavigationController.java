package net.wafflingpenguin.wynnhorse.automation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.wafflingpenguin.wynnhorse.WynnHorseConfig;
import net.wafflingpenguin.wynnhorse.waypoint.Waypoint;
import net.wafflingpenguin.wynnhorse.waypoint.WaypointRoute;

import java.util.List;

public final class NavigationController {
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
        Vec3 steeringTarget = this.resolveSteeringTarget(player.position(), waypoint.position(), nextWaypoint == null ? null : nextWaypoint.position());
        this.steerPlayer(player, steeringTarget, WynnHorseConfig.getSteeringYawStepDegrees());
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

    private Waypoint resolveNextWaypoint(final List<Waypoint> waypoints, final int activeIndex) {
        if (waypoints.size() < 2) {
            return null;
        }

        return waypoints.get((activeIndex + 1) % waypoints.size());
    }

    private Vec3 resolveSteeringTarget(final Vec3 playerPosition, final Vec3 currentWaypointPosition, final Vec3 nextWaypointPosition) {
        if (nextWaypointPosition == null) {
            return currentWaypointPosition;
        }

        double cornerRadius = Math.max(WynnHorseConfig.getWaypointCornerRadius(), WynnHorseConfig.getWaypointReachedDistance());
        double currentDistance = horizontalDistance(playerPosition, currentWaypointPosition);
        if (currentDistance >= cornerRadius) {
            return currentWaypointPosition;
        }

        Vec3 currentDirection = horizontalDirection(playerPosition, currentWaypointPosition);
        Vec3 nextDirection = horizontalDirection(playerPosition, nextWaypointPosition);
        if (currentDirection == null || nextDirection == null) {
            return currentWaypointPosition;
        }

        double blend = Mth.clamp(1.0D - currentDistance / cornerRadius, 0.0D, 1.0D);
        blend *= blend;

        Vec3 blendedDirection = currentDirection.scale(1.0D - blend).add(nextDirection.scale(blend));
        if (blendedDirection.lengthSqr() < 1.0E-6D) {
            return currentWaypointPosition;
        }

        Vec3 normalizedBlendedDirection = blendedDirection.normalize();
        double projectionDistance = Math.max(currentDistance, 4.0D);
        return playerPosition.add(normalizedBlendedDirection.scale(projectionDistance));
    }

    private void steerPlayer(final LocalPlayer player, final Vec3 waypointPosition, final double yawStepDegrees) {
        Vec3 playerPosition = player.position();
        double deltaX = waypointPosition.x - playerPosition.x;
        double deltaZ = waypointPosition.z - playerPosition.z;
        if (Mth.lengthSquared(deltaX, deltaZ) < 1.0E-6D) {
            return;
        }

        float desiredYaw = (float) Math.toDegrees(Mth.atan2(deltaZ, deltaX)) - 90.0F;
        float updatedYaw = Mth.approachDegrees(player.getYRot(), desiredYaw, (float) yawStepDegrees);
        player.setYRot(updatedYaw);
        player.setYHeadRot(updatedYaw);
        player.setYBodyRot(updatedYaw);
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
}
