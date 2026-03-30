package net.wafflingpenguin.wynnhorse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.wafflingpenguin.wynnhorse.automation.AutomationController;
import net.wafflingpenguin.wynnhorse.client.gui.WaypointManagerScreen;
import net.wafflingpenguin.wynnhorse.client.hud.HorseMatchOverlay;
import net.wafflingpenguin.wynnhorse.client.render.WaypointRenderer;
import net.wafflingpenguin.wynnhorse.horse.HorseItemTracker;
import net.wafflingpenguin.wynnhorse.waypoint.Waypoint;
import net.wafflingpenguin.wynnhorse.waypoint.WaypointStore;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;

public final class WynnHorseClient {
    private static final AutomationController AUTOMATION_CONTROLLER = new AutomationController();
    private static final HorseItemTracker HORSE_ITEM_TRACKER = new HorseItemTracker();
    private static final WaypointStore WAYPOINT_STORE = new WaypointStore();
    private static final int STATUS_MESSAGE_DURATION_TICKS = 20;

    private static boolean registered;
    private static Component statusOverlayMessage;
    private static int statusOverlayTicksRemaining;

    private WynnHorseClient() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        RegisterKeyMappingsEvent.BUS.addListener(WynnHorseKeyMappings::register);
        CustomizeGuiOverlayEvent.Chat.BUS.addListener(HorseMatchOverlay::render);
        TickEvent.ClientTickEvent.Post.BUS.addListener(WynnHorseClient::onClientTick);
        ViewportEvent.ComputeCameraAngles.BUS.addListener(WaypointRenderer::collectWaypointGizmos);
    }

    public static WaypointStore getWaypointStore() {
        return WAYPOINT_STORE;
    }

    public static HorseItemTracker getHorseItemTracker() {
        return HORSE_ITEM_TRACKER;
    }

    public static boolean isAutomationEnabled() {
        return AUTOMATION_CONTROLLER.isEnabled();
    }

    public static Component getStatusOverlayMessage() {
        return statusOverlayTicksRemaining > 0 ? statusOverlayMessage : null;
    }

    public static void showStatusOverlay(final Component message) {
        statusOverlayMessage = message;
        statusOverlayTicksRemaining = STATUS_MESSAGE_DURATION_TICKS;
    }

    private static void onClientTick(final TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (statusOverlayTicksRemaining > 0) {
            statusOverlayTicksRemaining--;
            if (statusOverlayTicksRemaining == 0) {
                statusOverlayMessage = null;
            }
        }

        while (WynnHorseKeyMappings.OPEN_WAYPOINT_MANAGER.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null) {
                continue;
            }

            minecraft.setScreen(new WaypointManagerScreen());
        }

        while (WynnHorseKeyMappings.ADD_CURRENT_WAYPOINT.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
                continue;
            }

            Waypoint waypoint = addCurrentWaypoint(minecraft);
            showStatusOverlay(Component.translatable("message.wynnhorse.waypoint_added", waypoint.name()));
        }

        while (WynnHorseKeyMappings.TOGGLE_AUTOMATION.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null) {
                continue;
            }

            if (!AUTOMATION_CONTROLLER.isEnabled() && WAYPOINT_STORE.route().getCurrentWaypoint().isEmpty()) {
                showStatusOverlay(Component.translatable("message.wynnhorse.automation.no_target"));
                continue;
            }

            HORSE_ITEM_TRACKER.refresh(minecraft);

            boolean enabled = AUTOMATION_CONTROLLER.toggle(minecraft);
            if (!enabled) {
                HORSE_ITEM_TRACKER.clearMountedHorseState();
            } else {
                HORSE_ITEM_TRACKER.clearMountedHorseState();
            }
            showStatusOverlay(
                    Component.translatable(enabled ? "message.wynnhorse.automation.enabled" : "message.wynnhorse.automation.disabled")
            );
        }

        HORSE_ITEM_TRACKER.refresh(minecraft);

        Component automationUpdate = AUTOMATION_CONTROLLER.tick(minecraft, WAYPOINT_STORE.route(), HORSE_ITEM_TRACKER);
        if (!AUTOMATION_CONTROLLER.isEnabled()) {
            HORSE_ITEM_TRACKER.clearMountedHorseState();
        }
        if (automationUpdate != null) {
            showStatusOverlay(automationUpdate);
        }
    }

    private static Waypoint addCurrentWaypoint(final Minecraft minecraft) {
        String name = "Waypoint " + (WAYPOINT_STORE.route().size() + 1);
        Waypoint waypoint = WAYPOINT_STORE.route().addWaypoint(name, minecraft.player.position());
        WAYPOINT_STORE.route().setActiveIndex(WAYPOINT_STORE.route().size() - 1);
        return waypoint;
    }

}
