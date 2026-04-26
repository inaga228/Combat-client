package com.example.combat.gui;

import com.example.combat.CombatClientMod;
import com.example.combat.modules.building.FastPlaceModule;
import com.example.combat.modules.building.ScaffoldModule;
import com.example.combat.modules.building.TowerModule;
import com.example.combat.modules.building.ClutchSaveModule;
import com.example.combat.modules.combat.AimAssistModule;
import com.example.combat.modules.combat.AutoCritModule;
import com.example.combat.modules.combat.BedwarsModule;
import com.example.combat.modules.combat.TriggerBotModule;
import com.example.combat.modules.client.OptimizationModule;
import com.example.combat.modules.client.PlayerEspModule;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

/**
 * ClickGUI — Fabric 1.16.5
 *
 * ЛКМ на модуле  → вкл/выкл
 * ПКМ на модуле  → настройки (не закрываются по клику, только Esc/RShift)
 * Клик по строке параметра → переключить / изменить
 * Drag по слайдеру → тянуть значение
 */
public class ClickGUI extends Screen {

    // ─── Цвета ───────────────────────────────────────────────────────────────
    private static final int C_BG      = c(230,  8,  8, 16);
    private static final int C_HDR     = c(255, 14, 12, 28);
    private static final int C_ACC     = c(255, 88, 80,220);
    private static final int C_TXT     = c(255,220,215,255);
    private static final int C_GRAY    = c(255,100, 95,150);
    private static final int C_ON      = c(255, 80,220,140);
    private static final int C_OFF     = c(255,160, 60, 80);
    private static final int C_HOV     = c(180, 30, 25, 55);
    private static final int C_SETT    = c(245, 20, 18, 40);
    private static final int C_ROW_OFF = c(200, 15, 12, 30);

    // ─── Геометрия ───────────────────────────────────────────────────────────
    private static final int PW    = 250;  // ширина главной панели
    private static final int HDR_H = 26;
    private static final int MOD_H = 22;
    private static final int PAD   = 5;
    private static final int SW    = 210;  // ширина панели настроек
    private static final int SH    = 220;  // макс высота панели настроек

    // ─── Модули ──────────────────────────────────────────────────────────────
    // 0=Scaffold 1=Tower 2=FastPlace 3=Optimization 4=TriggerBot 5=AimAssist 6=ClutchSave 7=Bedwars 8=PlayerESP 9=AutoCrit
    private static final String[] NAMES = {"Scaffold", "Tower", "Fast Place", "Optimization", "TriggerBot", "AimAssist", "ClutchSave", "Bedwars", "PlayerESP", "AutoCrit"};

    // ─── Состояние ───────────────────────────────────────────────────────────
    private int hovered         = -1;
    private int openSettings    = -1;
    private int bindWaiting     = -1;
    private int draggingSlider  = -1;
    private int settX           = -1;
    private int settY           = -1;
    private int settH           = SH;
    private final int[] scroll  = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private int moduleListScroll = 0;

    public ClickGUI() { super(new LiteralText("")); }

    @Override public boolean isPauseScreen() { return false; }

