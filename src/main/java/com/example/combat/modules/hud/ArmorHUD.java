package com.example.combat.modules.hud;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ArmorHUD extends Module {

    public final Setting<Integer> x = new Setting<>("X", 2).range(0, 400);
    public final Setting<Integer> y = new Setting<>("Y", 60).range(0, 300);

    public ArmorHUD() {
        super("ArmorHUD", "Shows armor durability on screen", Category.HUD);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || mc.player == null) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        MatrixStack ms = event.getMatrixStack();
        FontRenderer fr = mc.fontRenderer;
        int drawX = x.getValue();
        int drawY = y.getValue();

        Iterable<ItemStack> armor = mc.player.getArmorInventoryList();
        int i = 0;
        for (ItemStack stack : armor) {
            if (stack.isEmpty()) { i++; continue; }
            mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, drawX, drawY - i * 18);
            if (stack.isDamageable()) {
                int dur = stack.getMaxDamage() - stack.getDamage();
                float pct = (float) dur / stack.getMaxDamage();
                int color = pct > 0.5f ? 0x00FF00 : pct > 0.25f ? 0xFFFF00 : 0xFF0000;
                fr.drawStringWithShadow(ms, String.valueOf(dur), drawX + 18, drawY - i * 18 + 5, color);
            }
            i++;
        }
    }
}
