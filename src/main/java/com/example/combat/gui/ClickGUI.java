package com.example.combat.gui;

import com.example.combat.CombatClient;
import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClickGUI — анимированный, со скроллом.
 *
 *  • Открытие: каждая панель плавно «падает» сверху
 *  • Категории: анимированный expand/collapse (высота интерполируется)
 *  • Скролл колёсиком внутри панели + в окне настроек
 *  • Настройки модуля — скроллируемая панель справа
 *  • Слайдеры — drag мышью
 */
public class ClickGUI extends Screen {

    // ─── Палитра ──────────────────────────────────────────────────────
    private static final int BG        = 0xEE0D0D14;
    private static final int HDR       = 0xFF13131F;
    private static final int ACC       = 0xFF6C63FF; // основной акцент — фиолет
    private static final int ACC2      = 0xFF9D4EDD; // вторичный акцент
    private static final int ON_BG     = 0xFF1E1B33;
    private static final int HOVER_BG  = 0x20FFFFFF;
    private static final int TXT       = 0xFFE0E0FF;
    private static final int SUB       = 0xFF777799;
    private static final int SEP       = 0x18FFFFFF;
    private static final int SBG       = 0xF0090912; // настройки фон
    private static final int SHDR      = 0xFF111120;
    private static final int SLBG      = 0xFF1A1A2E;
    private static final int SLFG      = 0xFF6C63FF;

    // ─── Размеры ──────────────────────────────────────────────────────
    private static final int PW      = 115; // ширина панели
    private static final int HDRH    = 18;  // шапка категории
    private static final int ROWH    = 14;  // строка модуля
    private static final int GAP     = 6;   // отступ между панелями
    private static final int SW      = 145; // ширина окна настроек
    private static final int SROW    = 18;  // строка настройки
    private static final int SBIND_H = 16;  // строка бинда

    // ─── Панель ───────────────────────────────────────────────────────
    private static class Panel {
        Module.Category cat;
        int x; float y, targetY;      // targetY для анимации появления
        boolean collapsed = false;
        boolean dragging  = false;
        int dox, doy;
        float animH;                   // анимированная высота контента
        float scroll = 0;              // текущий скролл (пиксели)

        Panel(Module.Category cat, int x, float startY, float targetY) {
            this.cat = cat; this.x = x; this.y = startY; this.targetY = targetY;
            this.animH = 0;
        }

        int contentH(int modCount) { return modCount * ROWH; }
    }

    private final List<Panel>           panels      = new ArrayList<>();
    private final Map<Panel, Float>     expandAnim  = new HashMap<>(); // 0..1

    // Настройки
    private Module  setMod      = null;
    private int     sx, sy;
    private boolean sDrag       = false;
    private int     sdox, sdoy;
    private boolean waitBind    = false;
    private int     dragSetting = -1;
    private float   setScroll   = 0;

    // Анимация открытия
    private float openProgress = 0; // 0..1

    public ClickGUI() {
        super(new StringTextComponent(""));
    }

    @Override
    protected void init() {
        panels.clear();
        int x = GAP;
        for (Module.Category cat : Module.Category.values()) {
            if (CombatClient.moduleManager.getByCategory(cat).isEmpty()) continue;
            // Начинаем выше экрана, анимируем вниз
            panels.add(new Panel(cat, x, -60, GAP));
            expandAnim.put(panels.get(panels.size()-1), 0f);
            x += PW + GAP;
        }
        openProgress = 0;
    }

    @Override public boolean isPauseScreen() { return false; }

    // ═══════════════════════════════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════════════════════════════
    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        // ── Анимация открытия ──────────────────────────────────────────
        openProgress = Math.min(1f, openProgress + 0.07f);
        float ease   = easeOut(openProgress);

        // Полупрозрачный фон
        fillGrad(ms, 0, 0, width, height, 0x99000010, 0x99080022);

        for (Panel p : panels) {
            // Интерполяция позиции Y при открытии
            p.y += (p.targetY - p.y) * 0.18f;

            // Анимация expand/collapse
            int modCount = CombatClient.moduleManager.getByCategory(p.cat).size();
            float fullH  = p.contentH(modCount);
            float target = p.collapsed ? 0f : fullH;
            p.animH += (target - p.animH) * 0.22f;
            if (Math.abs(p.animH - target) < 0.5f) p.animH = target;

            renderPanel(ms, p, mx, my);
        }

