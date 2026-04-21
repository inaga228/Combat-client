package com.example.combat.modules.hud;

import com.example.combat.modules.Module;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class Notifications extends Module {

    public record Notif(String text, long expiry, int color) {}

    private static final Queue<Notif> queue = new LinkedList<>();

    public static void push(String text, int color, long durationMs) {
        queue.add(new Notif(text, System.currentTimeMillis() + durationMs, color));
    }

    public static void push(String text) {
        push(text, 0x00FFAA, 3000);
    }

    public Notifications() {
        super("Notifications", "Shows module toggle notifications on screen", Category.HUD);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled()) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        long now = System.currentTimeMillis();
        queue.removeIf(n -> now > n.expiry());

        MatrixStack ms = event.getMatrixStack();
        FontRenderer fr = mc.fontRenderer;
        int sw = mc.getMainWindow().getScaledWidth();
        int i = 0;
        for (Notif n : queue) {
            fr.drawStringWithShadow(ms, n.text(), sw - fr.getStringWidth(n.text()) - 4, 4 + i * 12, n.color());
            i++;
        }
    }
}
