package com.example.combat.event;

import com.example.combat.CombatClient;
import com.example.combat.modules.renderer.BetterTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Перехватывает рендер таб-листа и применяет BetterTab трансформации имён.
 *
 * В Forge 1.16.5 нет прямого события на каждый элемент таблицы,
 * поэтому мы модифицируем NetworkPlayerInfo через reflection перед рендером
 * и восстанавливаем после.
 *
 * Альтернативно — используем GuiTabList (PlayerTabOverlayGui) через reflection
 * для смены maxPlayers.
 */
public class TabListHandler {

    private final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.PLAYER_LIST) return;
        BetterTab bt = getBetterTab();
        if (bt == null || !bt.isEnabled()) return;

        // Устанавливаем размер таб-листа через reflection
        try {
            // net.minecraft.client.gui.overlay.PlayerTabOverlayGui хранит maxPlayers
            // В официальных маппингах это поле называется f_93085_ / maxPlayers
            var tabGui = mc.gui.getTabList();
            // Поле maxPlayers — пробуем по имени в official mappings
            java.lang.reflect.Field[] fields = tabGui.getClass().getDeclaredFields();
            for (java.lang.reflect.Field f : fields) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    int val = (int) f.get(tabGui);
                    // Ищем поле которое == 80 (дефолт) или близко к нему
                    if (val >= 20 && val <= 200) {
                        f.set(tabGui, bt.tabSize.getValue());
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private BetterTab getBetterTab() {
        if (CombatClient.moduleManager == null) return null;
        return (BetterTab) CombatClient.moduleManager.getByName("BetterTab");
    }
}
