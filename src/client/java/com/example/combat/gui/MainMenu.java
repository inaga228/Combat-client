package com.example.combat.gui;

import com.example.combat.CombatClientMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * MainMenu — Fabric 1.21.1
 * Кастомное главное меню с частицами и анимацией.
 */
public class MainMenu extends Screen {

    // ── Палитра ─────────────────────────────────────────────────────────
    private static final int BG1    = color(4,   4,  10, 255);
    private static final int BG2    = color(8,   4,  20, 255);
    private static final int ACC    = color(88,  80, 220, 255);
    private static final int ACC2   = color(160, 80, 240, 255);
    private static final int TXT    = color(220, 215,255, 255);
    private static final int SUBCLR = color(110, 100,160, 255);
    private static final int BTN    = color(14,  12,  28, 225);
    private static final int BHOV   = color(24,  20,  48, 240);
    private static final int BBORD  = color(70,  60, 180, 200);

    // ── Частицы ─────────────────────────────────────────────────────────
    private static final int PC = 60;
    private final float[] px  = new float[PC];
    private final float[] py  = new float[PC];
    private final float[] pvx = new float[PC];
    private final float[] pvy = new float[PC];
    private final float[] psz = new float[PC];
    private final float[] pal = new float[PC]; // alpha
    private final Random  rng = new Random();

    // ── Кнопки ──────────────────────────────────────────────────────────
    private static final int BW = 200, BH = 22, BGAP = 6;
    private int[] btnY;
    private final String[] btnLabels = {"Singleplayer", "Multiplayer", "Options", "Quit"};
    private int hovBtn = -1;

    // ── Анимация ────────────────────────────────────────────────────────
    private float anim    = 0f; // 0→1 при открытии
    private float titleX  = -400f;
    private float logoA   = 0f;
    private long  lastMs  = 0;
    private float glowT   = 0f; // пульсация

    public MainMenu() {
        super(Text.literal(""));
    }

    @Override
    protected void init() {
        // Инициализируем частицы
        for (int i = 0; i < PC; i++) spawnParticle(i, true);
        lastMs = System.currentTimeMillis();

        // Позиции кнопок
        int totalH = btnLabels.length * (BH + BGAP) - BGAP;
        int startY = height / 2 - totalH / 2 + 30;
        btnY = new int[btnLabels.length];
        for (int i = 0; i < btnLabels.length; i++) {
            btnY[i] = startY + i * (BH + BGAP);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastMs) / 1000f, 0.05f);
        lastMs = now;

        // Анимация
        anim   = Math.min(1f, anim + dt * 1.4f);
        glowT += dt * 1.2f;
        float ease = 1f - (float)Math.pow(1f - anim, 3); // ease-out cubic

        // Движение заголовка
        float targetX = width * 0.08f;
        titleX += (targetX - titleX) * dt * 6f;

        // Фон градиент
        ctx.fillGradient(0, 0, width, height, BG1, BG2);

        // Горизонтальная линия посередине (акцент)
        int ly = height / 2 - 20;
        ctx.fillGradient(0, ly, width / 3, ly + 1, color(0,0,0,0), color(88, 80, 220, 60));
        ctx.fillGradient(width / 3, ly, width * 2 / 3, ly + 1, color(88,80,220,60), color(160,80,240,60));
        ctx.fillGradient(width * 2 / 3, ly, width, ly + 1, color(160,80,240,60), color(0,0,0,0));

        // Частицы
        updateAndDrawParticles(ctx, dt);

        // Заголовок клиента (слева)
        if (ease > 0.05f) {
            int ta = (int)(255 * ease);
            String name = CombatClientMod.NAME;
            String ver  = "v" + CombatClientMod.VERSION;

            // Большой заголовок
            ctx.getMatrices().push();
            ctx.getMatrices().translate((int) titleX, height / 2 - 55, 0);
            ctx.getMatrices().scale(2.5f, 2.5f, 1f);
            ctx.drawTextWithShadow(textRenderer, name, 0, 0, color(200, 190, 255, ta));
            ctx.getMatrices().pop();

            // Версия под заголовком
            ctx.drawTextWithShadow(textRenderer, ver,
                (int) titleX + 3, height / 2 - 35, color(100, 90, 180, ta));

            // Акцентная линия под заголовком
            float glow = 0.7f + 0.3f * (float)Math.sin(glowT);
            int lw = 120;
            ctx.fillGradient((int)titleX, height / 2 - 28,
                (int)titleX + lw, height / 2 - 27,
                color(88, 80, 220, (int)(200 * glow * ease)),
                color(160, 80, 240, (int)(100 * glow * ease)));
        }

