package com.arsmanahud.jade;

import com.arsmanahud.ArsManaHud;
import com.hollingsworth.arsnouveau.api.potion.PotionData;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
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

        Component amountLine = null;
        if (blockEntity instanceof SourceJarTile sourceJar) {
            amountLine = Component.translatable(
                    "hud." + ArsManaHud.MODID + ".jade.source",
                    sourceJar.getSource(), sourceJar.getMaxSource());
        } else if (blockEntity instanceof PotionJarTile potionJar) {
            PotionData data = potionJar.getData();
            // An empty jar has no potion type to name, so there is nothing useful to add.
            if (data == null || data.getPotion() == Potions.EMPTY) {
                return;
            }
            amountLine = Component.translatable(
                    "hud." + ArsManaHud.MODID + ".jade.potion",
                    data.asPotionStack().getHoverName(),
                    potionJar.getAmount(), potionJar.getMaxFill());
        } else if (blockEntity instanceof ISourceTile sourceTile) {
            // This provider is only registered for SourcelinkBlock (魔源通道), whose
            // tiles expose their internal source cache through ISourceTile.
            amountLine = Component.translatable(
                    "hud." + ArsManaHud.MODID + ".jade.cache",
                    sourceTile.getSource(), sourceTile.getMaxSource());
        }

        if (amountLine != null) {
            tooltip.add(amountLine);
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
