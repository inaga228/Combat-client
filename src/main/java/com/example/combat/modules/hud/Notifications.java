package com.example.combat.modules.hud;

import com.example.combat.modules.Module;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class Notifications extends Module {

    public static class Notif {
        private final String text;
        private final long   expiry;
        private final int    color;
        public Notif(String text, long expiry, int color) {
            this.text = text; this.expiry = expiry; this.color = color;
        }
        public String getText()   { return text; }
        public long   getExpiry() { return expiry; }
        public int    getColor()  { return color; }
    }

    private static final Queue<Notif> queue = new LinkedList<>();

    public static void push(String text, int color, long durationMs) {
        queue.add(new Notif(text, System.currentTimeMillis() + durationMs, color));
    }

    public static void push(String text) {
        push(text, 0x00FFAA, 3000);
    }

    public Notifications() {
        super("Notifications", "Shows module toggle notifications", Category.HUD);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled()) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        long now = System.currentTimeMillis();
        Iterator<Notif> it = queue.iterator();
        while (it.hasNext()) { if (now > it.next().getExpiry()) it.remove(); }

        MatrixStack ms = event.getMatrixStack();
        int sw = mc.getWindow().getGuiScaledWidth();
        int i  = 0;
        for (Notif n : queue) {
            int tw = mc.font.width(n.getText());
            mc.font.drawWithShadow(ms, n.getText(), sw - tw - 4, 4 + i * 12, n.getColor());
            i++;
        }
    }
}
