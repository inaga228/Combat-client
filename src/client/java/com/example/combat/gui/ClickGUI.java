package com.example.combat.gui;

import com.example.combat.CombatClientMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * ClickGUI — Fabric 1.21.1
 * Пока пустая — только каркас с красивым фоном.
 * Модули будут добавлены позже.
 */
public class ClickGUI extends Screen {

    // ── Палитра ─────────────────────────────────────────────────────────
    private static final int BG       = color(8,   8,  16, 220);
    private static final int ACCENT   = color(88,  80, 220, 255);
    private static final int ACCENT2  = color(160, 80, 240, 255);
    private static final int HDR      = color(14,  12,  28, 255);
    private static final int TXT      = color(220, 215,255, 255);
    private static final int GRAY     = color(100,  95,150, 255);

    // Анимация открытия
    private float openAnim = 0f;

    public ClickGUI() {
        super(Text.literal(""));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        openAnim = 0f;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        openAnim = Math.min(1f, openAnim + delta * 0.12f);
        float ease = 1f - (1f - openAnim) * (1f - openAnim); // ease-out

        int sw = width, sh = height;

        // Затемнённый фон
        ctx.fillGradient(0, 0, sw, sh / 2,
            color(0, 0, 14, (int)(180 * ease)),
            color(0, 0, 14, (int)(180 * ease)));
        ctx.fillGradient(0, sh / 2, sw, sh,
            color(0, 0, 14, (int)(180 * ease)),
            color(5,  0, 28, (int)(195 * ease)));

        // Центральная панель
        int pw = 340, ph = 220;
        int px = (sw - pw) / 2;
        int py = (int)((sh - ph) / 2 + (1f - ease) * 30);

        // Тень
        ctx.fill(px + 5, py + 5, px + pw + 5, py + ph + 5, color(0, 0, 0, (int)(100 * ease)));

        // Фон панели
        ctx.fill(px, py, px + pw, py + ph, color(10, 9, 20, (int)(245 * ease)));

        // Шапка с градиентом
        ctx.fillGradient(px, py, px + pw, py + 28, color(16, 14, 36, (int)(255 * ease)), color(20, 16, 44, (int)(255 * ease)));

        // Акцентная линия сверху
        ctx.fillGradient(px, py, px + pw / 2, py + 2, color(88, 80, 220, (int)(255 * ease)), color(160, 80, 240, (int)(255 * ease)));
        ctx.fillGradient(px + pw / 2, py, px + pw, py + 2, color(160, 80, 240, (int)(255 * ease)), color(88, 80, 220, (int)(255 * ease)));

        // Разделитель шапки
        ctx.fill(px, py + 28, px + pw, py + 29, color(255, 255, 255, (int)(18 * ease)));

        // Заголовок
        String title = CombatClientMod.NAME;
        int tx = px + pw / 2 - textRenderer.getWidth(title) / 2;
        ctx.drawTextWithShadow(textRenderer, title, tx, py + 10, color(210, 200, 255, (int)(255 * ease)));

        // Подзаголовок "Coming soon"
        String sub = "Modules coming soon...";
        int sx2 = px + pw / 2 - textRenderer.getWidth(sub) / 2;
        ctx.drawTextWithShadow(textRenderer, sub, sx2, py + ph / 2 - 4, color(80, 75, 120, (int)(200 * ease)));

        // Подсказка снизу
        String hint = "[ Right Shift ] to open  •  [ Esc ] to close";
        int hx = px + pw / 2 - textRenderer.getWidth(hint) / 2;
        ctx.drawTextWithShadow(textRenderer, hint, hx, py + ph - 14, color(60, 55, 100, (int)(180 * ease)));

        // Акцентная линия снизу
        ctx.fillGradient(px, py + ph - 1, px + pw / 2, py + ph, color(88, 80, 220, (int)(200 * ease)), color(160, 80, 240, (int)(200 * ease)));
        ctx.fillGradient(px + pw / 2, py + ph - 1, px + pw, py + ph, color(160, 80, 240, (int)(200 * ease)), color(88, 80, 220, (int)(200 * ease)));

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private static int color(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }
}