        // Кнопки
        hovBtn = -1;
        int btnX = width / 2 - BW / 2;
        for (int i = 0; i < btnLabels.length; i++) {
            float btnEase = Math.max(0f, Math.min(1f, (anim - i * 0.12f) / 0.6f));
            float be3 = 1f - (float)Math.pow(1f - btnEase, 3);
            int   by   = btnY[i];
            int   offY = (int)((1f - be3) * 20f);
            int   ba   = (int)(255 * be3);
            if (ba <= 0) continue;

            boolean hov = mx >= btnX && mx < btnX + BW && my >= by && my < by + BH;
            if (hov) hovBtn = i;

            // Тень кнопки
            ctx.fill(btnX + 3, by + offY + 3, btnX + BW + 3, by + offY + BH + 3, color(0,0,0,(int)(80*be3)));

            // Фон кнопки
            ctx.fill(btnX, by + offY, btnX + BW, by + offY + BH, hov ? alphaBlend(BHOV, ba) : alphaBlend(BTN, ba));

            // Граница
            ctx.fillGradient(btnX, by + offY, btnX + BW / 2, by + offY + 1,
                alphaBlend(ACC, ba), alphaBlend(ACC2, ba));
            ctx.fillGradient(btnX + BW / 2, by + offY, btnX + BW, by + offY + 1,
                alphaBlend(ACC2, ba), alphaBlend(ACC, ba));
            ctx.fill(btnX, by + offY, btnX + 1, by + offY + BH, alphaBlend(BBORD, (int)(ba*0.5f)));
            ctx.fill(btnX + BW - 1, by + offY, btnX + BW, by + offY + BH, alphaBlend(BBORD, (int)(ba*0.5f)));

            // Текст кнопки
            String label = btnLabels[i];
            int lx = btnX + BW / 2 - textRenderer.getWidth(label) / 2;
            int lclr = hov ? color(230, 225, 255, ba) : color(170, 165, 210, ba);
            ctx.drawTextWithShadow(textRenderer, label, lx, by + offY + 7, lclr);
        }

        // Нижний текст
        String copy = "Combat Client  •  " + CombatClientMod.NAME;
        ctx.drawTextWithShadow(textRenderer, copy,
            width / 2 - textRenderer.getWidth(copy) / 2,
            height - 12, color(50, 45, 90, (int)(180 * ease)));

        super.render(ctx, mx, my, delta);
    }

    // ── Частицы ─────────────────────────────────────────────────────────
    private void spawnParticle(int i, boolean init) {
        px[i]  = rng.nextFloat() * width;
        py[i]  = init ? rng.nextFloat() * height : height + 5;
        pvx[i] = (rng.nextFloat() - 0.5f) * 18f;
        pvy[i] = -(rng.nextFloat() * 20f + 8f);
        psz[i] = rng.nextFloat() * 2.5f + 0.5f;
        pal[i] = rng.nextFloat() * 0.5f + 0.2f;
    }

    private void updateAndDrawParticles(DrawContext ctx, float dt) {
        for (int i = 0; i < PC; i++) {
            px[i] += pvx[i] * dt;
            py[i] += pvy[i] * dt;
            if (py[i] < -5 || px[i] < -10 || px[i] > width + 10) spawnParticle(i, false);

            int sz  = Math.max(1, (int) psz[i]);
            int a   = (int)(pal[i] * 180 * Math.min(1f, anim));
            // Цвет частицы — между фиолетовым и синим
            int clr = (i % 3 == 0) ? color(88,80,220,a)
                    : (i % 3 == 1) ? color(160,80,240,a)
                    : color(60,55,140,a);
            ctx.fill((int)px[i], (int)py[i], (int)px[i]+sz, (int)py[i]+sz, clr);
        }
    }

    // ── Клики ────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0 || hovBtn < 0) return super.mouseClicked(mx, my, btn);
        switch (hovBtn) {
            case 0 -> client.setScreen(new SelectWorldScreen(this));
            case 1 -> client.setScreen(new MultiplayerScreen(this));
            case 2 -> client.setScreen(new OptionsScreen(this, client.options));
            case 3 -> client.scheduleStop();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) return true; // не закрываем по Esc
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    // ── Утилиты ──────────────────────────────────────────────────────────
    private static int color(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }
    private static int alphaBlend(int color, int newAlpha) {
        return (color & 0x00FFFFFF) | ((newAlpha & 0xFF) << 24);
    }
}
