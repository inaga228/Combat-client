package com.example.combat.gui;

import com.example.combat.modules.building.FastPlaceModule;
import com.example.combat.modules.building.ScaffoldModule;
import com.example.combat.modules.building.TowerModule;
import com.example.combat.modules.client.OptimizationModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

/**
 * ClickGUI для Fabric 1.16.5.
 * ЛКМ — включить/выключить модуль.
 * ПКМ — открыть панель настроек.
 */
public class ClickGUI extends Screen {

    // ─── Цвета ───────────────────────────────────────────────────────────
    private static final int BG       = color(8,   8,  16, 230);
    private static final int HDR      = color(14,  12,  28, 255);
    private static final int ACC      = color(88,  80, 220, 255);
    private static final int TXT      = color(220, 215,255, 255);
    private static final int GRAY     = color(100,  95,150, 255);
    private static final int ON_CLR   = color(80,  220, 140, 255);
    private static final int OFF_CLR  = color(160,  60,  80, 255);
    private static final int HOV      = color(30,   25,  55, 180);
    private static final int SETTINGS = color(20,   18,  40, 245);

    // ─── Геометрия главного окна ──────────────────────────────────────────
    private static final int PW    = 260;
    private static final int HDR_H = 26;
    private static final int MOD_H = 22;
    private static final int PAD   = 5;

    // ─── Список модулей ───────────────────────────────────────────────────
    // index 0 = Scaffold, 1 = Tower, 2 = FastPlace, 3 = Optimization
    private static final String[] NAMES = {"Scaffold", "Tower", "Fast Place", "Optimization"};

    // ─── Состояние ───────────────────────────────────────────────────────
    private int hovered  = -1;
    private int settings = -1;   // какой модуль открыт в настройках (-1 = никакой)
    private int optimizationToggleIndex = 0;

    // Панель настроек
    private static final int SW  = 200;
    private static final int SH  = 110;

    public ClickGUI() {
        super(new LiteralText(""));
    }

    @Override public boolean shouldPause() { return false; }

    // ─── Рендер ──────────────────────────────────────────────────────────
    @Override
    public void render(MatrixStack ms, int mx, int my, float delta) {
        int PH = HDR_H + NAMES.length * (MOD_H + PAD) + PAD;
        int px = 10;
        int py = 40;

        // Фон главного окна
        fill(ms, px, py, px + PW, py + PH, BG);
        // Заголовок
        fill(ms, px, py, px + PW, py + HDR_H, HDR);
        drawCenteredText(ms, textRenderer, "§bCombat Client §7v1.0", px + PW / 2, py + 8, TXT);
        // Линия под заголовком
        fill(ms, px, py + HDR_H, px + PW, py + HDR_H + 1, ACC);

        hovered = -1;
        for (int i = 0; i < NAMES.length; i++) {
            int ry = py + HDR_H + PAD + i * (MOD_H + PAD);
            boolean on  = isEnabled(i);
            boolean hov = mx >= px + PAD && mx <= px + PW - PAD && my >= ry && my <= ry + MOD_H;
            if (hov) hovered = i;

            // Фон кнопки
            fill(ms, px + PAD, ry, px + PW - PAD, ry + MOD_H, hov ? HOV : color(15, 12, 30, 200));
            // Статус-полоска слева
            fill(ms, px + PAD, ry, px + PAD + 3, ry + MOD_H, on ? ON_CLR : OFF_CLR);

            // Название
            drawTextWithShadow(ms, textRenderer, NAMES[i], px + PAD + 7, ry + 7, on ? ON_CLR : GRAY);
            // Статус
            String status = on ? "ON" : "OFF";
            drawTextWithShadow(ms, textRenderer, status, px + PW - PAD - textRenderer.getWidth(status), ry + 7, on ? ON_CLR : OFF_CLR);
        }

        // ─── Панель настроек ──────────────────────────────────────────────
        if (settings >= 0) {
            int sx = px + PW + 5;
            int sy = py + HDR_H + PAD + settings * (MOD_H + PAD);

            // Не вылезать за экран справа
            if (sx + SW > width) sx = px - SW - 5;

            fill(ms, sx, sy, sx + SW, sy + SH, SETTINGS);
            fill(ms, sx, sy, sx + SW, sy + 14, HDR);
            drawTextWithShadow(ms, textRenderer, "§b" + NAMES[settings] + " §7Settings", sx + 5, sy + 3, TXT);
            fill(ms, sx, sy + 14, sx + SW, sy + 15, ACC);

            renderSettingsContent(ms, sx, sy + 18, settings, mx, my);
        }

        super.render(ms, mx, my, delta);
    }

