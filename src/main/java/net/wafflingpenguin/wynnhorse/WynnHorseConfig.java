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

    private static final ForgeConfigSpec.DoubleValue DYNAMIC_TURN_MIN_YAW_STEP_DEGREES = BUILDER
            .comment("Minimum yaw change per tick used by dynamic waypoint steering for wide gentle turns.")
            .defineInRange("dynamicTurnMinYawStepDegrees", 2.5D, 0.1D, 45.0D);

    private static final ForgeConfigSpec.DoubleValue DYNAMIC_TURN_MAX_YAW_STEP_DEGREES = BUILDER
            .comment("Maximum yaw change per tick used by dynamic waypoint steering for tight sharp turns.")
            .defineInRange("dynamicTurnMaxYawStepDegrees", 7.5D, 0.1D, 90.0D);

    private static final ForgeConfigSpec.DoubleValue DYNAMIC_TURN_MIN_START_DISTANCE = BUILDER
            .comment("Minimum distance from the waypoint where dynamic steering may begin turning for sharp corners.")
            .defineInRange("dynamicTurnMinStartDistance", 3.0D, 0.25D, 32.0D);

    private static final ForgeConfigSpec.DoubleValue DYNAMIC_TURN_MAX_START_DISTANCE = BUILDER
            .comment("Maximum distance from the waypoint where dynamic steering may begin turning for gentle corners.")
            .defineInRange("dynamicTurnMaxStartDistance", 10.0D, 0.25D, 48.0D);

    private static final ForgeConfigSpec.DoubleValue DYNAMIC_TURN_NEXT_SEGMENT_LENGTH_CAP = BUILDER
            .comment("Next-segment length that counts as fully long when computing dynamic turn-start distance and turn rate.")
            .defineInRange("dynamicTurnNextSegmentLengthCap", 20.0D, 1.0D, 128.0D);

    private static final ForgeConfigSpec.DoubleValue TRAVEL_DEFAULT_PITCH_DEGREES = BUILDER
            .comment("Default pitch in degrees used during ordinary travel steering. Positive values look downward.")
            .defineInRange("travelDefaultPitchDegrees", 17.5D, 0.0D, 45.0D);

    private static final ForgeConfigSpec.DoubleValue TRAVEL_PITCH_PULL_STEP_DEGREES = BUILDER
            .comment("Maximum pitch change per tick while gradually pulling the camera back toward the travel pitch target.")
            .defineInRange("travelPitchPullStepDegrees", 0.9D, 0.05D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue TRAVEL_PITCH_RECOVERY_STEP_DEGREES = BUILDER
            .comment("Maximum pitch change per tick while recovering back into the allowed travel pitch range after manual camera movement pushes it out of bounds.")
            .defineInRange("travelPitchRecoveryStepDegrees", 3.5D, 0.1D, 20.0D);

    private static final ForgeConfigSpec.DoubleValue TRAVEL_PITCH_JITTER_MIN_DEGREES = BUILDER
            .comment("Minimum pitch deviation in degrees used for ordinary travel turn humanization.")
            .defineInRange("travelPitchJitterMinDegrees", 0.75D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue TRAVEL_PITCH_JITTER_MAX_DEGREES = BUILDER
            .comment("Maximum pitch deviation in degrees used for ordinary travel turn humanization.")
            .defineInRange("travelPitchJitterMaxDegrees", 3.0D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue TRAVEL_PITCH_MAX_DEVIATION_DEGREES = BUILDER
            .comment("Maximum absolute deviation from the default travel pitch used during ordinary steering.")
            .defineInRange("travelPitchMaxDeviationDegrees", 10.0D, 0.0D, 25.0D);

    private static final ForgeConfigSpec.IntValue TRAVEL_PITCH_RETARGET_INTERVAL_TICKS = BUILDER
            .comment("Base tick interval used before choosing a new ordinary travel pitch target while steering.")
            .defineInRange("travelPitchRetargetIntervalTicks", 8, 1, 80);

    private static final ForgeConfigSpec.DoubleValue HORSE_SPAWN_DETECTION_RADIUS = BUILDER
            .comment("Radius in blocks used to search for a newly spawned horse after using the selected horse item.")
            .defineInRange("horseSpawnDetectionRadius", 10.0D, 2.0D, 48.0D);

    private static final ForgeConfigSpec.IntValue HORSE_SPAWN_DETECTION_TIMEOUT_TICKS = BUILDER
            .comment("How many client ticks to wait for a spawned horse to appear before failing the spawn step.")
            .defineInRange("horseSpawnDetectionTimeoutTicks", 30, 5, 200);

    private static final ForgeConfigSpec.IntValue HORSE_SPAWN_RETRY_DELAY_TICKS = BUILDER
            .comment("How many ticks to wait after a failed horse detection before retrying another spawn attempt.")
            .defineInRange("horseSpawnRetryDelayTicks", 24, 1, 200);

    private static final ForgeConfigSpec.DoubleValue HORSE_SPAWN_LOOK_PITCH_DEGREES = BUILDER
            .comment("Target pitch in degrees used before attempting to spawn the horse. Positive values look downward.")
            .defineInRange("horseSpawnLookPitchDegrees", 85.0D, 10.0D, 90.0D);

    private static final ForgeConfigSpec.DoubleValue HORSE_SPAWN_LOOK_PITCH_STEP_DEGREES = BUILDER
            .comment("Maximum pitch change per tick while turning downward before attempting to spawn the horse.")
            .defineInRange("horseSpawnLookPitchStepDegrees", 12.0D, 0.25D, 45.0D);

    private static final ForgeConfigSpec.DoubleValue HORSE_NEARBY_REUSE_RADIUS = BUILDER
            .comment("Maximum distance for reusing a nearby horse when exact named-horse matching is unavailable.")
            .defineInRange("horseNearbyReuseRadius", 4.0D, 1.0D, 16.0D);

    private static final ForgeConfigSpec.DoubleValue HORSE_MOUNT_RANGE = BUILDER
            .comment("Distance in blocks considered close enough to attempt mounting a detected horse.")
            .defineInRange("horseMountRange", 2.0D, 0.5D, 6.0D);

    private static final ForgeConfigSpec.IntValue HORSE_MOUNT_RETRY_COOLDOWN_TICKS = BUILDER
            .comment("Ticks to wait between repeated mount interaction attempts.")
            .defineInRange("horseMountRetryCooldownTicks", 10, 1, 100);

    private static final ForgeConfigSpec.DoubleValue HORSE_APPROACH_YAW_STEP_DEGREES = BUILDER
            .comment("Maximum yaw change per tick while turning toward a detected horse before mounting.")
            .defineInRange("horseApproachYawStepDegrees", 2.0D, 0.25D, 20.0D);

    private static final ForgeConfigSpec.IntValue HORSE_APPROACH_PAUSE_TICKS = BUILDER
            .comment("Ticks to pause briefly after horse detection before beginning the mount approach.")
            .defineInRange("horseApproachPauseTicks", 10, 0, 100);

    private static final ForgeConfigSpec.DoubleValue HORSE_POST_MOUNT_OVERSHOOT_YAW_STEP_DEGREES = BUILDER
            .comment("Maximum yaw change per tick during the initial overshoot leg of the post-mount pivot.")
            .defineInRange("horsePostMountOvershootYawStepDegrees", 30.0D, 0.25D, 90.0D);

    private static final ForgeConfigSpec.DoubleValue HORSE_POST_MOUNT_CORRECTION_YAW_STEP_DEGREES = BUILDER
            .comment("Maximum yaw change per tick during the correction leg of the post-mount pivot.")
            .defineInRange("horsePostMountCorrectionYawStepDegrees", 18.0D, 0.25D, 90.0D);

    private static final ForgeConfigSpec.DoubleValue HORSE_POST_MOUNT_FACING_TOLERANCE_DEGREES = BUILDER
            .comment("Yaw error tolerance in degrees considered close enough to begin movement after mounting.")
            .defineInRange("horsePostMountFacingToleranceDegrees", 3.0D, 0.5D, 45.0D);

    private static final ForgeConfigSpec.DoubleValue HORSE_POST_MOUNT_OVERSHOOT_MIN_DEGREES = BUILDER
            .comment("Minimum yaw overshoot in degrees for the post-mount snap turn before correcting back toward the waypoint.")
            .defineInRange("horsePostMountOvershootMinDegrees", 4.0D, 0.0D, 45.0D);

    private static final ForgeConfigSpec.DoubleValue HORSE_POST_MOUNT_OVERSHOOT_MAX_DEGREES = BUILDER
            .comment("Maximum yaw overshoot in degrees for the post-mount snap turn before correcting back toward the waypoint.")
            .defineInRange("horsePostMountOvershootMaxDegrees", 10.0D, 0.0D, 60.0D);

    private static final ForgeConfigSpec.DoubleValue HORSE_POST_MOUNT_PITCH_STEP_DEGREES = BUILDER
            .comment("Maximum pitch change per tick during the post-mount snap turn.")
            .defineInRange("horsePostMountPitchStepDegrees", 2.25D, 0.1D, 15.0D);

    private static final ForgeConfigSpec.DoubleValue HORSE_POST_MOUNT_PITCH_TOLERANCE_DEGREES = BUILDER
            .comment("Pitch error tolerance in degrees for completing the post-mount snap turn.")
            .defineInRange("horsePostMountPitchToleranceDegrees", 0.9D, 0.1D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue HORSE_POST_MOUNT_PITCH_JITTER_MIN_DEGREES = BUILDER
            .comment("Minimum pitch deviation in degrees used for the randomized post-mount snap turn animation.")
            .defineInRange("horsePostMountPitchJitterMinDegrees", 0.75D, 0.0D, 15.0D);

    private static final ForgeConfigSpec.DoubleValue HORSE_POST_MOUNT_PITCH_JITTER_MAX_DEGREES = BUILDER
            .comment("Maximum pitch deviation in degrees used for the randomized post-mount snap turn animation.")
            .defineInRange("horsePostMountPitchJitterMaxDegrees", 3.0D, 0.0D, 20.0D);

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
    public static double dynamicTurnMinYawStepDegrees = DYNAMIC_TURN_MIN_YAW_STEP_DEGREES.get();
    public static double dynamicTurnMaxYawStepDegrees = DYNAMIC_TURN_MAX_YAW_STEP_DEGREES.get();
    public static double dynamicTurnMinStartDistance = DYNAMIC_TURN_MIN_START_DISTANCE.get();
    public static double dynamicTurnMaxStartDistance = DYNAMIC_TURN_MAX_START_DISTANCE.get();
    public static double dynamicTurnNextSegmentLengthCap = DYNAMIC_TURN_NEXT_SEGMENT_LENGTH_CAP.get();
    public static double travelDefaultPitchDegrees = TRAVEL_DEFAULT_PITCH_DEGREES.get();
    public static double travelPitchPullStepDegrees = TRAVEL_PITCH_PULL_STEP_DEGREES.get();
    public static double travelPitchRecoveryStepDegrees = TRAVEL_PITCH_RECOVERY_STEP_DEGREES.get();
    public static double travelPitchJitterMinDegrees = TRAVEL_PITCH_JITTER_MIN_DEGREES.get();
    public static double travelPitchJitterMaxDegrees = TRAVEL_PITCH_JITTER_MAX_DEGREES.get();
    public static double travelPitchMaxDeviationDegrees = TRAVEL_PITCH_MAX_DEVIATION_DEGREES.get();
    public static int travelPitchRetargetIntervalTicks = TRAVEL_PITCH_RETARGET_INTERVAL_TICKS.get();
    public static double horseSpawnDetectionRadius = HORSE_SPAWN_DETECTION_RADIUS.get();
    public static int horseSpawnDetectionTimeoutTicks = HORSE_SPAWN_DETECTION_TIMEOUT_TICKS.get();
    public static int horseSpawnRetryDelayTicks = HORSE_SPAWN_RETRY_DELAY_TICKS.get();
    public static double horseSpawnLookPitchDegrees = HORSE_SPAWN_LOOK_PITCH_DEGREES.get();
    public static double horseSpawnLookPitchStepDegrees = HORSE_SPAWN_LOOK_PITCH_STEP_DEGREES.get();
    public static double horseNearbyReuseRadius = HORSE_NEARBY_REUSE_RADIUS.get();
    public static double horseMountRange = HORSE_MOUNT_RANGE.get();
    public static int horseMountRetryCooldownTicks = HORSE_MOUNT_RETRY_COOLDOWN_TICKS.get();
    public static double horseApproachYawStepDegrees = HORSE_APPROACH_YAW_STEP_DEGREES.get();
    public static int horseApproachPauseTicks = HORSE_APPROACH_PAUSE_TICKS.get();
    public static double horsePostMountOvershootYawStepDegrees = HORSE_POST_MOUNT_OVERSHOOT_YAW_STEP_DEGREES.get();
    public static double horsePostMountCorrectionYawStepDegrees = HORSE_POST_MOUNT_CORRECTION_YAW_STEP_DEGREES.get();
    public static double horsePostMountFacingToleranceDegrees = HORSE_POST_MOUNT_FACING_TOLERANCE_DEGREES.get();
    public static double horsePostMountOvershootMinDegrees = HORSE_POST_MOUNT_OVERSHOOT_MIN_DEGREES.get();
    public static double horsePostMountOvershootMaxDegrees = HORSE_POST_MOUNT_OVERSHOOT_MAX_DEGREES.get();
    public static double horsePostMountPitchStepDegrees = HORSE_POST_MOUNT_PITCH_STEP_DEGREES.get();
    public static double horsePostMountPitchToleranceDegrees = HORSE_POST_MOUNT_PITCH_TOLERANCE_DEGREES.get();
    public static double horsePostMountPitchJitterMinDegrees = HORSE_POST_MOUNT_PITCH_JITTER_MIN_DEGREES.get();
    public static double horsePostMountPitchJitterMaxDegrees = HORSE_POST_MOUNT_PITCH_JITTER_MAX_DEGREES.get();
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
        dynamicTurnMinYawStepDegrees = DYNAMIC_TURN_MIN_YAW_STEP_DEGREES.get();
        dynamicTurnMaxYawStepDegrees = DYNAMIC_TURN_MAX_YAW_STEP_DEGREES.get();
        dynamicTurnMinStartDistance = DYNAMIC_TURN_MIN_START_DISTANCE.get();
        dynamicTurnMaxStartDistance = DYNAMIC_TURN_MAX_START_DISTANCE.get();
        dynamicTurnNextSegmentLengthCap = DYNAMIC_TURN_NEXT_SEGMENT_LENGTH_CAP.get();
        travelDefaultPitchDegrees = TRAVEL_DEFAULT_PITCH_DEGREES.get();
        travelPitchPullStepDegrees = TRAVEL_PITCH_PULL_STEP_DEGREES.get();
        travelPitchRecoveryStepDegrees = TRAVEL_PITCH_RECOVERY_STEP_DEGREES.get();
        travelPitchJitterMinDegrees = TRAVEL_PITCH_JITTER_MIN_DEGREES.get();
        travelPitchJitterMaxDegrees = TRAVEL_PITCH_JITTER_MAX_DEGREES.get();
        travelPitchMaxDeviationDegrees = TRAVEL_PITCH_MAX_DEVIATION_DEGREES.get();
        travelPitchRetargetIntervalTicks = TRAVEL_PITCH_RETARGET_INTERVAL_TICKS.get();
        horseSpawnDetectionRadius = HORSE_SPAWN_DETECTION_RADIUS.get();
        horseSpawnDetectionTimeoutTicks = HORSE_SPAWN_DETECTION_TIMEOUT_TICKS.get();
        horseSpawnRetryDelayTicks = HORSE_SPAWN_RETRY_DELAY_TICKS.get();
        horseSpawnLookPitchDegrees = HORSE_SPAWN_LOOK_PITCH_DEGREES.get();
        horseSpawnLookPitchStepDegrees = HORSE_SPAWN_LOOK_PITCH_STEP_DEGREES.get();
        horseNearbyReuseRadius = HORSE_NEARBY_REUSE_RADIUS.get();
        horseMountRange = HORSE_MOUNT_RANGE.get();
        horseMountRetryCooldownTicks = HORSE_MOUNT_RETRY_COOLDOWN_TICKS.get();
        horseApproachYawStepDegrees = HORSE_APPROACH_YAW_STEP_DEGREES.get();
        horseApproachPauseTicks = HORSE_APPROACH_PAUSE_TICKS.get();
        horsePostMountOvershootYawStepDegrees = HORSE_POST_MOUNT_OVERSHOOT_YAW_STEP_DEGREES.get();
        horsePostMountCorrectionYawStepDegrees = HORSE_POST_MOUNT_CORRECTION_YAW_STEP_DEGREES.get();
        horsePostMountFacingToleranceDegrees = HORSE_POST_MOUNT_FACING_TOLERANCE_DEGREES.get();
        horsePostMountOvershootMinDegrees = HORSE_POST_MOUNT_OVERSHOOT_MIN_DEGREES.get();
        horsePostMountOvershootMaxDegrees = HORSE_POST_MOUNT_OVERSHOOT_MAX_DEGREES.get();
        horsePostMountPitchStepDegrees = HORSE_POST_MOUNT_PITCH_STEP_DEGREES.get();
        horsePostMountPitchToleranceDegrees = HORSE_POST_MOUNT_PITCH_TOLERANCE_DEGREES.get();
        horsePostMountPitchJitterMinDegrees = HORSE_POST_MOUNT_PITCH_JITTER_MIN_DEGREES.get();
        horsePostMountPitchJitterMaxDegrees = HORSE_POST_MOUNT_PITCH_JITTER_MAX_DEGREES.get();
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

    public static double getDynamicTurnMinYawStepDegrees() {
        return dynamicTurnMinYawStepDegrees;
    }

    public static double getDynamicTurnMaxYawStepDegrees() {
        return dynamicTurnMaxYawStepDegrees;
    }

    public static double getDynamicTurnMinStartDistance() {
        return dynamicTurnMinStartDistance;
    }

    public static double getDynamicTurnMaxStartDistance() {
        return dynamicTurnMaxStartDistance;
    }

    public static double getDynamicTurnNextSegmentLengthCap() {
        return dynamicTurnNextSegmentLengthCap;
    }

    public static double getTravelDefaultPitchDegrees() {
        return travelDefaultPitchDegrees;
    }

    public static double getTravelPitchPullStepDegrees() {
        return travelPitchPullStepDegrees;
    }

    public static double getTravelPitchRecoveryStepDegrees() {
        return travelPitchRecoveryStepDegrees;
    }

    public static double getTravelPitchJitterMinDegrees() {
        return travelPitchJitterMinDegrees;
    }

    public static double getTravelPitchJitterMaxDegrees() {
        return travelPitchJitterMaxDegrees;
    }

    public static double getTravelPitchMaxDeviationDegrees() {
        return travelPitchMaxDeviationDegrees;
    }

    public static int getTravelPitchRetargetIntervalTicks() {
        return travelPitchRetargetIntervalTicks;
    }

    public static double getHorseSpawnDetectionRadius() {
        return horseSpawnDetectionRadius;
    }

    public static int getHorseSpawnDetectionTimeoutTicks() {
        return horseSpawnDetectionTimeoutTicks;
    }

    public static int getHorseSpawnRetryDelayTicks() {
        return horseSpawnRetryDelayTicks;
    }

    public static double getHorseSpawnLookPitchDegrees() {
        return horseSpawnLookPitchDegrees;
    }

    public static double getHorseSpawnLookPitchStepDegrees() {
        return horseSpawnLookPitchStepDegrees;
    }

    public static double getHorseNearbyReuseRadius() {
        return horseNearbyReuseRadius;
    }

    public static double getHorseMountRange() {
        return horseMountRange;
    }

    public static int getHorseMountRetryCooldownTicks() {
        return horseMountRetryCooldownTicks;
    }

    public static double getHorseApproachYawStepDegrees() {
        return horseApproachYawStepDegrees;
    }

    public static int getHorseApproachPauseTicks() {
        return horseApproachPauseTicks;
    }

    public static double getHorsePostMountOvershootYawStepDegrees() {
        return horsePostMountOvershootYawStepDegrees;
    }

    public static double getHorsePostMountCorrectionYawStepDegrees() {
        return horsePostMountCorrectionYawStepDegrees;
    }

    public static double getHorsePostMountFacingToleranceDegrees() {
        return horsePostMountFacingToleranceDegrees;
    }

    public static double getHorsePostMountOvershootMinDegrees() {
        return horsePostMountOvershootMinDegrees;
    }

    public static double getHorsePostMountOvershootMaxDegrees() {
        return horsePostMountOvershootMaxDegrees;
    }

    public static double getHorsePostMountPitchStepDegrees() {
        return horsePostMountPitchStepDegrees;
    }

    public static double getHorsePostMountPitchToleranceDegrees() {
        return horsePostMountPitchToleranceDegrees;
    }

    public static double getHorsePostMountPitchJitterMinDegrees() {
        return horsePostMountPitchJitterMinDegrees;
    }

    public static double getHorsePostMountPitchJitterMaxDegrees() {
        return horsePostMountPitchJitterMaxDegrees;
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
