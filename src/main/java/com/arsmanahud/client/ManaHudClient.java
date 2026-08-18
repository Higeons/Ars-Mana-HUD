package com.arsmanahud.client;

import com.arsmanahud.ArsManaHud;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ArsManaHud.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ManaHudClient {

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        // registerAboveAll draws this layer after every vanilla and Ars Nouveau layer,
        // so the text always overlaps the mana bar instead of being hidden behind it.
        event.registerAboveAll("mana_hud_text", ManaHudRenderer::render);
    }
}
