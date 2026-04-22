package com.example.combat.modules.hud;

import com.example.combat.CombatClient;
import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.example.combat.utils.RenderUtil;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleListHud extends Module {

    public final Setting<Boolean> showBackground = new Setting<>("Background", true);
    public final Setting<Boolean> sortByLength   = new Setting<>("SortByLen",  true);

    // Цвет полоски слева (RGB без альфы)
    private static final int ACCENT = 0xFF00FFAA;
    private static final int BG     = 0x80000000;
    private static final int TEXT   = 0xFFFFFFFF;

    public ModuleListHud() {
        super("ModuleList", "Shows enabled modules on screen", Category.HUD);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled()) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (CombatClient.moduleManager == null) return;

        List<Module> active = CombatClient.moduleManager.getModules().stream()
            .filter(m -> m.isEnabled() && !(m instanceof ModuleListHud)
                      && !(m instanceof Notifications))
            .collect(Collectors.toList());

        if (sortByLength.getValue()) {
            active.sort(Comparator.comparingInt(
                m -> -mc.font.width(m.getName())));
        }

        MatrixStack ms = event.getMatrixStack();
        int sw = mc.getWindow().getGuiScaledWidth();
        int y  = 2;

        for (Module m : active) {
            String name = m.getName();
            int tw = mc.font.width(name);
            int x  = sw - tw - 6;

            if (showBackground.getValue()) {
                RenderUtil.drawRect(ms, x - 2, y - 1, tw + 4, 10, BG);
            }
            // Акцентная полоска справа
            RenderUtil.drawRect(ms, sw - 2, y - 1, 2, 10, ACCENT);

            mc.font.drawShadow(ms, name, x, y, TEXT);
            y += 11;
        }
    }
}
