package com.arsmanahud.jade;

import com.hollingsworth.arsnouveau.common.block.PotionJar;
import com.hollingsworth.arsnouveau.common.block.Relay;
import com.hollingsworth.arsnouveau.common.block.SourceJar;
import com.hollingsworth.arsnouveau.common.block.SourcelinkBlock;
import net.neoforged.fml.ModList;
import snownee.jade.impl.WailaClientRegistration;

/**
 * Registers the container-amount provider straight on Jade's client registration
 * singleton, bypassing the {@code @WailaPlugin} annotation scan. Jade discovers
 * {@link ArsManaHudJadePlugin} through that scan in {@code CommonProxy.loadComplete},
 * but some packs replace the scan with a hard-coded plugin whitelist via a Mixin,
 * which would otherwise leave the container-amount lines unregistered.
 * <p>
 * Jade 15.x made the singleton field private, so the registration goes through
 * {@link WailaClientRegistration#instance()}. Registering outside an active
 * registration session is supported by Jade and writes straight into the
 * hierarchy lookup, which is the path used here.
 * <p>
 * The regular {@link ArsManaHudJadePlugin} scan path is kept as well; registering
 * the same provider twice is harmless because {@link ContainerAmountProvider}
 * removes its tagged lines before adding them.
 */
public final class JadeDirectRegistration {

    private JadeDirectRegistration() {
    }

    /** Must only run on the physical client. No-op when Jade is not installed. */
    public static void registerIfPresent() {
        if (!ModList.get().isLoaded("jade")) {
            return;
        }
        WailaClientRegistration registration = WailaClientRegistration.instance();
        registration.registerBlockComponent(ContainerAmountProvider.INSTANCE, SourceJar.class);
        registration.registerBlockComponent(ContainerAmountProvider.INSTANCE, PotionJar.class);
        registration.registerBlockComponent(ContainerAmountProvider.INSTANCE, SourcelinkBlock.class);
        registration.registerBlockComponent(ContainerAmountProvider.INSTANCE, Relay.class);
    }
}
