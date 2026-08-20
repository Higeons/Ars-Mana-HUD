package com.arsmanahud.client;

import com.arsmanahud.ArsManaHud;
import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.ISpellCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.util.CasterUtil;
import com.hollingsworth.arsnouveau.api.util.ManaUtil;
import com.hollingsworth.arsnouveau.client.gui.GuiManaHUD;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.setup.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.Locale;

public final class ManaHudRenderer {
    /** Width of the Ars Nouveau mana bar border sprite, in pixels. */
    private static final int MANA_BAR_WIDTH = 108;
    /** Gap between the right edge of the mana bar and the regen text, in pixels. */
    private static final int REGEN_TEXT_GAP = 4;
    /** Vertical distance between the top of the mana bar and the spell cost text above it. */
    private static final int SPELL_COST_ABOVE_BAR = 28;

    private static boolean initialized;
    private static boolean lastVisible;
    private static double lastMana = Double.NaN;
    private static int lastMaxMana = -1;
    private static double lastRegen = Double.NaN;
    private static String cachedManaText = "";
    private static String cachedRegenText = "";

    private ManaHudRenderer() {
    }

    public static void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        boolean visible = GuiManaHUD.shouldDisplayBar();
        if (visible != lastVisible) {
            // Force a rebuild whenever the bar appears or disappears.
            initialized = false;
            lastVisible = visible;
        }
        if (!visible) {
            return;
        }

        double mana = ManaUtil.getCurrentMana(minecraft.player);
        int maxMana = ManaUtil.getMaxMana(minecraft.player);
        if (maxMana <= 0) {
            return;
        }
        double regen = ManaUtil.getManaRegen(minecraft.player);

        // Refresh the mana text only when the mana or the max mana actually changed.
        if (!initialized || mana != lastMana || maxMana != lastMaxMana) {
            cachedManaText = Component.translatable("hud." + ArsManaHud.MODID + ".mana", (int) mana, maxMana).getString();
            lastMana = mana;
            lastMaxMana = maxMana;
        }
        // Refresh the regen text only when the regen rate actually changed.
        if (!initialized || regen != lastRegen) {
            cachedRegenText = Component.translatable(
                    "hud." + ArsManaHud.MODID + ".regen",
                    String.format(Locale.ROOT, "%.1f", regen)).getString();
            lastRegen = regen;
        }
        initialized = true;

        // Mirror the coordinate system of Ars Nouveau's GuiManaHUD so the text follows
        // the bar, including the user-configurable MANABAR_X_OFFSET / MANABAR_Y_OFFSET.
        int offsetLeft = 10 + Config.MANABAR_X_OFFSET.get();
        int yOffset = minecraft.getWindow().getGuiScaledHeight() - 5 + Config.MANABAR_Y_OFFSET.get();
        int textY = yOffset - 10;

        // Mana text, horizontally centered over the bar.
        int manaTextX = offsetLeft + (MANA_BAR_WIDTH - minecraft.font.width(cachedManaText)) / 2;
        guiGraphics.drawString(minecraft.font, cachedManaText, manaTextX, textY, 0xFFFFFF, true);

        // Regen text, to the right of the bar.
        int regenTextX = offsetLeft + MANA_BAR_WIDTH + REGEN_TEXT_GAP;
        guiGraphics.drawString(minecraft.font, cachedRegenText, regenTextX, textY, 0xFFFFFF, true);

        // Feature 4: estimated mana cost of the selected spell for held caster tools.
        renderSpellCost(minecraft, guiGraphics, offsetLeft, yOffset);
    }

    /**
     * Feature 4: render the estimated mana cost of the selected spell for the held
     * caster tool (e.g. Enchanter's Eye, Caster Tome). For spell books the cost is
     * drawn to the right of the spell name line rendered by GuiSpellHUD (which sits
     * above the mana bar); for other caster tools it is drawn directly above the bar.
     */
    private static void renderSpellCost(Minecraft minecraft, GuiGraphics guiGraphics, int offsetLeft, int yOffset) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        ItemStack casterStack = mainHand.getItem() instanceof ICasterTool ? mainHand : ItemStack.EMPTY;
        if (casterStack.isEmpty()) {
            ItemStack offHand = minecraft.player.getOffhandItem();
            if (offHand.getItem() instanceof ICasterTool) {
                casterStack = offHand;
            }
        }
        if (casterStack.isEmpty()) {
            return;
        }

        ISpellCaster caster = CasterUtil.getCaster(casterStack);
        Spell spell = caster.getSpell(caster.getCurrentSlot());
        if (spell == null || spell.isEmpty()) {
            return;
        }

        int cost = ArsManaHudEvents.spellCost(minecraft.player, spell, casterStack);
        String text = Component.translatable("hud." + ArsManaHud.MODID + ".cost", cost).getString();

        if (casterStack.getItem() instanceof SpellBook) {
            // To the right of the spell name rendered by GuiSpellHUD (x=10, y=screenH-30).
            // Drawn without shadow to match GuiSpellHUD's rendering, so the cost text
            // stays pixel-aligned with the "spell number + spell name" line.
            String name = caster.getCurrentSlot() + 1 + " " + caster.getSpellName();
            int x = 10 + minecraft.font.width(name) + REGEN_TEXT_GAP;
            int y = minecraft.getWindow().getGuiScaledHeight() - 30;
            guiGraphics.drawString(minecraft.font, text, x, y, 0xFFFFFF, false);
        } else {
            // Above the mana bar.
            guiGraphics.drawString(minecraft.font, text, offsetLeft, yOffset - SPELL_COST_ABOVE_BAR, 0xFFFFFF, true);
        }
    }
}
