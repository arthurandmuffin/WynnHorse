package net.wafflingpenguin.wynnhorse;

import net.minecraft.util.ARGB;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = WynnHorse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WynnHorseConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final String DEFAULT_WAYPOINT_BEACON_COLOR = "#56C5FF";
    private static final String DEFAULT_ACTIVE_WAYPOINT_BEACON_COLOR = "#FFD640";
    private static final String DEFAULT_WAYPOINT_TEXT_COLOR = "#E6F7FF";
    private static final String DEFAULT_ROUTE_LINE_COLOR = "#56C5FF";

    private static final ForgeConfigSpec.ConfigValue<String> HORSE_ITEM_DISPLAY_NAME = BUILDER
            .comment("Display-name fragment used to find the horse spawn item in the hotbar.")
            .define("horseItemDisplayName", "Horse");

    private static final ForgeConfigSpec.DoubleValue WAYPOINT_REACHED_DISTANCE = BUILDER
            .comment("Distance in blocks considered close enough to reach a waypoint.")
            .defineInRange("waypointReachedDistance", 2.0D, 0.25D, 16.0D);

    private static final ForgeConfigSpec.DoubleValue STEERING_YAW_STEP_DEGREES = BUILDER
            .comment("Maximum yaw change applied per client tick while steering toward a waypoint.")
            .defineInRange("steeringYawStepDegrees", 4.0D, 0.5D, 45.0D);

    private static final ForgeConfigSpec.DoubleValue WAYPOINT_CORNER_RADIUS = BUILDER
            .comment("Distance from a waypoint where steering may begin blending toward the next waypoint.")
            .defineInRange("waypointCornerRadius", 6.0D, 0.5D, 32.0D);

    private static final ForgeConfigSpec.ConfigValue<String> WAYPOINT_RENDER_STYLE = BUILDER
            .comment("Render style used for waypoint visualization. Valid values: marker, beacon.")
            .define("waypointRenderStyle", WaypointRenderStyle.MARKER.serializedName());

    private static final ForgeConfigSpec.ConfigValue<String> WAYPOINT_BEACON_COLOR = BUILDER
            .comment("Hex color used for normal waypoint markers/beacons. Accepts #RRGGBB or RRGGBB.")
            .define("waypointBeaconColor", DEFAULT_WAYPOINT_BEACON_COLOR);

    private static final ForgeConfigSpec.ConfigValue<String> ACTIVE_WAYPOINT_BEACON_COLOR = BUILDER
            .comment("Hex color used for the active/next waypoint marker/beacon. Accepts #RRGGBB or RRGGBB.")
            .define("activeWaypointBeaconColor", DEFAULT_ACTIVE_WAYPOINT_BEACON_COLOR);

    private static final ForgeConfigSpec.ConfigValue<String> WAYPOINT_TEXT_COLOR = BUILDER
            .comment("Hex color used for waypoint label text. Accepts #RRGGBB or RRGGBB.")
            .define("waypointTextColor", DEFAULT_WAYPOINT_TEXT_COLOR);

    private static final ForgeConfigSpec.ConfigValue<String> ROUTE_LINE_COLOR = BUILDER
            .comment("Hex color used for route line tracing. Accepts #RRGGBB or RRGGBB.")
            .define("routeLineColor", DEFAULT_ROUTE_LINE_COLOR);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String horseItemDisplayName = HORSE_ITEM_DISPLAY_NAME.get();
    public static double waypointReachedDistance = WAYPOINT_REACHED_DISTANCE.get();
    public static double steeringYawStepDegrees = STEERING_YAW_STEP_DEGREES.get();
    public static double waypointCornerRadius = WAYPOINT_CORNER_RADIUS.get();
    public static WaypointRenderStyle waypointRenderStyle = WaypointRenderStyle.fromSerializedName(WAYPOINT_RENDER_STYLE.get());
    public static String waypointBeaconColor = readHexColor(WAYPOINT_BEACON_COLOR.get(), DEFAULT_WAYPOINT_BEACON_COLOR);
    public static String activeWaypointBeaconColor = readHexColor(ACTIVE_WAYPOINT_BEACON_COLOR.get(), DEFAULT_ACTIVE_WAYPOINT_BEACON_COLOR);
    public static String waypointTextColor = readHexColor(WAYPOINT_TEXT_COLOR.get(), DEFAULT_WAYPOINT_TEXT_COLOR);
    public static String routeLineColor = readHexColor(ROUTE_LINE_COLOR.get(), DEFAULT_ROUTE_LINE_COLOR);

    private WynnHorseConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        horseItemDisplayName = HORSE_ITEM_DISPLAY_NAME.get().trim();
        waypointReachedDistance = WAYPOINT_REACHED_DISTANCE.get();
        steeringYawStepDegrees = STEERING_YAW_STEP_DEGREES.get();
        waypointCornerRadius = WAYPOINT_CORNER_RADIUS.get();
        waypointRenderStyle = WaypointRenderStyle.fromSerializedName(WAYPOINT_RENDER_STYLE.get());
        waypointBeaconColor = syncLoadedHexColor(WAYPOINT_BEACON_COLOR, DEFAULT_WAYPOINT_BEACON_COLOR);
        activeWaypointBeaconColor = syncLoadedHexColor(ACTIVE_WAYPOINT_BEACON_COLOR, DEFAULT_ACTIVE_WAYPOINT_BEACON_COLOR);
        waypointTextColor = syncLoadedHexColor(WAYPOINT_TEXT_COLOR, DEFAULT_WAYPOINT_TEXT_COLOR);
        routeLineColor = syncLoadedHexColor(ROUTE_LINE_COLOR, DEFAULT_ROUTE_LINE_COLOR);
    }

    public static String getHorseItemDisplayName() {
        return horseItemDisplayName;
    }

    public static void setHorseItemDisplayName(final String value) {
        horseItemDisplayName = value == null ? "" : value.trim();
        HORSE_ITEM_DISPLAY_NAME.set(horseItemDisplayName);
    }

    public static WaypointRenderStyle getWaypointRenderStyle() {
        return waypointRenderStyle;
    }

    public static double getWaypointReachedDistance() {
        return waypointReachedDistance;
    }

    public static double getSteeringYawStepDegrees() {
        return steeringYawStepDegrees;
    }

    public static double getWaypointCornerRadius() {
        return waypointCornerRadius;
    }

    public static void setWaypointRenderStyle(final WaypointRenderStyle style) {
        waypointRenderStyle = style == null ? WaypointRenderStyle.MARKER : style;
        WAYPOINT_RENDER_STYLE.set(waypointRenderStyle.serializedName());
    }

    public static String getWaypointBeaconColor() {
        return waypointBeaconColor;
    }

    public static void setWaypointBeaconColor(final String value) {
        String normalized = normalizeHexColor(value);
        if (normalized == null) {
            return;
        }

        waypointBeaconColor = normalized;
        WAYPOINT_BEACON_COLOR.set(normalized);
    }

    public static String getActiveWaypointBeaconColor() {
        return activeWaypointBeaconColor;
    }

    public static void setActiveWaypointBeaconColor(final String value) {
        String normalized = normalizeHexColor(value);
        if (normalized == null) {
            return;
        }

        activeWaypointBeaconColor = normalized;
        ACTIVE_WAYPOINT_BEACON_COLOR.set(normalized);
    }

    public static String getWaypointTextColor() {
        return waypointTextColor;
    }

    public static void setWaypointTextColor(final String value) {
        String normalized = normalizeHexColor(value);
        if (normalized == null) {
            return;
        }

        waypointTextColor = normalized;
        WAYPOINT_TEXT_COLOR.set(normalized);
    }

    public static String getRouteLineColor() {
        return routeLineColor;
    }

    public static void setRouteLineColor(final String value) {
        String normalized = normalizeHexColor(value);
        if (normalized == null) {
            return;
        }

        routeLineColor = normalized;
        ROUTE_LINE_COLOR.set(normalized);
    }

    public static String normalizeHexColor(final String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String hex = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
        if (hex.length() != 6) {
            return null;
        }

        for (int index = 0; index < hex.length(); index++) {
            if (Character.digit(hex.charAt(index), 16) == -1) {
                return null;
            }
        }

        return "#" + hex.toUpperCase(Locale.ROOT);
    }

    public static int parseHexColor(final String value, final int fallback) {
        String normalized = normalizeHexColor(value);
        if (normalized == null) {
            return fallback;
        }

        int rgb = Integer.parseInt(normalized.substring(1), 16);
        return ARGB.color(255, (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255);
    }

    private static String readHexColor(final String value, final String fallback) {
        String normalized = normalizeHexColor(value);
        if (normalized == null) {
            normalized = fallback;
        }

        return normalized;
    }

    private static String syncLoadedHexColor(final ForgeConfigSpec.ConfigValue<String> value, final String fallback) {
        String normalized = readHexColor(value.get(), fallback);
        value.set(normalized);
        return normalized;
    }
}