    // ════════════════════════════════════════════════════════════════════════
    //  РЕНДЕР
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void render(MatrixStack ms, int mx, int my, float delta) {
        int neon = getNeonAccent();
        drawAnimatedBackdrop(ms, neon);
        // Главная панель — левый верх
        int maxListArea = Math.max(120, height - 28 - HDR_H);
        int fullListH = NAMES.length * (MOD_H + PAD) + PAD;
        int listAreaH = Math.min(fullListH, maxListArea);
        int PH = HDR_H + listAreaH;
        int px = 10, py = 10;

        drawNeonPanel(ms, px, py, PW, PH, C_BG, neon);
        drawRoundedRect(ms, px+2, py+2, PW-4, HDR_H-2, c(255, 20, 16, 34));
        drawCenteredText(ms, textRenderer, "§bCombat Client §7v1.0", px+PW/2, py+8, C_TXT);
        fill(ms, px+2, py+HDR_H, px+PW-2, py+HDR_H+1, neon);

        int maxListScroll = Math.max(0, fullListH - listAreaH);
        moduleListScroll = clamp(moduleListScroll, 0, maxListScroll);

        hovered = -1;
        beginScissor(py + HDR_H + 1, px + 2, PW - 4, listAreaH - 2);
        for (int i = 0; i < NAMES.length; i++) {
            int ry = py + HDR_H + PAD + i*(MOD_H+PAD) - moduleListScroll;
            boolean on  = isEnabled(i);
            boolean hov = mx>=px+PAD && mx<px+PW-PAD && my>=ry && my<ry+MOD_H;
            boolean sel = (openSettings == i);
            if (hov) hovered = i;

            int rowColor = hov ? blend(C_HOV, neon, 0.25f) : C_ROW_OFF;
            drawRoundedRect(ms, px+PAD, ry, PW-PAD*2, MOD_H, rowColor);
            // Полоска слева: зелёная=вкл, красная=выкл
            fill(ms, px+PAD, ry, px+PAD+3, ry+MOD_H, on ? C_ON : C_OFF);
            // Жёлтая полоска справа = настройки открыты
            if (sel) fill(ms, px+PW-PAD-3, ry, px+PW-PAD, ry+MOD_H, blend(c(255,220,180,50), neon, 0.35f));

            textRenderer.drawWithShadow(ms, NAMES[i], px+PAD+7, ry+7, on ? C_ON : C_GRAY);
            String st = on ? "§aON" : "§cOFF";
            textRenderer.drawWithShadow(ms, st, px+PW-PAD-textRenderer.getWidth(on?"ON":"OFF")-2, ry+7, C_TXT);
        }
        endScissor();
        if (maxListScroll > 0) {
            drawScrollbar(ms, px + PW - 3, py + HDR_H + 2, listAreaH - 4, moduleListScroll, maxListScroll);
        }

        // ── Панель настроек ───────────────────────────────────────────────
        if (openSettings >= 0) {
            int sx = px + PW + 5;
            int sy = 10; // фиксируем настройки у верхнего края экрана
            if (sx + SW > width-5) sx = px - SW - 5;

            int dH = Math.max(80, Math.min(SH, height - sy - 8));
            settX = sx; settY = sy; settH = dH;

            drawNeonPanel(ms, sx, sy, SW, dH, C_SETT, neon);
            drawRoundedRect(ms, sx+2, sy+2, SW-4, 12, c(255, 22, 18, 40));
            textRenderer.drawWithShadow(ms, "§b"+NAMES[openSettings]+" §7Settings", sx+5, sy+3, C_TXT);
            fill(ms, sx+2, sy+14, sx+SW-2, sy+15, neon);

            int contentH = dH - 18;
            int maxSc = Math.max(0, contentHeight(openSettings) - contentH);
            scroll[openSettings] = clamp(scroll[openSettings], 0, maxSc);

            beginScissor(sy+18, sx+2, SW-4, contentH);
            renderSettings(ms, sx, sy+18 - scroll[openSettings], openSettings);
            endScissor();
            drawScrollbar(ms, sx+SW-3, sy+18, contentH, scroll[openSettings], maxSc);
        } else {
            settX = -1; settY = -1;
        }

        super.render(ms, mx, my, delta);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  СОДЕРЖИМОЕ НАСТРОЕК
    // ════════════════════════════════════════════════════════════════════════
    private void renderSettings(MatrixStack ms, int x, int y, int mod) {
        int ly = y;
        switch (mod) {
            case 0: // Scaffold
                ly = row(ms, x, ly, "§7Режим",      "§b"+ScaffoldModule.getModeName(), "[клик]");
                ly = row(ms, x, ly, "§7Задержка",   "§b"+ScaffoldModule.delay+" тк",  "[drag]");
                ly = sliderRow(ms, x, ly, ScaffoldModule.delay / 10f);
                ly = boolRow(ms, x, ly, "SafeWalk",     ScaffoldModule.safeWalk);
                ly = boolRow(ms, x, ly, "Только земля", ScaffoldModule.onlyOnGround);
                ly = boolRow(ms, x, ly, "AutoJump",     ScaffoldModule.autoJump);
                ly = boolRow(ms, x, ly, "Eagle Sneak",  ScaffoldModule.eagleSneak);
                ly = boolRow(ms, x, ly, "Clutch Save",  ScaffoldModule.clutchRescue);
                ly = row(ms, x, ly, "§7Clutch Turn",    "§b"+ScaffoldModule.clutchTurnSpeed+"°", "[drag]");
                ly = sliderRow(ms, x, ly, (ScaffoldModule.clutchTurnSpeed - 25) / 95f);
                ly = row(ms, x, ly, "§7Clutch Fall V",  "§b"+String.format("%.2f", ScaffoldModule.clutchFallSpeed), "[drag]");
                ly = sliderRow(ms, x, ly, (float)((ScaffoldModule.clutchFallSpeed - 0.05) / 0.40));
                ly = bindRow(ms, x, ly, 0);
                break;
            case 1: // Tower
                ly = row(ms, x, ly, "§7Режим",    "§b"+TowerModule.getModeName(), "[клик]");
                ly = row(ms, x, ly, "§7Скорость", "§b"+TowerModule.speed+" б/с",  "[drag]");
                ly = sliderRow(ms, x, ly, (TowerModule.speed-1) / 4f);
                ly = boolRow(ms, x, ly, "Legit движение", TowerModule.legitMovement);
                ly = bindRow(ms, x, ly, 1);
                break;
            case 2: // FastPlace
                ly = row(ms, x, ly, "§7Cooldown", "§b"+FastPlaceModule.cooldown+" тк", "[drag]");
                ly = sliderRow(ms, x, ly, FastPlaceModule.cooldown / 4f);
                ly = boolRow(ms, x, ly, "Только блоки", FastPlaceModule.onlyBlocks);
                ly = bindRow(ms, x, ly, 2);
                break;
            case 3: // Optimization
                ly = boolRow(ms, x, ly, "Выкл. частицы", OptimizationModule.disableParticles);
                ly = boolRow(ms, x, ly, "Выкл. небо",    OptimizationModule.disableSky);
                ly = boolRow(ms, x, ly, "Выкл. облака",  OptimizationModule.disableClouds);
                ly = boolRow(ms, x, ly, "Выкл. туман",   OptimizationModule.disableFog);
                ly = boolRow(ms, x, ly, "Выкл. погоду",  OptimizationModule.disableWeather);
                ly = boolRow(ms, x, ly, "Буст рендера",  OptimizationModule.renderBoost);
                ly = boolRow(ms, x, ly, "Выкл. блок-сущности", OptimizationModule.disableBlockEntities);
                ly = boolRow(ms, x, ly, "Выкл. сущности", OptimizationModule.disableEntities);
                ly = boolRow(ms, x, ly, "Ultra Low preset", OptimizationModule.ultraLowPreset);
                ly = bindRow(ms, x, ly, 3);
                break;
            case 4: // TriggerBot
                ly = row(ms, x, ly, "§7CPS", "§b"+TriggerBotModule.cps, "[drag]");
                ly = sliderRow(ms, x, ly, (TriggerBotModule.cps - 4) / 10f);
                ly = row(ms, x, ly, "§7Range", "§b"+String.format("%.1f", TriggerBotModule.range), "[drag]");
                ly = sliderRow(ms, x, ly, (float)((TriggerBotModule.range - 2.5) / 3.5));
                ly = boolRow(ms, x, ly, "Только оружие", TriggerBotModule.onlyWeapon);
                ly = boolRow(ms, x, ly, "Только игроки", TriggerBotModule.playersOnly);
                ly = bindRow(ms, x, ly, 4);
                break;
            case 5: // AimAssist
                ly = row(ms, x, ly, "§7Range", "§b"+String.format("%.1f", AimAssistModule.range), "[drag]");
                ly = sliderRow(ms, x, ly, (float)((AimAssistModule.range - 2.5) / 3.5));
                ly = row(ms, x, ly, "§7Strength", "§b"+AimAssistModule.strength+"°", "[drag]");
                ly = sliderRow(ms, x, ly, (AimAssistModule.strength - 5) / 85f);
                ly = row(ms, x, ly, "§7FOV", "§b"+AimAssistModule.fov+"°", "[drag]");
                ly = sliderRow(ms, x, ly, (AimAssistModule.fov - 30) / 150f);
                ly = boolRow(ms, x, ly, "Только при атаке", AimAssistModule.requireClick);
                ly = boolRow(ms, x, ly, "Только игроки", AimAssistModule.playersOnly);
                ly = bindRow(ms, x, ly, 5);
                break;
            case 6: // ClutchSave
                ly = row(ms, x, ly, "§7Turn", "§b"+ClutchSaveModule.turnSpeed+"°", "[drag]");
                ly = sliderRow(ms, x, ly, (ClutchSaveModule.turnSpeed - 25) / 95f);
                ly = row(ms, x, ly, "§7Fall V", "§b"+String.format("%.2f", ClutchSaveModule.fallSpeed), "[drag]");
                ly = sliderRow(ms, x, ly, (float)((ClutchSaveModule.fallSpeed - 0.05) / 0.40));
                ly = bindRow(ms, x, ly, 6);
                break;
            case 7: // Bedwars
                ly = boolRow(ms, x, ly, "Auto Sprint", BedwarsModule.autoSprint);
                ly = boolRow(ms, x, ly, "Edge Stop", BedwarsModule.edgeStop);
                ly = boolRow(ms, x, ly, "Auto Block Swap", BedwarsModule.autoBlockSwap);
                ly = boolRow(ms, x, ly, "Bridge Jump Assist", BedwarsModule.bridgeJumpAssist);
                ly = bindRow(ms, x, ly, 7);
                break;
            case 8: // PlayerESP
                ly = boolRow(ms, x, ly, "Include Invisible", PlayerEspModule.includeInvisible);
                ly = row(ms, x, ly, "§7Range", "§b"+String.format("%.0f", PlayerEspModule.maxDistance), "[drag]");
                ly = sliderRow(ms, x, ly, (float)((PlayerEspModule.maxDistance - 16.0) / 80.0));
                ly = bindRow(ms, x, ly, 8);
                break;
            case 9: // AutoCrit
                ly = boolRow(ms, x, ly, "Только игроки", AutoCritModule.playersOnly);
                ly = boolRow(ms, x, ly, "Только оружие", AutoCritModule.onlyWeapon);
                ly = boolRow(ms, x, ly, "Sync с TriggerBot", AutoCritModule.syncWithTriggerBot);
                ly = row(ms, x, ly, "§7Jump cooldown", "§b"+AutoCritModule.jumpCooldownTicks+" тк", "[drag]");
                ly = sliderRow(ms, x, ly, AutoCritModule.jumpCooldownTicks / 12f);
                ly = bindRow(ms, x, ly, 9);
                break;
        }
        if (bindWaiting == mod) {
            textRenderer.drawWithShadow(ms, "§eНажми клавишу для бинда...", x+5, ly+4, C_TXT);
        }
    }

    // ── Строки ───────────────────────────────────────────────────────────────
    private int row(MatrixStack ms, int x, int y, String label, String val, String hint) {
        textRenderer.drawWithShadow(ms, label+": "+val, x+5, y+3, C_TXT);
        textRenderer.drawWithShadow(ms, "§8"+hint, x+SW-textRenderer.getWidth(hint)-5, y+3, C_GRAY);
        return y+12;
    }

    private int boolRow(MatrixStack ms, int x, int y, String label, boolean val) {
        // Цветная точка слева
        fill(ms, x+5, y+3, x+9, y+9, val ? C_ON : C_OFF);
        textRenderer.drawWithShadow(ms, "§7"+label, x+13, y+3, C_TXT);
        textRenderer.drawWithShadow(ms, val?"§aВКЛ":"§cВЫКЛ", x+SW-30, y+3, C_TXT);
        return y+12;
    }

    private int sliderRow(MatrixStack ms, int x, int y, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int w = SW - 16;
        fill(ms, x+5, y+2, x+5+w, y+7, c(255,40,38,70));
        fill(ms, x+5, y+2, x+5+(int)(w*t), y+7, C_ACC);
        // ручка
        int kx = x+5+(int)(w*t);
        fill(ms, kx-2, y, kx+2, y+9, C_TXT);
        return y+12;
    }

    private int bindRow(MatrixStack ms, int x, int y, int modIdx) {
        textRenderer.drawWithShadow(ms, "§7Бинд: §b"+getBindName(modIdx), x+5, y+3, C_TXT);
        textRenderer.drawWithShadow(ms, "§8[ЛКМ по строке | Del=сброс]", x+5, y+13, C_GRAY);
        return y+24;
    }

    // ─── Высота содержимого (для скролла) ────────────────────────────────────
    private int contentHeight(int mod) {
        switch (mod) {
            case 0: return 12*13 + 24 + 10;
            case 1: return 12*4 + 24 + 10;
            case 2: return 12*2 + 12 + 24 + 10;
            case 3: return 12*9 + 24 + 10;
            case 4: return 12 + 12 + 12 + 12 + 12 + 12 + 24 + 10;
            case 5: return 12 + 12 + 12 + 12 + 12 + 12 + 12 + 12 + 24 + 10;
            case 6: return 12 + 12 + 12 + 12 + 24 + 10;
            case 7: return 12 + 12 + 12 + 12 + 24 + 10;
            case 8: return 12 + 12 + 12 + 24 + 10;
            case 9: return 12 + 12 + 12 + 12 + 12 + 24 + 10;
        }
        return 100;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  КЛИКИ
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Клик по настройкам
        if (openSettings >= 0 && button == 0) {
            if (handleSettingsClick((int)mx, (int)my)) return true;
        }

        // Клик по строке модуля
        int px = 10, py = 10;
        for (int i = 0; i < NAMES.length; i++) {
            int ry = py + HDR_H + PAD + i*(MOD_H+PAD) - moduleListScroll;
            if ((int)mx>=px+PAD && (int)mx<px+PW-PAD && (int)my>=ry && (int)my<ry+MOD_H) {
                if (button == 0) {
                    toggle(i);
                    CombatClientMod.saveConfig();
                } else if (button == 1) {
                    openSettings = (openSettings == i) ? -1 : i;
                    bindWaiting  = -1;
                    scroll[i]    = 0;
                }
                return true;
            }
        }
        // Не закрываем настройки при клике мимо — только Esc/RShift
        return super.mouseClicked(mx, my, button);
    }

    private boolean handleSettingsClick(int mx, int my) {
        if (settX < 0) return false;
        int mod = openSettings;
        // Контентная область с учётом скролла
        int x  = settX;
        int y  = settY + 18 - scroll[mod];

        // Определяем строки по тем же шагам что и в renderSettings
        switch (mod) {
            case 0: { // Scaffold
                int ly = y;
                // Режим (row → 12px)
                if (hitRow(mx, my, ly)) { ScaffoldModule.cycleMode(1); save(); return true; } ly+=12;
                // Задержка label (12px) + slider (12px)
                ly+=12;
                if (hitSlider(mx, my, ly)) { draggingSlider=0; updateSlider(0, mx); save(); return true; } ly+=12;
                // Булевые
                if (hitRow(mx,my,ly)) { ScaffoldModule.safeWalk      = !ScaffoldModule.safeWalk;      save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { ScaffoldModule.onlyOnGround  = !ScaffoldModule.onlyOnGround;  save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { ScaffoldModule.autoJump      = !ScaffoldModule.autoJump;      save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { ScaffoldModule.eagleSneak    = !ScaffoldModule.eagleSneak;    save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { ScaffoldModule.clutchRescue  = !ScaffoldModule.clutchRescue;  save(); return true; } ly+=12;
                ly+=12; // label clutch turn
                if (hitSlider(mx, my, ly)) { draggingSlider=8; updateSlider(8, mx); save(); return true; } ly+=12;
                ly+=12; // label clutch fall speed
                if (hitSlider(mx, my, ly)) { draggingSlider=9; updateSlider(9, mx); save(); return true; } ly+=12;
                if (hitBindRow(mx, my, ly)) { bindWaiting = mod; return true; }
                return false;
            }
            case 1: { // Tower
                int ly = y;
                if (hitRow(mx,my,ly)) { TowerModule.cycleMode(1); save(); return true; } ly+=12;
                ly+=12; // label скорости
                if (hitSlider(mx,my,ly)) { draggingSlider=1; updateSlider(1, mx); save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { TowerModule.legitMovement = !TowerModule.legitMovement; save(); return true; } ly+=12;
                if (hitBindRow(mx, my, ly)) { bindWaiting = mod; return true; }
                return false;
            }
            case 2: { // FastPlace
                int ly = y;
                ly+=12; // label cooldown
                if (hitSlider(mx,my,ly)) { draggingSlider=2; updateSlider(2, mx); save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { FastPlaceModule.onlyBlocks = !FastPlaceModule.onlyBlocks; save(); return true; } ly+=12;
                if (hitBindRow(mx, my, ly)) { bindWaiting = mod; return true; }
                return false;
            }
            case 3: { // Optimization
                int ly = y;
                if (hitRow(mx,my,ly)) { OptimizationModule.disableParticles = !OptimizationModule.disableParticles; save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { OptimizationModule.disableSky       = !OptimizationModule.disableSky;       save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { OptimizationModule.disableClouds    = !OptimizationModule.disableClouds;    save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { OptimizationModule.disableFog       = !OptimizationModule.disableFog;       save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { OptimizationModule.disableWeather   = !OptimizationModule.disableWeather;   save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { OptimizationModule.renderBoost      = !OptimizationModule.renderBoost;      save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { OptimizationModule.disableBlockEntities = !OptimizationModule.disableBlockEntities; save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { OptimizationModule.disableEntities = !OptimizationModule.disableEntities; save(); return true; } ly+=12;
                if (hitRow(mx,my,ly)) { OptimizationModule.ultraLowPreset = !OptimizationModule.ultraLowPreset; save(); return true; } ly+=12;
                if (hitBindRow(mx, my, ly)) { bindWaiting = mod; return true; }
                return false;
            }
            case 4: { // TriggerBot
                int ly = y;
                ly += 12; // label cps
                if (hitSlider(mx, my, ly)) { draggingSlider = 3; updateSlider(3, mx); save(); return true; } ly += 12;
                ly += 12; // label range
                if (hitSlider(mx, my, ly)) { draggingSlider = 4; updateSlider(4, mx); save(); return true; } ly += 12;
                if (hitRow(mx, my, ly)) { TriggerBotModule.onlyWeapon = !TriggerBotModule.onlyWeapon; save(); return true; }
                ly += 12;
                if (hitRow(mx, my, ly)) { TriggerBotModule.playersOnly = !TriggerBotModule.playersOnly; save(); return true; }
                ly += 12;
                if (hitBindRow(mx, my, ly)) { bindWaiting = mod; return true; }
                return false;
            }
            case 5: { // AimAssist
                int ly = y;
                ly += 12; // label range
                if (hitSlider(mx, my, ly)) { draggingSlider = 5; updateSlider(5, mx); save(); return true; } ly += 12;
                ly += 12; // label strength
                if (hitSlider(mx, my, ly)) { draggingSlider = 6; updateSlider(6, mx); save(); return true; } ly += 12;
                ly += 12; // label fov
                if (hitSlider(mx, my, ly)) { draggingSlider = 7; updateSlider(7, mx); save(); return true; } ly += 12;
                if (hitRow(mx, my, ly)) { AimAssistModule.requireClick = !AimAssistModule.requireClick; save(); return true; }
                ly += 12;
                if (hitRow(mx, my, ly)) { AimAssistModule.playersOnly = !AimAssistModule.playersOnly; save(); return true; }
                ly += 12;
                if (hitBindRow(mx, my, ly)) { bindWaiting = mod; return true; }
                return false;
            }
            case 6: { // ClutchSave
                int ly = y;
                ly += 12; // label turn
                if (hitSlider(mx, my, ly)) { draggingSlider = 10; updateSlider(10, mx); save(); return true; } ly += 12;
                ly += 12; // label fall
                if (hitSlider(mx, my, ly)) { draggingSlider = 11; updateSlider(11, mx); save(); return true; } ly += 12;
                if (hitBindRow(mx, my, ly)) { bindWaiting = mod; return true; }
                return false;
            }
            case 7: { // Bedwars
                int ly = y;
                if (hitRow(mx, my, ly)) { BedwarsModule.autoSprint = !BedwarsModule.autoSprint; save(); return true; } ly += 12;
                if (hitRow(mx, my, ly)) { BedwarsModule.edgeStop = !BedwarsModule.edgeStop; save(); return true; } ly += 12;
                if (hitRow(mx, my, ly)) { BedwarsModule.autoBlockSwap = !BedwarsModule.autoBlockSwap; save(); return true; } ly += 12;
                if (hitRow(mx, my, ly)) { BedwarsModule.bridgeJumpAssist = !BedwarsModule.bridgeJumpAssist; save(); return true; } ly += 12;
                if (hitBindRow(mx, my, ly)) { bindWaiting = mod; return true; }
                return false;
            }
            case 8: { // PlayerESP
                int ly = y;
                if (hitRow(mx, my, ly)) { PlayerEspModule.includeInvisible = !PlayerEspModule.includeInvisible; save(); return true; } ly += 12;
                ly += 12; // label range
                if (hitSlider(mx, my, ly)) { draggingSlider = 12; updateSlider(12, mx); save(); return true; } ly += 12;
                if (hitBindRow(mx, my, ly)) { bindWaiting = mod; return true; }
                return false;
            }
            case 9: { // AutoCrit
                int ly = y;
                if (hitRow(mx, my, ly)) { AutoCritModule.playersOnly = !AutoCritModule.playersOnly; save(); return true; } ly += 12;
                if (hitRow(mx, my, ly)) { AutoCritModule.onlyWeapon = !AutoCritModule.onlyWeapon; save(); return true; } ly += 12;
                if (hitRow(mx, my, ly)) { AutoCritModule.syncWithTriggerBot = !AutoCritModule.syncWithTriggerBot; save(); return true; } ly += 12;
                ly += 12; // label cooldown
                if (hitSlider(mx, my, ly)) { draggingSlider = 13; updateSlider(13, mx); save(); return true; } ly += 12;
                if (hitBindRow(mx, my, ly)) { bindWaiting = mod; return true; }
                return false;
            }
        }
        return false;
    }

    /** Попали ли в строку высотой 12px */
    private boolean hitRow(int mx, int my, int rowY) {
        return mx >= settX+5 && mx < settX+SW-5 && my >= rowY && my < rowY+12;
    }

    /** Попали ли в слайдер */
    private boolean hitSlider(int mx, int my, int rowY) {
        return mx >= settX+5 && mx < settX+SW-11 && my >= rowY && my < rowY+12;
    }

    private boolean hitBindRow(int mx, int my, int rowY) {
        return mx >= settX + 5 && mx < settX + SW - 5 && my >= rowY && my < rowY + 24;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingSlider >= 0) { updateSlider(draggingSlider, (int)mx); save(); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        draggingSlider = -1;
        return super.mouseReleased(mx, my, btn);
    }

    private void updateSlider(int id, int mx) {
        float t = (mx - (settX+5)) / (float)(SW-16);
        t = Math.max(0f, Math.min(1f, t));
        switch (id) {
            case 0: ScaffoldModule.delay  = Math.round(t * 10); break;
            case 1: TowerModule.speed     = clamp(Math.round(1 + t * 4), 1, 5); break;
            case 2: FastPlaceModule.cooldown = clamp(Math.round(t * 4), 0, 4); break;
            case 3: TriggerBotModule.cps = clamp(Math.round(4 + t * 10), 4, 14); break;
            case 4: TriggerBotModule.range = Math.max(2.5, Math.min(6.0, 2.5 + t * 3.5)); break;
            case 5: AimAssistModule.range = Math.max(2.5, Math.min(6.0, 2.5 + t * 3.5)); break;
            case 6: AimAssistModule.strength = clamp(Math.round(5 + t * 85), 5, 90); break;
            case 7: AimAssistModule.fov = clamp(Math.round(30 + t * 150), 30, 180); break;
            case 8: ScaffoldModule.clutchTurnSpeed = clamp(Math.round(25 + t * 95), 25, 120); break;
            case 9: ScaffoldModule.clutchFallSpeed = Math.max(0.05, Math.min(0.45, 0.05 + t * 0.40)); break;
            case 10: ClutchSaveModule.turnSpeed = clamp(Math.round(25 + t * 95), 25, 120); break;
            case 11: ClutchSaveModule.fallSpeed = Math.max(0.05, Math.min(0.45, 0.05 + t * 0.40)); break;
            case 12: PlayerEspModule.maxDistance = Math.max(16.0, Math.min(96.0, 16.0 + t * 80.0)); break;
            case 13: AutoCritModule.jumpCooldownTicks = clamp(Math.round(t * 12), 0, 12); break;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        int px = 10, py = 10;
        int maxListArea = Math.max(120, height - 28 - HDR_H);
        int fullListH = NAMES.length * (MOD_H + PAD) + PAD;
        int listAreaH = Math.min(fullListH, maxListArea);
        if ((int)mx >= px && (int)mx < px + PW && (int)my >= py + HDR_H && (int)my < py + HDR_H + listAreaH) {
            int maxListScroll = Math.max(0, fullListH - listAreaH);
            moduleListScroll = clamp(moduleListScroll - (int)(amount * 10), 0, maxListScroll);
            return true;
        }

        if (openSettings >= 0 && settX >= 0 &&
            (int)mx>=settX && (int)mx<settX+SW && (int)my>=settY && (int)my<settY+settH) {
            int maxSc = Math.max(0, contentHeight(openSettings) - (settH-18));
            scroll[openSettings] = clamp(scroll[openSettings] - (int)(amount*10), 0, maxSc);
            return true;
        }
        return super.mouseScrolled(mx, my, amount);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  КЛАВИШИ
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // Ждём бинд
        if (bindWaiting >= 0) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { bindWaiting = -1; return true; }
            CombatClientMod.setModuleBind(bindWaiting, key);
            bindWaiting = -1;
            save();
            return true;
        }
        // Delete = сбросить
        if (openSettings >= 0) {
            if (key == GLFW.GLFW_KEY_DELETE) {
                CombatClientMod.setModuleBind(openSettings, GLFW.GLFW_KEY_UNKNOWN); save(); return true;
            }
        }
        // Закрыть только по Esc или RShift
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose(); return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        openSettings = -1;
        bindWaiting  = -1;
        MinecraftClient.getInstance().openScreen(null);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ВСПОМОГАТЕЛЬНОЕ
    // ════════════════════════════════════════════════════════════════════════
    private static boolean isEnabled(int i) {
        switch (i) {
            case 0: return ScaffoldModule.enabled;
            case 1: return TowerModule.enabled;
            case 2: return FastPlaceModule.enabled;
            case 3: return OptimizationModule.enabled;
            case 4: return TriggerBotModule.enabled;
            case 5: return AimAssistModule.enabled;
            case 6: return ClutchSaveModule.enabled;
            case 7: return BedwarsModule.enabled;
            case 8: return PlayerEspModule.enabled;
            case 9: return AutoCritModule.enabled;
        }
        return false;
    }

    private static void toggle(int i) {
        switch (i) {
            case 0: ScaffoldModule.enabled    = !ScaffoldModule.enabled;    break;
            case 1: TowerModule.enabled       = !TowerModule.enabled;       break;
            case 2: FastPlaceModule.enabled   = !FastPlaceModule.enabled;   break;
            case 3: OptimizationModule.enabled= !OptimizationModule.enabled;break;
            case 4: TriggerBotModule.enabled  = !TriggerBotModule.enabled;  break;
            case 5: AimAssistModule.enabled   = !AimAssistModule.enabled;   break;
            case 6: ClutchSaveModule.enabled  = !ClutchSaveModule.enabled;  break;
            case 7: BedwarsModule.enabled     = !BedwarsModule.enabled;     break;
            case 8: PlayerEspModule.enabled   = !PlayerEspModule.enabled;   break;
            case 9: AutoCritModule.enabled    = !AutoCritModule.enabled;    break;
        }
    }

    private static void save() { CombatClientMod.saveConfig(); }

    private static String getBindName(int idx) {
        int key = CombatClientMod.getModuleBind(idx);
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "нет";
        String n = GLFW.glfwGetKeyName(key, 0);
        return n == null ? "key"+key : n.toUpperCase();
    }

    private void drawScrollbar(MatrixStack ms, int x, int y, int h, int scroll, int maxSc) {
        if (maxSc <= 0) return;
        fill(ms, x, y, x+2, y+h, c(255,40,38,70));
        int th  = Math.max(14, h*h/(h+maxSc));
        int ty  = y + (h-th)*scroll/maxSc;
        fill(ms, x, ty, x+2, ty+th, getNeonAccent());
    }

    private void drawNeonPanel(MatrixStack ms, int x, int y, int w, int h, int bodyColor, int accentColor) {
        drawRoundedRect(ms, x, y, w, h, bodyColor);
        drawRoundedRect(ms, x + 1, y + 1, w - 2, h - 2, c(120, 8, 8, 16));
        fillGradient(ms, x, y, x + w, y + 14, blend(accentColor, c(200, 30, 25, 48), 0.30f), c(0, 0, 0, 0));
        fill(ms, x + 1, y + 1, x + w - 1, y + 2, blend(accentColor, c(255, 255, 255, 255), 0.10f));
    }

    private void drawAnimatedBackdrop(MatrixStack ms, int neon) {
        fillGradient(ms, 0, 0, width, height, c(200, 6, 8, 18), c(200, 8, 10, 22));
        long t = System.currentTimeMillis();
        int wave = (int) (Math.sin(t / 450.0) * 30.0);
        fillGradient(ms, 0, height / 3 + wave, width, height / 3 + 90 + wave, c(40, 255, 255, 255), c(0, 0, 0, 0));
        fillGradient(ms, 0, (height * 2) / 3 - wave, width, (height * 2) / 3 + 80 - wave, c(30, 180, 140, 255), c(0, 0, 0, 0));
        int cx = (int) ((t / 12) % (width + 140)) - 140;
        fillGradient(ms, cx, 0, cx + 140, height, c(22, (neon >> 16) & 255, (neon >> 8) & 255, neon & 255), c(0, 0, 0, 0));
    }

    private void drawRoundedRect(MatrixStack ms, int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) {
            fill(ms, x, y, x + w, y + h, color);
            return;
        }
        fill(ms, x + 2, y, x + w - 2, y + h, color);
        fill(ms, x, y + 2, x + w, y + h - 2, color);
    }

    private int getNeonAccent() {
        long t = System.currentTimeMillis();
        float pulse = (float) ((Math.sin(t / 320.0) + 1.0) * 0.5);
        int r = 90 + (int) (90 * pulse);
        int g = 70 + (int) (160 * (1.0f - pulse));
        int b = 220 + (int) (25 * pulse);
        return c(255, r, g, b);
    }

    private int blend(int base, int add, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a1 = (base >>> 24) & 0xFF, r1 = (base >>> 16) & 0xFF, g1 = (base >>> 8) & 0xFF, b1 = base & 0xFF;
        int a2 = (add >>> 24) & 0xFF, r2 = (add >>> 16) & 0xFF, g2 = (add >>> 8) & 0xFF, b2 = add & 0xFF;
        return c(
            (int) (a1 + (a2 - a1) * t),
            (int) (r1 + (r2 - r1) * t),
            (int) (g1 + (g2 - g1) * t),
            (int) (b1 + (b2 - b1) * t)
        );
    }

    private void beginScissor(int y, int x, int w, int h) {
        Window win = MinecraftClient.getInstance().getWindow();
        double s = win.getScaleFactor();
        RenderSystem.enableScissor(
            (int)(x*s), (int)(win.getHeight()-(y+h)*s),
            (int)(w*s), (int)(h*s)
        );
    }
    private void endScissor() { RenderSystem.disableScissor(); }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo,Math.min(hi,v)); }
    private static int c(int a,int r,int g,int b)   { return (a<<24)|(r<<16)|(g<<8)|b; }
}
