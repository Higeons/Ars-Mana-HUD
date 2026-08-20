package com.arsmanahud.jade;

import com.arsmanahud.ArsManaHud;
import com.hollingsworth.arsnouveau.common.block.PotionJar;
import com.hollingsworth.arsnouveau.common.block.SourceJar;
import com.hollingsworth.arsnouveau.common.block.SourcelinkBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade plugin entry point. Jade discovers this class through the
 * {@link WailaPlugin} annotation and calls {@link #registerClient} on the client.
 * <p>
 * Only client-side tooltip lines are added, so no common-side registration is
 * needed. Jade's hierarchy lookup walks superclasses: registering for
 * {@link SourceJar} also covers {@code CreativeSourceJar}, and registering for
 * {@link SourcelinkBlock} covers all five sourcelink (魔源通道) variants.
 */
@WailaPlugin(ArsManaHud.MODID)
public class ArsManaHudJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ContainerAmountProvider.INSTANCE, SourceJar.class);
        registration.registerBlockComponent(ContainerAmountProvider.INSTANCE, PotionJar.class);
        registration.registerBlockComponent(ContainerAmountProvider.INSTANCE, SourcelinkBlock.class);
    }
}
