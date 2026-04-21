package com.example.combat.modules.hud;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class HUDModule extends Module {

    public final Setting<Boolean> showFPS    = new Setting<>("FPS", true);
    public final Setting<Boolean> showCoords = new Setting<>("Coords", true);
    public final Setting<Boolean> showBiome  = new Setting<>("Biome", true);
    public final Setting<Boolean> showSpeed  = new Setting<>("Speed", true);
    public final Setting<Integer> x          = new Setting<>("X", 2).range(0, 400);
    public final Setting<Integer> y          = new Setting<>("Y", 2).range(0, 300);

    public HUDModule() {
        super("HUD", "Displays client info on screen", Category.HUD);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        FontRenderer fr = mc.fontRenderer;
        MatrixStack ms  = event.getMatrixStack();
        List<String> lines = new ArrayList<>();

        lines.add(TextFormatting.AQUA + "§lCombatClient");

        if (showFPS.getValue())
            lines.add(TextFormatting.GRAY + "FPS: " + TextFormatting.WHITE + mc.getFPS());

        if (showCoords.getValue()) {
            int bx = (int) mc.player.getPosX();
            int by = (int) mc.player.getPosY();
            int bz = (int) mc.player.getPosZ();
            lines.add(TextFormatting.GRAY + "XYZ: " + TextFormatting.WHITE + bx + " " + by + " " + bz);
        }

        if (showBiome.getValue()) {
            String biome = mc.world.getBiome(mc.player.getPosition())
                    .getRegistryName() != null
                    ? mc.world.getBiome(mc.player.getPosition()).getRegistryName().getPath()
                    : "unknown";
            lines.add(TextFormatting.GRAY + "Biome: " + TextFormatting.WHITE + biome);
        }

        if (showSpeed.getValue()) {
            double spd = Math.sqrt(mc.player.getMotion().x * mc.player.getMotion().x
                    + mc.player.getMotion().z * mc.player.getMotion().z);
            lines.add(TextFormatting.GRAY + "Speed: " + TextFormatting.WHITE + String.format("%.2f", spd));
        }

        int startX = x.getValue();
        int startY = y.getValue();
        for (int i = 0; i < lines.size(); i++) {
            fr.drawStringWithShadow(ms, lines.get(i), startX, startY + i * 10, 0xFFFFFF);
        }
    }
}
