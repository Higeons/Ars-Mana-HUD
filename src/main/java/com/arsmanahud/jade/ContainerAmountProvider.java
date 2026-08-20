package com.arsmanahud.jade;

import com.arsmanahud.ArsManaHud;
import com.hollingsworth.arsnouveau.api.potion.PotionData;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.common.block.Relay;
import com.hollingsworth.arsnouveau.common.block.tile.PotionJarTile;
import com.hollingsworth.arsnouveau.common.block.tile.SourceJarTile;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Adds exact container amounts to the Jade tooltip for Ars Nouveau containers.
 * <ul>
 *   <li>Source jars (incl. creative): "魔源量：X/Y"</li>
 *   <li>Potion jars: "XX药水：X/Y mB" (only when a potion is stored)</li>
 *   <li>Sourcelinks (魔源通道): "缓存量：X/Y" (their internal source cache)</li>
 *   <li>Mana relays (魔源中继器, all 5 variants): "缓存量：X/Y" + "吞吐量：X/s"</li>
 * </ul>
 * The line is drawn directly above the mod-name line: Jade's mod name provider
 * uses priority 9999, so this provider uses 9998 and appends a regular line,
 * which places it one line above the mod name.
 */
public enum ContainerAmountProvider implements IBlockComponentProvider {
    INSTANCE;

    /** One step below Jade's mod-name provider (9999) so the line sits above it. */
    private static final int PRIORITY_BELOW_MOD_NAME = 9998;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity == null) {
            return;
        }

        if (blockEntity instanceof SourceJarTile sourceJar) {
            tooltip.add(Component.translatable(
                    "hud." + ArsManaHud.MODID + ".jade.source",
                    sourceJar.getSource(), sourceJar.getMaxSource()));
            return;
        }
        if (blockEntity instanceof PotionJarTile potionJar) {
            PotionData data = potionJar.getData();
            // An empty jar has no potion type to name, so there is nothing useful to add.
            if (data == null || data.getPotion() == Potions.EMPTY) {
                return;
            }
            tooltip.add(Component.translatable(
                    "hud." + ArsManaHud.MODID + ".jade.potion",
                    data.asPotionStack().getHoverName(),
                    potionJar.getAmount(), potionJar.getMaxFill()));
            return;
        }
        if (blockEntity instanceof ISourceTile sourceTile) {
            // Sourcelinks (魔源通道) and mana relays (魔源中继器) both buffer source.
            // The relay tile class itself references geckolib, so relays are
            // recognized through their block superclass instead (all five relay
            // variants extend Relay; the provider is only registered for those blocks).
            tooltip.add(Component.translatable(
                    "hud." + ArsManaHud.MODID + ".jade.cache",
                    sourceTile.getSource(), sourceTile.getMaxSource()));
            if (accessor.getBlock() instanceof Relay) {
                tooltip.add(Component.translatable(
                        "hud." + ArsManaHud.MODID + ".jade.throughput",
                        sourceTile.getTransferRate()));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ArsManaHud.prefix("container_amount");
    }

    @Override
    public int getDefaultPriority() {
        return PRIORITY_BELOW_MOD_NAME;
    }
}
