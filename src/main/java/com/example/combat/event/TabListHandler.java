package com.example.combat.event;

import com.example.combat.CombatClient;
import com.example.combat.modules.renderer.BetterTab;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class TabListHandler {

    private final Minecraft mc = Minecraft.getInstance();

    // Кэшируем reflection один раз
    private Field  fMaxPlayers = null;
    private boolean reflInit   = false;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.PLAYER_LIST) return;
        BetterTab bt = getBetterTab();
        if (bt == null || !bt.isEnabled()) return;

        // 1. Увеличиваем лимит игроков в таб-листе
        trySetMaxPlayers(bt.tabSize.getValue());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.PLAYER_LIST) return;
        BetterTab bt = getBetterTab();
        if (bt == null || !bt.isEnabled()) return;

        // 2. Рисуем поверх стандартного таб-листа кастомные имена
        if (!mc.options.keyPlayerList.isDown()) return;
        if (mc.level == null || mc.player == null) return;

        MatrixStack ms = event.getMatrixStack();
        FontRenderer font = mc.font;

        // Получаем список игроков отсортированный как в vanilla
        Collection<NetworkPlayerInfo> players = mc.getConnection().getOnlinePlayers();
        List<NetworkPlayerInfo> sorted = new ArrayList<>(players);
        sorted.sort(Comparator.comparing(p -> {
            ITextComponent name = p.getTabListDisplayName();
            return name != null ? name.getString() : p.getProfile().getName();
        }));

        // Параметры сетки таб-листа (vanilla использует похожие)
        int screenW = mc.getWindow().getGuiScaledWidth();
        int cols     = Math.max(1, (sorted.size() + bt.tabHeight.getValue() - 1) / bt.tabHeight.getValue());
        int rows     = Math.min(sorted.size(), bt.tabHeight.getValue());
        int colW     = Math.min(210, (screenW - 50) / cols);
        int startX   = (screenW - colW * cols) / 2;
        int startY   = 10;

        for (int i = 0; i < sorted.size(); i++) {
            NetworkPlayerInfo info = sorted.get(i);
            int col = i / rows;
            int row = i % rows;

            int px = startX + col * colW + 2;
            int py = startY + 20 + row * 9;

            ITextComponent displayName = bt.getPlayerName(info);

            // Рисуем поверх стандартного (с тенью)
            // Небольшой фон чтобы перекрыть старый текст
            int bgColor = 0x80000000;
            net.minecraft.client.gui.AbstractGui.fill(ms, px - 1, py - 1, px + colW - 2, py + 8, bgColor);

            font.drawWithShadow(ms, displayName, px, py, 0xFFFFFF);

            // Пинг справа (если включен)
            if (bt.pingNum.getValue()) {
                int ping = info.getLatency();
                String pingStr = ping + "ms";
                int pingColor = ping < 100 ? 0x55FF55 : ping < 200 ? 0xFFFF55 : 0xFF5555;
                font.drawWithShadow(ms, pingStr, px + colW - font.width(pingStr) - 2, py, pingColor);
            }
        }
    }

    private void trySetMaxPlayers(int size) {
        // PlayerTabOverlayGui API not available in Forge 1.16.5 - no-op
    }

    private BetterTab getBetterTab() {
        if (CombatClient.moduleManager == null) return null;
        return (BetterTab) CombatClient.moduleManager.getByName("BetterTab");
    }
}
