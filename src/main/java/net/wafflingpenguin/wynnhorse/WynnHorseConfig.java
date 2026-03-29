package net.wafflingpenguin.wynnhorse;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = WynnHorse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WynnHorseConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> HORSE_ITEM_DISPLAY_NAME = BUILDER
            .comment("Display-name fragment used to find the horse spawn item in the hotbar.")
            .define("horseItemDisplayName", "Horse");

    private static final ForgeConfigSpec.DoubleValue WAYPOINT_REACHED_DISTANCE = BUILDER
            .comment("Distance in blocks considered close enough to reach a waypoint.")
            .defineInRange("waypointReachedDistance", 2.0D, 0.25D, 16.0D);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String horseItemDisplayName = HORSE_ITEM_DISPLAY_NAME.get();
    public static double waypointReachedDistance = WAYPOINT_REACHED_DISTANCE.get();

    private WynnHorseConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        horseItemDisplayName = HORSE_ITEM_DISPLAY_NAME.get().trim();
        waypointReachedDistance = WAYPOINT_REACHED_DISTANCE.get();
    }
}
