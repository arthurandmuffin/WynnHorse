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

    private WynnHorseKeyMappings() {
    }

    public static void register(final RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_AUTOMATION);
    }
}
