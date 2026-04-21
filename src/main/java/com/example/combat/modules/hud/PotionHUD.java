package com.example.combat.modules.hud;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;

public class PotionHUD extends Module {

    public final Setting<Integer> x = new Setting<>("X", 2).range(0, 400);
    public final Setting<Integer> y = new Setting<>("Y", 140).range(0, 300);

    public PotionHUD() {
        super("PotionHUD", "Shows active potion effects and duration", Category.HUD);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || mc.player == null) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        MatrixStack ms = event.getMatrixStack();
        FontRenderer fr = mc.fontRenderer;
        Collection<EffectInstance> effects = mc.player.getActivePotionEffects();
        int i = 0;
        for (EffectInstance eff : effects) {
            Effect effect = eff.getPotion();
            String name = effect.getDisplayName().getString();
            int dur = eff.getDuration();
            int secs = dur / 20;
            String time = secs >= 60 ? (secs / 60) + "m" + (secs % 60) + "s" : secs + "s";
            String line = name + " " + (eff.getAmplifier() + 1) + " (" + time + ")";
            fr.drawStringWithShadow(ms, line, x.getValue(), y.getValue() + i * 10, 0xFFFFFF);
            i++;
        }
    }
}
