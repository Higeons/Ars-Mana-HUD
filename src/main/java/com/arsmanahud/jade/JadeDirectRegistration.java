package com.arsmanahud.jade;

import com.hollingsworth.arsnouveau.common.block.PotionJar;
import com.hollingsworth.arsnouveau.common.block.Relay;
import com.hollingsworth.arsnouveau.common.block.SourceJar;
import com.hollingsworth.arsnouveau.common.block.SourcelinkBlock;
import net.minecraftforge.fml.ModList;
import snownee.jade.impl.WailaClientRegistration;

/**
 * Registers the container-amount provider straight on Jade's client registration
 * singleton, bypassing the {@code @WailaPlugin} annotation scan. Jade discovers
 * {@link ArsManaHudJadePlugin} through that scan in {@code CommonProxy.loadComplete},
 * but some packs (e.g. GregTech Odyssey's gtocore) replace the scan with a
 * hard-coded plugin whitelist via a Mixin, which would otherwise leave the
 * container-amount lines unregistered.
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
        WailaClientRegistration.INSTANCE.registerBlockComponent(ContainerAmountProvider.INSTANCE, SourceJar.class);
        WailaClientRegistration.INSTANCE.registerBlockComponent(ContainerAmountProvider.INSTANCE, PotionJar.class);
        WailaClientRegistration.INSTANCE.registerBlockComponent(ContainerAmountProvider.INSTANCE, SourcelinkBlock.class);
        WailaClientRegistration.INSTANCE.registerBlockComponent(ContainerAmountProvider.INSTANCE, Relay.class);
    }
}