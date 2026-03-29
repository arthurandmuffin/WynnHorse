package net.wafflingpenguin.wynnhorse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.wafflingpenguin.wynnhorse.automation.AutomationController;
import net.wafflingpenguin.wynnhorse.client.gui.WaypointManagerScreen;
import net.wafflingpenguin.wynnhorse.client.render.WaypointRenderer;
import net.wafflingpenguin.wynnhorse.waypoint.Waypoint;
import net.wafflingpenguin.wynnhorse.waypoint.WaypointStore;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;

public final class WynnHorseClient {
    private static final AutomationController AUTOMATION_CONTROLLER = new AutomationController();
    private static final WaypointStore WAYPOINT_STORE = new WaypointStore();

    private static boolean registered;

    private WynnHorseClient() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        RegisterKeyMappingsEvent.BUS.addListener(WynnHorseKeyMappings::register);
        TickEvent.ClientTickEvent.Post.BUS.addListener(WynnHorseClient::onClientTick);
        ViewportEvent.ComputeCameraAngles.BUS.addListener(WaypointRenderer::collectWaypointGizmos);
    }

    public static WaypointStore getWaypointStore() {
        return WAYPOINT_STORE;
    }

    private static void onClientTick(final TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

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
            minecraft.player.sendOverlayMessage(Component.translatable("message.wynnhorse.waypoint_added", waypoint.name()));
        }

        while (WynnHorseKeyMappings.TOGGLE_AUTOMATION.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null) {
                continue;
            }

            if (!AUTOMATION_CONTROLLER.isEnabled() && WAYPOINT_STORE.route().getCurrentWaypoint().isEmpty()) {
                minecraft.player.sendOverlayMessage(Component.translatable("message.wynnhorse.automation.no_target"));
                continue;
            }

            boolean enabled = AUTOMATION_CONTROLLER.toggle(minecraft);
            minecraft.player.sendOverlayMessage(
                    Component.translatable(enabled ? "message.wynnhorse.automation.enabled" : "message.wynnhorse.automation.disabled")
            );
        }

        Component automationUpdate = AUTOMATION_CONTROLLER.tick(minecraft, WAYPOINT_STORE.route());
        if (automationUpdate != null && minecraft.player != null) {
            minecraft.player.sendOverlayMessage(automationUpdate);
        }
    }

    private static Waypoint addCurrentWaypoint(final Minecraft minecraft) {
        String name = "Waypoint " + (WAYPOINT_STORE.route().size() + 1);
        Waypoint waypoint = WAYPOINT_STORE.route().addWaypoint(name, minecraft.player.position());
        WAYPOINT_STORE.route().setActiveIndex(WAYPOINT_STORE.route().size() - 1);
        return waypoint;
    }
}
