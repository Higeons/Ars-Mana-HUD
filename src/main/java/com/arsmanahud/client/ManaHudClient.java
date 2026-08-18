package com.arsmanahud.client;

import com.arsmanahud.ArsManaHud;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = ArsManaHud.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ManaHudClient {

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // registerAboveAll draws this layer after every vanilla and Ars Nouveau layer,
        // so the text always overlaps the mana bar instead of being hidden behind it.
        event.registerAboveAll(ArsManaHud.prefix("mana_hud_text"), ManaHudRenderer::render);
    }
}
