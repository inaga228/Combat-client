package com.example.combat.event;

import com.example.combat.CombatClient;
import com.example.combat.modules.renderer.BetterTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.overlay.PlayerTabOverlayGui;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;

public class TabListHandler {

    private final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.PLAYER_LIST) return;
        BetterTab bt = getBetterTab();
        if (bt == null || !bt.isEnabled()) return;

        // Меняем maxPlayers через reflection (нет прямого API)
        PlayerTabOverlayGui tabGui = mc.gui.getTabList();
        try {
            for (Field f : PlayerTabOverlayGui.class.getDeclaredFields()) {
                if (f.getType() != int.class) continue;
                f.setAccessible(true);
                int val = (int) f.get(tabGui);
                if (val >= 20 && val <= 200) {
                    f.set(tabGui, bt.tabSize.getValue());
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private BetterTab getBetterTab() {
        if (CombatClient.moduleManager == null) return null;
        return (BetterTab) CombatClient.moduleManager.getByName("BetterTab");
    }
}