        if (setMod != null) renderSettings(ms, mx, my);
    }

    // ─── Панель ───────────────────────────────────────────────────────
    private void renderPanel(MatrixStack ms, Panel p, int mx, int my) {
        int py = (int) p.y;
        int modCount = CombatClient.moduleManager.getByCategory(p.cat).size();
        int visibleH = (int) p.animH;

        // Ограничиваем скролл
        int maxScroll = Math.max(0, (int) p.contentH(modCount) - Math.min(visibleH, height - py - HDRH - GAP * 2));
        p.scroll = Math.max(0, Math.min(p.scroll, maxScroll));

        int totalH = HDRH + visibleH;

        // Тень
        fillA(ms, p.x + 3, py + 3, PW, totalH, 0x66000000);
        // Основной фон
        fillA(ms, p.x, py, PW, totalH, BG);
        // Шапка
        fillA(ms, p.x, py, PW, HDRH, HDR);
        // Акцент-полоска слева
        fillA(ms, p.x, py, 2, HDRH, ACC);

        String catLabel = capitalize(p.cat.name());
        font.draw(ms, catLabel, p.x + 7, py + 5, ACC);
        font.draw(ms, p.collapsed ? "+" : "-", p.x + PW - 11, py + 5, SUB);

        // Нижняя линия шапки
        fillA(ms, p.x, py + HDRH - 1, PW, 1, 0x30FFFFFF);

        // ── Контент с клиппингом ──────────────────────────────────────
        if (visibleH <= 0) { fillA(ms, p.x, py + totalH, PW, 1, ACC); return; }

        List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);
        int clipTop    = py + HDRH;
        int clipBottom = clipTop + visibleH;

        enableScissor(p.x, clipTop, p.x + PW, clipBottom);

        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int ry = clipTop + i * ROWH - (int) p.scroll;
            if (ry + ROWH < clipTop || ry > clipBottom) continue;

            boolean hover = mx >= p.x && mx < p.x + PW && my >= ry && my < ry + ROWH;
            if (m.isEnabled())  fillA(ms, p.x, ry, PW, ROWH, ON_BG);
            else if (hover)     fillA(ms, p.x, ry, PW, ROWH, HOVER_BG);
            if (m.isEnabled())  fillA(ms, p.x, ry, 2, ROWH, ACC2);

            int tc = m.isEnabled() ? TXT : SUB;
            font.draw(ms, m.getName(), p.x + 7, ry + 3, tc);

            if (m.getKeyBind() != GLFW.GLFW_KEY_UNKNOWN) {
                String kb = m.getKeyName();
                font.draw(ms, kb, p.x + PW - font.width(kb) - 4, ry + 3, 0x44AAAACC);
            }

            if (i < mods.size() - 1) fillA(ms, p.x + 4, ry + ROWH - 1, PW - 8, 1, SEP);
        }

        disableScissor();

        // Нижняя граница-акцент
        fillA(ms, p.x, py + totalH, PW, 1, ACC);

        // Полоска скролла если нужно
        if (maxScroll > 0) {
            float ratio  = p.scroll / maxScroll;
            int   trackH = visibleH;
            int   thumbH = Math.max(12, trackH * trackH / (int) p.contentH(modCount));
            int   thumbY = clipTop + (int)((trackH - thumbH) * ratio);
            fillA(ms, p.x + PW - 3, clipTop, 2, trackH, 0x22FFFFFF);
            fillA(ms, p.x + PW - 3, thumbY,  2, thumbH, ACC);
        }
    }

    // ─── Окно настроек ─────────────────────────────────────────────────
    private void renderSettings(MatrixStack ms, int mx, int my) {
        List<Setting<?>> sets = collectSettings(setMod);
        int contentH = sets.size() * SROW + SBIND_H + 4;
        int visH     = Math.min(contentH, height - sy - HDRH - 8);
        int maxScroll= Math.max(0, contentH - visH);
        setScroll = Math.max(0, Math.min(setScroll, maxScroll));
        int totalH = HDRH + visH;

        // Тень
        fillA(ms, sx + 3, sy + 3, SW, totalH, 0x77000000);
        fillA(ms, sx,     sy,     SW, totalH, SBG);
        fillA(ms, sx,     sy,     SW, HDRH,   SHDR);
        fillA(ms, sx,     sy,     2,  HDRH,   ACC2);

        font.draw(ms, setMod.getName(), sx + 6, sy + 5, ACC);

        int clipT = sy + HDRH, clipB = clipT + visH;
        enableScissor(sx, clipT, sx + SW, clipB);

        for (int i = 0; i < sets.size(); i++) {
            int ry = clipT + i * SROW - (int) setScroll;
            if (ry + SROW >= clipT && ry <= clipB)
                renderSetRow(ms, sets.get(i), i, ry, mx, my);
        }

        // Бинд строка
        int by = clipT + sets.size() * SROW - (int) setScroll;
        if (by >= clipT && by <= clipB) {
            boolean bh = in(mx, my, sx, by, SW, SBIND_H);
            if (bh) fillA(ms, sx, by, SW, SBIND_H, HOVER_BG);
            String bl = waitBind ? "Press key..." : "Bind: " + setMod.getKeyName();
            font.draw(ms, bl, sx + 6, by + 4, waitBind ? 0xFFFFAA00 : SUB);
        }

        disableScissor();

        fillA(ms, sx, sy + totalH, SW, 1, ACC2);

        // Скролл
        if (maxScroll > 0) {
            float ratio = setScroll / maxScroll;
            int tH      = visH;
            int thH     = Math.max(10, tH * tH / contentH);
            int thY     = clipT + (int)((tH - thH) * ratio);
            fillA(ms, sx + SW - 3, clipT, 2, tH,  0x22FFFFFF);
            fillA(ms, sx + SW - 3, thY,   2, thH, ACC2);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderSetRow(MatrixStack ms, Setting<?> s, int idx, int ry, int mx, int my) {
        boolean hov = in(mx, my, sx, ry, SW, SROW);
        if (hov) fillA(ms, sx, ry, SW, SROW, HOVER_BG);

        font.draw(ms, s.getName(), sx + 6, ry + 5, SUB);

        Setting.Type t = s.getType();
        if (t == Setting.Type.TOGGLE) {
            boolean v = (Boolean) s.getValue();
            font.draw(ms, v ? "ON" : "OFF", sx + SW - font.width(v?"ON":"OFF") - 6, ry + 5, v ? 0xFF55FF55 : 0xFFFF5555);

        } else if (t == Setting.Type.ENUM) {
            String lbl = ((Enum<?>) s.getValue()).name();
            font.draw(ms, lbl, sx + SW - font.width(lbl) - 6, ry + 5, 0xFFFFD700);

        } else if (t == Setting.Type.SLIDER_INT || t == Setting.Type.SLIDER_FLOAT) {
            double val    = s.getValue() instanceof Float ? (Float) s.getValue() : (Integer) s.getValue();
            int barX      = sx + 6, barW = SW - 14, barY = ry + 12, barH = 3;
            int filled    = (int)((val - s.getMin()) / (s.getMax() - s.getMin()) * barW);

            fillA(ms, barX, barY, barW, barH, SLBG);
            fillA(ms, barX, barY, Math.max(0, filled), barH, SLFG);
            // Ручка
            fillA(ms, barX + filled - 2, barY - 2, 5, barH + 4, 0xFFCCCCFF);

            String vs = s.getValue() instanceof Float
                    ? String.format("%.2f", val) : String.valueOf((int) val);
            font.draw(ms, vs, sx + SW - font.width(vs) - 6, ry + 5, TXT);
        }

        if (idx > 0) fillA(ms, sx + 4, ry, SW - 8, 1, SEP);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MOUSE EVENTS
    // ═══════════════════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Тащим окно настроек за шапку
        if (setMod != null && in((int)mx,(int)my, sx, sy, SW, HDRH) && btn == 0) {
            sDrag = true; sdox=(int)(mx-sx); sdoy=(int)(my-sy); return true;
        }
        // Клик по настройкам
        if (setMod != null) {
            if (handleSetClick((int)mx,(int)my,btn)) return true;
            setMod = null; waitBind = false; return true;
        }

        for (Panel p : panels) {
            int py = (int) p.y;
            // Шапка
            if (in((int)mx,(int)my, p.x, py, PW, HDRH)) {
                if (btn==0) { p.dragging=true; p.dox=(int)(mx-p.x); p.doy=(int)(my-py); }
                else if (btn==1) p.collapsed = !p.collapsed;
                return true;
            }
            // Строки модулей
            if (!p.collapsed && p.animH > 1) {
                int clipT = py + HDRH;
                int clipB = clipT + (int) p.animH;
                if ((int)mx < p.x || (int)mx >= p.x + PW || (int)my < clipT || (int)my >= clipB) continue;
                List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);
                for (int i = 0; i < mods.size(); i++) {
                    int ry = clipT + i * ROWH - (int) p.scroll;
                    if (!in((int)mx,(int)my, p.x, ry, PW, ROWH)) continue;
                    if (btn==0) mods.get(i).toggle();
                    else if (btn==1) {
                        setMod = mods.get(i);
                        sx = Math.min((int)mx + 4, width - SW - 4);
                        sy = Math.max(4, (int)my - 10);
                        waitBind = false; dragSetting = -1; setScroll = 0;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @SuppressWarnings("unchecked")
    private boolean handleSetClick(int mx, int my, int btn) {
        List<Setting<?>> sets = collectSettings(setMod);
        int clipT = sy + HDRH;

        for (int i = 0; i < sets.size(); i++) {
            Setting<?> s = sets.get(i);
            int ry = clipT + i * SROW - (int) setScroll;
            if (!in(mx,my,sx,ry,SW,SROW)) continue;
            Setting.Type t = s.getType();
            if (t == Setting.Type.TOGGLE && btn==0) {
                Setting<Boolean> sb = (Setting<Boolean>) s; sb.setValue(!sb.getValue());
            } else if (t == Setting.Type.ENUM && btn==0) {
                s.cycleEnum();
            } else if (t==Setting.Type.SLIDER_INT || t==Setting.Type.SLIDER_FLOAT) {
                dragSetting = i; applySlider(s, mx);
            }
            return true;
        }
        // Бинд
        int by = clipT + sets.size() * SROW - (int) setScroll;
        if (in(mx,my,sx,by,SW,SBIND_H) && btn==0) { waitBind=true; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (sDrag) { sx=(int)(mx-sdox); sy=(int)(my-sdoy); return true; }
        for (Panel p : panels) {
            if (p.dragging) {
                p.x = (int)(mx - p.dox);
                p.y = (int)(my - p.doy);
                p.targetY = p.y;
                return true;
            }
        }
        if (dragSetting >= 0 && setMod != null) {
            List<Setting<?>> s = collectSettings(setMod);
            if (dragSetting < s.size()) applySlider(s.get(dragSetting), (int)mx);
            return true;
        }
        return super.mouseDragged(mx,my,btn,dx,dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        sDrag = false; dragSetting = -1;
        for (Panel p : panels) p.dragging = false;
        return super.mouseReleased(mx,my,btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        // Скролл окна настроек
        if (setMod != null && in((int)mx,(int)my, sx, sy, SW, height)) {
            setScroll -= (float)(delta * 12);
            return true;
        }
        // Скролл панели
        for (Panel p : panels) {
            int py = (int) p.y;
            if (in((int)mx,(int)my, p.x, py, PW, HDRH + (int)p.animH)) {
                p.scroll -= (float)(delta * 12);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, delta);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  KEY EVENTS
    // ═══════════════════════════════════════════════════════════════════
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (waitBind && setMod != null) {
            if (key==GLFW.GLFW_KEY_ESCAPE || key==GLFW.GLFW_KEY_DELETE)
                setMod.setKeyBind(GLFW.GLFW_KEY_UNKNOWN);
            else setMod.setKeyBind(key);
            waitBind = false; return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (setMod != null) { setMod = null; return true; }
            onClose(); return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  УТИЛИТЫ
    // ═══════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void applySlider(Setting<?> s, int mx) {
        double ratio = Math.max(0, Math.min(1.0, (mx - sx - 6) / (double)(SW - 14)));
        double val   = s.getMin() + ratio * (s.getMax() - s.getMin());
        if (s.getValue() instanceof Float) ((Setting<Float>)s).setValue((float)val);
        else ((Setting<Integer>)s).setValue((int)Math.round(val));
    }

    private List<Setting<?>> collectSettings(Module m) {
        List<Setting<?>> list = new ArrayList<>();
        for (java.lang.reflect.Field f : m.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            try { Object v = f.get(m); if (v instanceof Setting) list.add((Setting<?>)v); }
            catch (IllegalAccessException ignored) {}
        }
        return list;
    }

    private boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x+w && my >= y && my < y+h;
    }

    private void fillA(MatrixStack ms, int x, int y, int w, int h, int c) {
        if (w <= 0 || h <= 0) return;
        fill(ms, x, y, x+w, y+h, c);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    /** Градиент сверху-вниз через GL-тесселятор (правильные имена методов 1.16.5). */
    private void fillGrad(MatrixStack ms, int x1, int y1, int x2, int y2, int top, int bot) {
        float at=((top>>24)&0xFF)/255f, rt=((top>>16)&0xFF)/255f,
              gt=((top>>8)&0xFF)/255f,  bt=(top&0xFF)/255f;
        float ab=((bot>>24)&0xFF)/255f, rb=((bot>>16)&0xFF)/255f,
              gb=((bot>>8)&0xFF)/255f,  bb=(bot&0xFF)/255f;

        RenderSystem.enableBlend(); RenderSystem.disableTexture(); RenderSystem.defaultBlendFunc();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buf.vertex(x1,y1,0).color(rt,gt,bt,at).endVertex();
        buf.vertex(x1,y2,0).color(rb,gb,bb,ab).endVertex();
        buf.vertex(x2,y2,0).color(rb,gb,bb,ab).endVertex();
        buf.vertex(x2,y1,0).color(rt,gt,bt,at).endVertex();
        tess.end();
        RenderSystem.enableTexture(); RenderSystem.disableBlend();
    }

    /** OpenGL scissor test — обрезаем рендер за границей панели. */
    private void enableScissor(int x1, int y1, int x2, int y2) {
        double scale = minecraft.getWindow().getGuiScale();
        int winH = minecraft.getWindow().getGuiScaledHeight();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            (int)(x1 * scale),
            (int)(winH - y2 * scale),
            (int)((x2 - x1) * scale),
            (int)((y2 - y1) * scale)
        );
    }

    private void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
