package net.wafflingpenguin.wynnhorse.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.wafflingpenguin.wynnhorse.WynnHorse;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

public final class WynnHorseKeyMappings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(WynnHorse.MOD_ID, "controls")
    );

    public static final KeyMapping TOGGLE_AUTOMATION = new KeyMapping(
            "key.wynnhorse.toggle_automation",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_J,
            CATEGORY
    );

    public static final KeyMapping OPEN_WAYPOINT_MANAGER = new KeyMapping(
            "key.wynnhorse.open_waypoint_manager",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_O,
            CATEGORY
    );

    public static final KeyMapping ADD_CURRENT_WAYPOINT = new KeyMapping(
            "key.wynnhorse.add_current_waypoint",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_P,
            CATEGORY
    );

    private WynnHorseKeyMappings() {
    }

    public static void register(final RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_AUTOMATION);
        event.register(OPEN_WAYPOINT_MANAGER);
        event.register(ADD_CURRENT_WAYPOINT);
    }
}
