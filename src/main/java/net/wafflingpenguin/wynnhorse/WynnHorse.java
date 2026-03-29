package net.wafflingpenguin.wynnhorse;

import net.wafflingpenguin.wynnhorse.client.WynnHorseClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(WynnHorse.MOD_ID)
public final class WynnHorse {
    public static final String MOD_ID = "wynnhorse";

    public WynnHorse(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
        context.registerConfig(ModConfig.Type.CLIENT, WynnHorseConfig.SPEC);

        if (FMLLoader.getDist() == Dist.CLIENT) {
            WynnHorseClient.register();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }
}
