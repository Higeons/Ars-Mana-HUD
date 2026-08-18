package com.arsmanahud;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;

@Mod(ArsManaHud.MODID)
public class ArsManaHud {
    public static final String MODID = "arsmanahud";

    public ArsManaHud() {
        // Client-only HUD logic lives in Dist.CLIENT event subscribers, so this
        // constructor deliberately references no client or Ars Nouveau classes.
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
