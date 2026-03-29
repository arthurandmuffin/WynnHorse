package net.wafflingpenguin.wynnhorse.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.wafflingpenguin.wynnhorse.automation.AutomationController;
import net.wafflingpenguin.wynnhorse.waypoint.WaypointStore;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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
    }

    public static WaypointStore getWaypointStore() {
        return WAYPOINT_STORE;
    }

    private static void onClientTick(final TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        while (WynnHorseKeyMappings.TOGGLE_AUTOMATION.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null) {
                continue;
            }

            boolean enabled = AUTOMATION_CONTROLLER.toggle(minecraft);
            minecraft.player.sendOverlayMessage(
                    Component.translatable(enabled ? "message.wynnhorse.automation.enabled" : "message.wynnhorse.automation.disabled")
            );
        }

        AUTOMATION_CONTROLLER.tick(minecraft);
    }
}