    // ─── Контент настроек ────────────────────────────────────────────────
    private void renderSettingsContent(MatrixStack ms, int x, int y, int mod, int mx, int my) {
        int ly = y;
        switch (mod) {
            case 0: // Scaffold
                drawTextWithShadow(ms, textRenderer, "§7Placing blocks as you walk.", x + 5, ly, GRAY);
                ly += 14;
                drawTextWithShadow(ms, textRenderer, "§fDelay: §b" + ScaffoldModule.delay + " ticks", x + 5, ly, TXT);
                drawSliderHint(ms, x, ly, SW);
                ly += 16;
                drawTextWithShadow(ms, textRenderer, "§fSafe Walk: " + (ScaffoldModule.safeWalk ? "§aON" : "§cOFF"), x + 5, ly, TXT);
                ly += 12;
                drawTextWithShadow(ms, textRenderer, "§fLegit Movement: " + (ScaffoldModule.legitMovement ? "§aON" : "§cOFF"), x + 5, ly, TXT);
                break;
            case 1: // Tower
                drawTextWithShadow(ms, textRenderer, "§7Builds tower upward.", x + 5, ly, GRAY);
                ly += 14;
                drawTextWithShadow(ms, textRenderer, "§fSpeed: §b" + TowerModule.speed + " b/s", x + 5, ly, TXT);
                ly += 12;
                drawTextWithShadow(ms, textRenderer, "§fLegit Movement: " + (TowerModule.legitMovement ? "§aON" : "§cOFF"), x + 5, ly, TXT);
                break;
            case 2: // FastPlace
                drawTextWithShadow(ms, textRenderer, "§7Removes placement delay.", x + 5, ly, GRAY);
                ly += 14;
                drawTextWithShadow(ms, textRenderer, "§fCooldown: §b" + FastPlaceModule.cooldown + " ticks", x + 5, ly, TXT);
                drawSliderHint(ms, x, ly, SW);
                ly += 16;
                drawTextWithShadow(ms, textRenderer, "§fOnly Blocks: " + (FastPlaceModule.onlyBlocks ? "§aON" : "§cOFF"), x + 5, ly, TXT);
                break;
            case 3: // Optimization
                drawTextWithShadow(ms, textRenderer, "§7OptiFine-like visual optimization.", x + 5, ly, GRAY);
                ly += 14;
                drawTextWithShadow(ms, textRenderer, "§fDisable Particles: " + (OptimizationModule.disableParticles ? "§aON" : "§cOFF"), x + 5, ly, TXT);
                ly += 12;
                drawTextWithShadow(ms, textRenderer, "§fDisable Sky/Stars: " + (OptimizationModule.disableSky ? "§aON" : "§cOFF"), x + 5, ly, TXT);
                ly += 12;
                drawTextWithShadow(ms, textRenderer, "§fDisable Clouds: " + (OptimizationModule.disableClouds ? "§aON" : "§cOFF"), x + 5, ly, TXT);
                ly += 12;
                drawTextWithShadow(ms, textRenderer, "§fDisable Fog: " + (OptimizationModule.disableFog ? "§aON" : "§cOFF"), x + 5, ly, TXT);
                ly += 12;
                drawTextWithShadow(ms, textRenderer, "§fRender Boost: " + (OptimizationModule.renderBoost ? "§aON" : "§cOFF"), x + 5, ly, TXT);
                ly += 12;
                drawTextWithShadow(ms, textRenderer, "§8[Scroll toggles next option]", x + 5, ly, GRAY);
                break;
        }
    }

    private void drawSliderHint(MatrixStack ms, int x, int y, int w) {
        drawTextWithShadow(ms, textRenderer, "§8[Scroll to adjust]", x + w - 90, y, GRAY);
    }

    // ─── Клики ──────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (hovered >= 0) {
            if (button == 0) {
                // ЛКМ — toggle
                toggle(hovered);
                settings = -1;
                return true;
            } else if (button == 1) {
                // ПКМ — настройки
                settings = (settings == hovered) ? -1 : hovered;
                return true;
            }
        }
        // Клик вне панелей — закрываем настройки
        if (button == 0) settings = -1;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (settings >= 0) {
            adjustSetting(settings, (int) amount);
            return true;
        }
        return super.mouseScrolled(mx, my, amount);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        settings = -1;
        MinecraftClient.getInstance().openScreen(null);
    }

    // ─── Хелперы ─────────────────────────────────────────────────────────
    private static boolean isEnabled(int i) {
        switch (i) {
            case 0: return ScaffoldModule.enabled;
            case 1: return TowerModule.enabled;
            case 2: return FastPlaceModule.enabled;
            case 3: return OptimizationModule.enabled;
        }
        return false;
    }

    private static void toggle(int i) {
        switch (i) {
            case 0: ScaffoldModule.enabled = !ScaffoldModule.enabled; break;
            case 1: TowerModule.enabled    = !TowerModule.enabled;    break;
            case 2: FastPlaceModule.enabled = !FastPlaceModule.enabled; break;
            case 3: OptimizationModule.enabled = !OptimizationModule.enabled; break;
        }
    }

    private void adjustSetting(int mod, int delta) {
        switch (mod) {
            case 0: // Scaffold delay 0..10
                ScaffoldModule.delay = clamp(ScaffoldModule.delay + delta, 0, 10);
                break;
            case 1: // Tower speed 1..5
                TowerModule.speed = clamp(TowerModule.speed + delta, 1, 5);
                break;
            case 2: // FastPlace cooldown 0..4
                FastPlaceModule.cooldown = clamp(FastPlaceModule.cooldown + delta, 0, 4);
                break;
            case 3:
                toggleOptimizationOption(delta);
                break;
        }
    }

    private void toggleOptimizationOption(int delta) {
        if (delta == 0) return;
        optimizationToggleIndex = (optimizationToggleIndex + 1) % 5;
        switch (optimizationToggleIndex) {
            case 0:
                OptimizationModule.disableParticles = !OptimizationModule.disableParticles;
                break;
            case 1:
                OptimizationModule.disableSky = !OptimizationModule.disableSky;
                break;
            case 2:
                OptimizationModule.disableClouds = !OptimizationModule.disableClouds;
                break;
            case 3:
                OptimizationModule.disableFog = !OptimizationModule.disableFog;
                break;
            case 4:
                OptimizationModule.renderBoost = !OptimizationModule.renderBoost;
                break;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int color(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
