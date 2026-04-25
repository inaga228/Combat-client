package com.example.combat.gui;

import com.example.combat.CombatClientMod;
import com.example.combat.modules.building.FastPlaceModule;
import com.example.combat.modules.building.ScaffoldModule;
import com.example.combat.modules.building.TowerModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * ClickGUI — Fabric 1.21.1
 * Список модулей разбит по категориям.
 */
public class ClickGUI extends Screen {

    // ── Палитра ─────────────────────────────────────────────────────────
    private static final int BG      = color(8,   8,  16, 220);
    private static final int HDR     = color(14,  12,  28, 255);
    private static final int ACC     = color(88,  80, 220, 255);
    private static final int ACC2    = color(160, 80, 240, 255);
    private static final int TXT     = color(220, 215,255, 255);
    private static final int GRAY    = color(100,  95,150, 255);
    private static final int ON_CLR  = color(80,  220, 140, 255);
    private static final int OFF_CLR = color(160, 60,  80, 255);
    private static final int HOV     = color(30,  25,  55, 200);

    // ── Модули ───────────────────────────────────────────────────────────
    // {Название, категория, ключ}
    private static final String[] MOD_NAMES = {
        "Scaffold", "Tower", "Fast Place"
    };
    private static final String[] MOD_CAT = {
        "Building", "Building", "Building"
    };

    // ── Геометрия ─────────────────────────────────────────────────────────
    private static final int PW = 360, PH = 260;
    private static final int HDR_H = 28;
    private static final int CAT_H = 16, MOD_H = 22, PAD = 6;

    private float openAnim = 0f;
    private int hovered = -1;

    public ClickGUI() {
        super(Text.literal(""));
    }

    @Override public boolean shouldPause() { return false; }
    @Override protected void init() { openAnim = 0f; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        openAnim = Math.min(1f, openAnim + delta * 0.14f);
        float ease = 1f - (1f - openAnim) * (1f - openAnim);
        int a = (int)(255 * ease);
        if (a <= 0) return;

        int px = (width - PW) / 2;
        int py = (int)((height - PH) / 2 + (1f - ease) * 24);

        // Фон + тень
        ctx.fill(px + 5, py + 5, px + PW + 5, py + PH + 5, color(0,0,0,(int)(80*ease)));
        ctx.fill(px, py, px + PW, py + PH, color(10, 9, 20, a));

        // Шапка
        ctx.fill(px, py, px + PW, py + HDR_H, color(16, 14, 36, a));
        ctx.fillGradient(px, py, px + PW/2, py + 2, color(88,80,220,a), color(160,80,240,a));
        ctx.fillGradient(px + PW/2, py, px + PW, py + 2, color(160,80,240,a), color(88,80,220,a));
        ctx.fill(px, py + HDR_H - 1, px + PW, py + HDR_H, color(255,255,255,(int)(18*ease)));

        String title = CombatClientMod.NAME + "  §8v" + CombatClientMod.VERSION;
        ctx.drawTextWithShadow(textRenderer, title,
            px + PW/2 - textRenderer.getWidth(title.replace("§8","")) / 2, py + 10, color(210,200,255,a));

        // Категория "Building"
        int cy = py + HDR_H + PAD;
        ctx.fillGradient(px + PAD, cy, px + PAD + 80, cy + 1, color(88,80,220,a), color(0,0,0,0));
        ctx.drawTextWithShadow(textRenderer, "§9Building", px + PAD, cy + 3, color(140,130,220,a));
        cy += CAT_H + PAD;

        // Модули
        hovered = -1;
        int[] modStates = getModStates();
        for (int i = 0; i < MOD_NAMES.length; i++) {
            boolean on  = modStates[i] == 1;
            boolean hov = mx >= px + PAD && mx < px + PW - PAD
                       && my >= cy && my < cy + MOD_H;
            if (hov) hovered = i;

            // Фон строки
            ctx.fill(px + PAD, cy, px + PW - PAD, cy + MOD_H,
                hov ? color(30,25,55,a) : color(18,16,36,a));

            // Левая полоска цвета
            ctx.fill(px + PAD, cy, px + PAD + 3, cy + MOD_H,
                on ? color(80,220,140,a) : color(160,60,80,a));

            // Название модуля
            ctx.drawTextWithShadow(textRenderer, MOD_NAMES[i],
                px + PAD + 8, cy + 7, color(200,195,240,a));

            // Статус ON/OFF справа
            String status = on ? "§aON" : "§cOFF";
            int sw2 = textRenderer.getWidth(on ? "ON" : "OFF");
            ctx.drawTextWithShadow(textRenderer, status,
                px + PW - PAD - sw2 - 6, cy + 7, 0xFFFFFFFF);

            // Разделитель
            ctx.fill(px + PAD + 3, cy + MOD_H - 1, px + PW - PAD, cy + MOD_H,
                color(255,255,255,(int)(12*ease)));

            cy += MOD_H + 2;
        }

        // Подсказка снизу
        String hint = "[ Right Shift ] — открыть/закрыть  |  Click — вкл/выкл";
        ctx.drawTextWithShadow(textRenderer, hint,
            px + PW/2 - textRenderer.getWidth(hint)/2,
            py + PH - 12, color(60,55,100,a));

        // Нижняя линия
        ctx.fillGradient(px, py + PH - 1, px + PW/2, py + PH, color(88,80,220,(int)(180*ease)), color(160,80,240,(int)(180*ease)));
        ctx.fillGradient(px + PW/2, py + PH - 1, px + PW, py + PH, color(160,80,240,(int)(180*ease)), color(88,80,220,(int)(180*ease)));

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && hovered >= 0) {
            toggleModule(hovered);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    // ── Состояния и переключение ─────────────────────────────────────────
    private static int[] getModStates() {
        return new int[]{
            ScaffoldModule.enabled  ? 1 : 0,
            TowerModule.enabled     ? 1 : 0,
            FastPlaceModule.enabled ? 1 : 0
        };
    }

    private static void toggleModule(int idx) {
        switch (idx) {
            case 0 -> ScaffoldModule.enabled  = !ScaffoldModule.enabled;
            case 1 -> TowerModule.enabled     = !TowerModule.enabled;
            case 2 -> FastPlaceModule.enabled = !FastPlaceModule.enabled;
        }
    }

    private static int color(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }
}
