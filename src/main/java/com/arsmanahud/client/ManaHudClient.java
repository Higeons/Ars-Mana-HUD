package com.arsmanahud.client;

import com.arsmanahud.ArsManaHud;
import com.arsmanahud.jade.JadeDirectRegistration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ArsManaHud.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ManaHudClient {

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        // registerAboveAll draws this layer after every vanilla and Ars Nouveau layer,
        // so the text always overlaps the mana bar instead of being hidden behind it.
        event.registerAboveAll("mana_hud_text", ManaHudRenderer::render);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Fires before Jade's FMLLoadCompleteEvent plugin scan, so the direct
        // registration is already in place when Jade builds its provider lists.
        // No-op when Jade is not installed.
        JadeDirectRegistration.registerIfPresent();
    }
}
