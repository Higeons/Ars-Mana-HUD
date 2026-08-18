package com.arsmanahud.client;

import com.arsmanahud.ArsManaHud;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.util.ManaUtil;
import com.hollingsworth.arsnouveau.client.gui.GuiUtils;
import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import com.hollingsworth.arsnouveau.client.gui.buttons.CraftingButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Forge event bus subscribers for the client-side mana cost display features.
 * These events (ItemTooltipEvent, RenderTooltipEvent, ScreenEvent) fire on the FORGE bus,
 * unlike the MOD-bus overlay registration in {@link ManaHudClient}.
 */
@Mod.EventBusSubscriber(modid = ArsManaHud.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ArsManaHudEvents {

    private ArsManaHudEvents() {
    }

    /**
     * Casting cost of a single spell part (glyph), after config is applied.
     */
    public static int partCost(AbstractSpellPart part) {
        return part.getCastingCost();
    }

    /**
     * Estimated mana cost of casting the given spell with the given caster tool,
     * after the player's mana discounts. Mirrors GuiSpellBook#getCurrentManaCost.
     */
    public static int spellCost(LivingEntity caster, Spell spell, ItemStack casterStack) {
        return Math.max(spell.getCost() - ManaUtil.getPlayerDiscounts(caster, spell, casterStack), 0);
    }

    /**
     * Feature 1: append "Mana Cost: Xmana" right after the glyph level line of
     * glyph items in inventories and JEI. The glyph level line uses the same
     * localization key as the spell book GUI tooltip, so the insertion logic is shared.
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof Glyph glyph)) {
            return;
        }
        AbstractSpellPart part = glyph.spellPart;
        if (part == null) {
            return;
        }
        insertCostLineAfterLevel(event.getToolTip(), part,
                Component.translatable("hud." + ArsManaHud.MODID + ".cost", partCost(part)));
    }

    /**
     * Feature 2: append "Mana Cost: Xmana" right after the glyph level line when
     * hovering a glyph button in the spell book GUI while holding Shift.
     * The spell book tooltip is rendered via ModdedScreen#drawTooltip ->
     * GuiGraphics.renderComponentTooltip, which fires GatherComponents on the FORGE bus.
     * GatherComponents carries no mouse position, so the coords are derived from the
     * mouse handler the same way Minecraft computes screen coords, then divided by the
     * book scale factor like BaseBook does before hit-testing its widgets.
     */
    @SubscribeEvent
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof GuiSpellBook book)) {
            return;
        }
        if (!Screen.hasShiftDown()) {
            return;
        }
        Window window = minecraft.getWindow();
        int mouseX = (int) (minecraft.mouseHandler.xpos() * (double) window.getGuiScaledWidth() / (double) window.getScreenWidth());
        int mouseY = (int) (minecraft.mouseHandler.ypos() * (double) window.getGuiScaledHeight() / (double) window.getScreenHeight());
        if (book.scaleFactor != 1.0F) {
            mouseX = (int) (mouseX / book.scaleFactor);
            mouseY = (int) (mouseY / book.scaleFactor);
        }
        for (GlyphButton button : book.glyphButtons) {
            if (!GuiUtils.isMouseInRelativeRange(mouseX, mouseY, button)) {
                continue;
            }
            AbstractSpellPart part = GlyphRegistry.getSpellpartMap().get(button.abstractSpellPart.getRegistryName());
            if (part == null) {
                return;
            }
            Component costLine = Component.translatable("hud." + ArsManaHud.MODID + ".cost", partCost(part));
            String levelLine = Component.translatable("tooltip.ars_nouveau.glyph_level", part.getConfigTier().value).getString();
            List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
            for (int i = 0; i < elements.size(); i++) {
                Either<FormattedText, TooltipComponent> element = elements.get(i);
                if (!element.left().isPresent()) {
                    continue;
                }
                if (element.left().get().getString().equals(levelLine)) {
                    elements.add(i + 1, Either.left(costLine));
                    return;
                }
            }
            return;
        }
    }

    /**
     * Feature 3: render the current spell cost ("Xmana") on the mana bar inside the
     * spell book GUI. Reproduces the coordinate chain of
     * GuiSpellBook#drawBackgroundElements (translate by bookLeft/bookTop, then scale
     * 1.2 and translate -25/-30) so the text lands where the built-in debug numbers
     * would go, without requiring debug numbers to be enabled. The Post event fires
     * after the GUI content is drawn, so the text overlays the mana bar.
     * The cost is recomputed from the public crafting cells (GuiSpellBook#currentCostCache
     * is package-private), mirroring GuiSpellBook#getCurrentManaCost.
     */
    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof GuiSpellBook book)) {
            return;
        }
        Spell spell = new Spell();
        for (CraftingButton cell : book.craftingCells) {
            AbstractSpellPart part = cell.getAbstractSpellPart();
            if (part != null) {
                spell.add(part);
            }
        }
        GuiGraphics guiGraphics = event.getGuiGraphics();
        String text = Component.translatable("hud." + ArsManaHud.MODID + ".cost_short",
                spellCost(minecraft.player, spell, book.bookStack)).getString();
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(book.bookLeft, book.bookTop, 0);
        pose.scale(1.2F, 1.2F, 1.2F);
        pose.translate(-25.0F, -30.0F, 0.0F);
        // Centered on the 108px-wide mana bar, vertically at the bar's text line (200 in book coords).
        int x = 89 + (108 - minecraft.font.width(text)) / 2;
        guiGraphics.drawString(minecraft.font, text, x, 200, 0xFFFFFF, true);
        pose.popPose();
    }

    /**
     * Inserts the mana cost line right after the "A Level X Glyph." line
     * (localization key tooltip.ars_nouveau.glyph_level) of a glyph tooltip.
     * No-op when the glyph level line is absent (e.g. disabled glyphs).
     */
    private static void insertCostLineAfterLevel(List<Component> tooltip, AbstractSpellPart part, Component costLine) {
        String levelLine = Component.translatable("tooltip.ars_nouveau.glyph_level", part.getConfigTier().value).getString();
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().equals(levelLine)) {
                tooltip.add(i + 1, costLine);
                return;
            }
        }
    }
}