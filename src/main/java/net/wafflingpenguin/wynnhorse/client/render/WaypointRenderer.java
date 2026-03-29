package net.wafflingpenguin.wynnhorse.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wafflingpenguin.wynnhorse.WaypointRenderStyle;
import net.wafflingpenguin.wynnhorse.WynnHorseConfig;
import net.wafflingpenguin.wynnhorse.client.WynnHorseClient;
import net.wafflingpenguin.wynnhorse.waypoint.Waypoint;
import net.wafflingpenguin.wynnhorse.waypoint.WaypointRoute;
import net.minecraftforge.client.event.ViewportEvent;

import java.util.List;

public final class WaypointRenderer {
    private static final double MARKER_HALF_WIDTH = 0.35;
    private static final double MARKER_HEIGHT = 2.75;
    private static final double LABEL_HEIGHT = 3.2;
    private static final double BEACON_BASE_OFFSET = 0.5;
    private static final double BEACON_HALF_WIDTH = MARKER_HALF_WIDTH;
    private static final float LABEL_SCALE = 0.9F;
    private static final float ACTIVE_LABEL_SCALE = 1.0F;
    private static final float ROUTE_LINE_WIDTH = 3.5F;
    private static final float ACTIVE_LINE_WIDTH = 4.5F;

    private WaypointRenderer() {
    }

    public static void collectWaypointGizmos(final ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        WaypointRoute route = WynnHorseClient.getWaypointStore().route();
        List<Waypoint> waypoints = route.getWaypoints();
        if (waypoints.isEmpty()) {
            return;
        }

        try (var ignored = minecraft.levelRenderer.collectPerFrameGizmos()) {
            renderRoute(waypoints, route.getActiveIndex());
        }
    }

    private static void renderRoute(final List<Waypoint> waypoints, final int activeIndex) {
        int baseColor = WynnHorseConfig.parseHexColor(WynnHorseConfig.getWaypointBeaconColor(), ARGB.color(255, 86, 197, 255));
        int activeColor = WynnHorseConfig.parseHexColor(WynnHorseConfig.getActiveWaypointBeaconColor(), ARGB.color(255, 255, 214, 64));
        int textColor = WynnHorseConfig.parseHexColor(WynnHorseConfig.getWaypointTextColor(), ARGB.color(255, 230, 247, 255));
        int routeLineColor = WynnHorseConfig.parseHexColor(WynnHorseConfig.getRouteLineColor(), baseColor);

        renderRouteLines(waypoints, routeLineColor, activeColor);

        WaypointRenderStyle renderStyle = WynnHorseConfig.getWaypointRenderStyle();
        for (int index = 0; index < waypoints.size(); index++) {
            Waypoint waypoint = waypoints.get(index);
            renderWaypoint(Minecraft.getInstance(), waypoint, index, index == activeIndex, renderStyle, baseColor, activeColor, textColor);
        }
    }

    private static void renderRouteLines(final List<Waypoint> waypoints, final int routeLineColor, final int activeColor) {
        if (waypoints.size() < 2) {
            return;
        }

        int visibleRouteLineColor = ARGB.multiplyAlpha(routeLineColor, 0.9F);
        int loopLineColor = ARGB.multiplyAlpha(ARGB.average(routeLineColor, activeColor), 0.8F);

        for (int index = 0; index < waypoints.size() - 1; index++) {
            Vec3 start = elevatedRoutePoint(waypoints.get(index).position());
            Vec3 end = elevatedRoutePoint(waypoints.get(index + 1).position());
            Gizmos.line(start, end, visibleRouteLineColor, ROUTE_LINE_WIDTH);
        }

        Vec3 loopStart = elevatedRoutePoint(waypoints.getLast().position());
        Vec3 loopEnd = elevatedRoutePoint(waypoints.getFirst().position());
        Gizmos.line(loopStart, loopEnd, loopLineColor, ROUTE_LINE_WIDTH);
    }

    private static void renderWaypoint(final Minecraft minecraft, final Waypoint waypoint, final int index, final boolean active, final WaypointRenderStyle renderStyle, final int baseColor, final int activeColor, final int textColor) {
        Vec3 position = waypoint.position();
        int strokeColor = active ? activeColor : baseColor;
        int fillColor = active ? ARGB.multiplyAlpha(activeColor, 0.30F) : ARGB.multiplyAlpha(baseColor, 0.22F);
        float lineWidth = active ? ACTIVE_LINE_WIDTH : ROUTE_LINE_WIDTH;

        if (renderStyle == WaypointRenderStyle.BEACON) {
            renderBeacon(minecraft, position, active, baseColor, activeColor);
        } else {
            Gizmos.cuboid(markerBounds(position), GizmoStyle.strokeAndFill(strokeColor, lineWidth, fillColor));
            Gizmos.line(position, position.add(0.0D, MARKER_HEIGHT, 0.0D), strokeColor, lineWidth);
        }
        Gizmos.point(position.add(0.0D, MARKER_HEIGHT, 0.0D), strokeColor, active ? 12.0F : 9.0F);

        String prefix = active ? "* " : "";
        TextGizmo.Style labelStyle = TextGizmo.Style.forColorAndCentered(textColor).withScale(active ? ACTIVE_LABEL_SCALE : LABEL_SCALE);
        Gizmos.billboardText(prefix + (index + 1) + ". " + waypoint.name(), position.add(0.0D, LABEL_HEIGHT, 0.0D), labelStyle)
                .setAlwaysOnTop();
    }

    private static AABB markerBounds(final Vec3 position) {
        return new AABB(
                position.x - MARKER_HALF_WIDTH,
                position.y,
                position.z - MARKER_HALF_WIDTH,
                position.x + MARKER_HALF_WIDTH,
                position.y + MARKER_HEIGHT,
                position.z + MARKER_HALF_WIDTH
        );
    }

    private static Vec3 elevatedRoutePoint(final Vec3 position) {
        return position.add(0.0D, 0.2D, 0.0D);
    }

    private static void renderBeacon(final Minecraft minecraft, final Vec3 position, final boolean active, final int baseColor, final int activeColor) {
        if (minecraft.level == null) {
            return;
        }

        int beaconColor = active ? activeColor : baseColor;
        int beaconFill = beaconColor;
        float beaconWidth = active ? ACTIVE_LINE_WIDTH : ROUTE_LINE_WIDTH;
        double minY = minecraft.level.getMinY();
        double maxY = minecraft.level.getMaxY();
        AABB beamBounds = new AABB(
                position.x - BEACON_HALF_WIDTH,
                minY + BEACON_BASE_OFFSET,
                position.z - BEACON_HALF_WIDTH,
                position.x + BEACON_HALF_WIDTH,
                maxY - BEACON_BASE_OFFSET,
                position.z + BEACON_HALF_WIDTH
        );

        Gizmos.cuboid(beamBounds, GizmoStyle.strokeAndFill(beaconColor, beaconWidth, beaconFill));
    }
}
