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
import java.util.List;

/**
 * ClickGUI — тёмный дизайн с плавными градиентами и аккуратными элементами.
 * LMB: toggle | RMB: настройки | Drag header: перемещение
 */
public class ClickGUI extends Screen {

    // ── Цветовая палитра ─────────────────────────────────────────
    private static final int BG        = 0xF0121218;
    private static final int HDR_TOP   = 0xFF1E1E2E;
    private static final int HDR_BOT   = 0xFF161624;
    private static final int ACCENT    = 0xFF6C63FF;   // фиолетовый
    private static final int ACCENT2   = 0xFF9B59B6;   // более тёплый фиолет
    private static final int MOD_ON    = 0xFF1E1E38;
    private static final int MOD_GLOW  = 0x336C63FF;
    private static final int HOVER     = 0x1AFFFFFF;
    private static final int TEXT_ON   = 0xFFE8E8FF;
    private static final int TEXT_OFF  = 0xFF7A7A9A;
    private static final int DIVIDER   = 0x18FFFFFF;
    private static final int SL_TRACK  = 0xFF252535;
    private static final int SL_FILL   = 0xFF6C63FF;
    private static final int SL_THUMB  = 0xFFBBBBDD;
    private static final int SET_BG    = 0xF2131320;
    private static final int SET_HDR_T = 0xFF201832;
    private static final int SET_HDR_B = 0xFF180C28;

    // ── Размеры ──────────────────────────────────────────────────
    private static final int PW   = 116;
    private static final int HDRH = 18;
    private static final int ROWH = 14;
    private static final int SW   = 148;
    private static final int SROW = 18;
    private static final int SBND = 15;

    // ── Анимации модулей ─────────────────────────────────────────
    // anim[panelIdx][modIdx] — от 0.0 до 1.0
    private float[][] modAnim;

    private static class Panel {
        Module.Category cat;
        int x, y, dox, doy;
        boolean collapsed = false, dragging = false;
        Panel(Module.Category c, int x, int y) { cat=c; this.x=x; this.y=y; }
    }

    private final List<Panel> panels = new ArrayList<>();
    private Module  setMod   = null;
    private int     sx, sy, sdox, sdoy;
    private boolean sDrag    = false;
    private boolean waitBind = false;
    private int     dragSlider = -1;

    public ClickGUI() {
        super(new StringTextComponent(""));
        int x = 7;
        for (Module.Category cat : Module.Category.values()) {
            if (!CombatClient.moduleManager.getByCategory(cat).isEmpty()) {
                panels.add(new Panel(cat, x, 7));
                x += PW + 6;
            }
        }
        modAnim = new float[panels.size()][];
        for (int i = 0; i < panels.size(); i++) {
            int cnt = CombatClient.moduleManager.getByCategory(panels.get(i).cat).size();
            modAnim[i] = new float[cnt];
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    // ════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════
    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        // Тёмный фон с градиентом
        fillGradV(ms, 0, 0, width, height, 0xCC000010, 0xCC08001A);

        // Обновляем анимации
        for (int pi = 0; pi < panels.size(); pi++) {
            Panel p = panels.get(pi);
            List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);
            for (int mi = 0; mi < mods.size(); mi++) {
                float target = mods.get(mi).isEnabled() ? 1f : 0f;
                modAnim[pi][mi] += (target - modAnim[pi][mi]) * 0.18f;
            }
        }

        for (int i = 0; i < panels.size(); i++) renderPanel(ms, panels.get(i), i, mx, my);
        if (setMod != null) renderSettings(ms, mx, my);
    }

    private void renderPanel(MatrixStack ms, Panel p, int pi, int mx, int my) {
        List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);
        int bodyH  = p.collapsed ? 0 : mods.size() * ROWH;
        int totalH = HDRH + bodyH;

        // Тень панели
        fillRect(ms, p.x+3, p.y+3, PW, totalH, 0x55000000);

        // Фон
        fillRect(ms, p.x, p.y + HDRH, PW, bodyH, BG);

        // Заголовок с градиентом
        fillGradV(ms, p.x, p.y, p.x + PW, p.y + HDRH, HDR_TOP, HDR_BOT);

        // Акцентная полоска слева (градиент)
        fillGradV(ms, p.x, p.y, p.x + 2, p.y + HDRH, ACCENT, ACCENT2);

        // Нижняя линия заголовка
        fillRect(ms, p.x, p.y + HDRH - 1, PW, 1, 0x44FFFFFF);

        String catName = p.cat.name().charAt(0) + p.cat.name().substring(1).toLowerCase();
        font.draw(ms, catName, p.x + 8, p.y + 5, ACCENT);

        // Стрелка свернуть/развернуть
        font.draw(ms, p.collapsed ? "+" : "-", p.x + PW - 10, p.y + 5, TEXT_OFF);

        if (!p.collapsed) {
            for (int i = 0; i < mods.size(); i++) {
                Module m  = mods.get(i);
                int    ry = p.y + HDRH + i * ROWH;
                boolean hover = inBox(mx, my, p.x, ry, PW, ROWH);
                float  anim  = modAnim[pi][i];

                // Фон строки
                if (anim > 0.01f) {
                    // Плавное свечение включённого модуля
                    int glowAlpha = (int)(anim * 0x1A);
                    fillRect(ms, p.x, ry, PW, ROWH, (glowAlpha << 24) | (MOD_GLOW & 0x00FFFFFF));
                    fillRect(ms, p.x, ry, PW, ROWH, lerpColor(0, MOD_ON, anim));
                }
                if (hover && anim < 0.5f) fillRect(ms, p.x, ry, PW, ROWH, HOVER);

                // Цветная полоска слева у включённых
                if (anim > 0.01f) {
                    int barColor = lerpColor(0x00000000, ACCENT, anim);
                    fillRect(ms, p.x, ry, 2, ROWH, barColor | 0xFF000000);
                }

                // Текст
                int textColor = lerpColor(TEXT_OFF, TEXT_ON, anim);
                font.draw(ms, m.getName(), p.x + 8, ry + 3, textColor);

                // Кейбинд
                if (m.getKeyBind() != GLFW.GLFW_KEY_UNKNOWN) {
                    String kb = "[" + m.getKeyName() + "]";
                    font.draw(ms, kb, p.x + PW - font.width(kb) - 4, ry + 3, 0x33AAAACC);
                }

                // Разделитель
                if (i < mods.size() - 1)
                    fillRect(ms, p.x + 2, ry + ROWH - 1, PW - 4, 1, DIVIDER);
            }
        }

        // Нижняя акцентная линия
        fillGradH(ms, p.x, p.y + totalH, p.x + PW, p.y + totalH + 1, ACCENT, ACCENT2);
    }

    private void renderSettings(MatrixStack ms, int mx, int my) {
        List<Setting<?>> settings = collectSettings(setMod);
        int sh = 16 + settings.size() * SROW + SBND + 4;

        // Тень
        fillRect(ms, sx+3, sy+3, SW, sh, 0x66000000);

        // Фон
        fillRect(ms, sx, sy + 16, SW, sh - 16, SET_BG);

        // Заголовок с градиентом
        fillGradV(ms, sx, sy, sx + SW, sy + 16, SET_HDR_T, SET_HDR_B);
        fillGradV(ms, sx, sy, sx + 2, sy + 16, ACCENT2, ACCENT);

        font.draw(ms, setMod.getName(), sx + 7, sy + 4, ACCENT2);
        fillRect(ms, sx, sy + 15, SW, 1, 0x33FFFFFF);

        for (int i = 0; i < settings.size(); i++)
            renderSettingRow(ms, settings.get(i), sy + 16 + i * SROW, mx, my);

        // Кейбинд строка
        int by = sy + 16 + settings.size() * SROW;
        boolean bhover = inBox(mx, my, sx, by, SW, SBND);
        if (bhover) fillRect(ms, sx, by, SW, SBND, HOVER);
        fillRect(ms, sx, by, SW, 1, DIVIDER);
        String bl = waitBind ? "▶ Press key..." : "⌨ Bind: " + setMod.getKeyName();
        font.draw(ms, bl, sx + 7, by + 3, waitBind ? 0xFFFFCC00 : TEXT_OFF);

        // Нижняя линия
        fillGradH(ms, sx, sy + sh, sx + SW, sy + sh + 1, ACCENT2, ACCENT);
    }

    @SuppressWarnings("unchecked")
    private void renderSettingRow(MatrixStack ms, Setting<?> s, int ry, int mx, int my) {
        boolean hover = inBox(mx, my, sx, ry, SW, SROW);
        if (hover) fillRect(ms, sx, ry, SW, SROW, HOVER);
        fillRect(ms, sx, ry + SROW - 1, SW, 1, DIVIDER);

        font.draw(ms, s.getName(), sx + 7, ry + 4, TEXT_OFF);

        Setting.Type type = s.getType();
        if (type == Setting.Type.TOGGLE) {
            boolean v = (Boolean) s.getValue();
            // Красивый тоггл
            int toggleX = sx + SW - 28;
            int toggleY = ry + 4;
            int tw = 22, th = 8;
            fillRect(ms, toggleX, toggleY, tw, th, v ? 0xFF2A2A50 : 0xFF222230);
            // Ползунок
            int knobX = v ? toggleX + tw - th : toggleX;
            fillRect(ms, knobX, toggleY, th, th, v ? ACCENT : 0xFF555568);
            // Текст
            font.draw(ms, v ? "ON" : "OFF", sx + SW - font.width(v ? "ON" : "OFF") - 34, ry + 4,
                    v ? 0xFF88DDAA : 0xFFDD8888);

        } else if (type == Setting.Type.ENUM) {
            String lbl = "< " + ((Enum<?>) s.getValue()).name() + " >";
            font.draw(ms, lbl, sx + SW - font.width(lbl) - 6, ry + 4, 0xFFFFD966);

        } else if (type == Setting.Type.SLIDER_INT || type == Setting.Type.SLIDER_FLOAT) {
            double val = s.getValue() instanceof Float ? (Float) s.getValue() : (Integer) s.getValue();
            int bx  = sx + 7, bw = SW - 14, by2 = ry + 12, bh = 3;
            double ratio = (val - s.getMin()) / (s.getMax() - s.getMin());
            int filled = (int)(ratio * bw);

            // Трек
            fillRect(ms, bx, by2, bw, bh, SL_TRACK);
            // Заполненная часть с градиентом
            fillGradH(ms, bx, by2, bx + filled, by2 + bh, ACCENT, ACCENT2);
            // Ползунок
            fillRect(ms, bx + filled - 2, by2 - 1, 5, bh + 2, SL_THUMB);

            // Значение
            String vs = s.getValue() instanceof Float
                    ? String.format("%.2f", val) : String.valueOf((int) val);
            font.draw(ms, vs, sx + SW - font.width(vs) - 6, ry + 4, TEXT_ON);
        }
    }

    // ════════════════════════════════════════════════════════════
    //  MOUSE
    // ════════════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (setMod != null && inBox((int)mx,(int)my, sx, sy, SW, 16) && btn==0) {
            sDrag=true; sdox=(int)(mx-sx); sdoy=(int)(my-sy); return true;
        }
        if (setMod != null) {
            if (handleSettingsClick((int)mx,(int)my,btn)) return true;
            setMod=null; waitBind=false; return true;
        }
        for (Panel p : panels) {
            if (inBox((int)mx,(int)my, p.x, p.y, PW, HDRH)) {
                if (btn==0) { p.dragging=true; p.dox=(int)(mx-p.x); p.doy=(int)(my-p.y); }
                else if (btn==1) p.collapsed = !p.collapsed;
                return true;
            }
            if (!p.collapsed) {
                List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);
                for (int i = 0; i < mods.size(); i++) {
                    int ry = p.y + HDRH + i * ROWH;
                    if (inBox((int)mx,(int)my, p.x, ry, PW, ROWH)) {
                        if (btn==0) mods.get(i).toggle();
                        else if (btn==1) {
                            setMod=mods.get(i);
                            sx=(int)mx+4; sy=(int)my-8; waitBind=false; dragSlider=-1;
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @SuppressWarnings("unchecked")
    private boolean handleSettingsClick(int mx, int my, int btn) {
        List<Setting<?>> settings = collectSettings(setMod);
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            int ry = sy + 16 + i * SROW;
            if (!inBox(mx,my,sx,ry,SW,SROW)) continue;
            Setting.Type t = s.getType();
            if (t == Setting.Type.TOGGLE && btn==0)
                { Setting<Boolean> sb=(Setting<Boolean>)s; sb.setValue(!sb.getValue()); }
            else if (t == Setting.Type.ENUM && btn==0) s.cycleEnum();
            else if (t == Setting.Type.SLIDER_INT || t == Setting.Type.SLIDER_FLOAT)
                { dragSlider=i; applySlider(s,mx); }
            return true;
        }
        int by = sy + 16 + settings.size() * SROW;
        if (inBox(mx,my,sx,by,SW,SBND) && btn==0) { waitBind=true; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (sDrag) { sx=(int)(mx-sdox); sy=(int)(my-sdoy); return true; }
        for (Panel p : panels) if (p.dragging) { p.x=(int)(mx-p.dox); p.y=(int)(my-p.doy); return true; }
        if (dragSlider>=0 && setMod!=null) {
            List<Setting<?>> s=collectSettings(setMod);
            if (dragSlider<s.size()) applySlider(s.get(dragSlider),(int)mx);
            return true;
        }
        return super.mouseDragged(mx,my,btn,dx,dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        sDrag=false; dragSlider=-1;
        for (Panel p : panels) p.dragging=false;
        return super.mouseReleased(mx,my,btn);
    }

    @SuppressWarnings("unchecked")
    private void applySlider(Setting<?> s, int mx) {
        double ratio = Math.max(0, Math.min(1.0, (mx - sx - 7) / (double)(SW - 14)));
        double val   = s.getMin() + ratio * (s.getMax() - s.getMin());
        if (s.getValue() instanceof Float) ((Setting<Float>)s).setValue((float)val);
        else ((Setting<Integer>)s).setValue((int)Math.round(val));
    }

    // ════════════════════════════════════════════════════════════
    //  KEYS
    // ════════════════════════════════════════════════════════════
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (waitBind && setMod!=null) {
            setMod.setKeyBind(key==GLFW.GLFW_KEY_ESCAPE||key==GLFW.GLFW_KEY_DELETE
                    ? GLFW.GLFW_KEY_UNKNOWN : key);
            waitBind=false; return true;
        }
        if (key==GLFW.GLFW_KEY_ESCAPE) {
            if (setMod!=null) { setMod=null; return true; }
            onClose(); return true;
        }
        return super.keyPressed(key,scan,mods);
    }

    // ════════════════════════════════════════════════════════════
    //  УТИЛИТЫ
    // ════════════════════════════════════════════════════════════
    private List<Setting<?>> collectSettings(Module m) {
        List<Setting<?>> list = new ArrayList<>();
        for (java.lang.reflect.Field f : m.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            try { Object v=f.get(m); if(v instanceof Setting) list.add((Setting<?>)v); }
            catch (IllegalAccessException ignored) {}
        }
        return list;
    }

    private boolean inBox(int mx, int my, int x, int y, int w, int h) {
        return mx>=x && mx<x+w && my>=y && my<y+h;
    }

    private void fillRect(MatrixStack ms, int x, int y, int w, int h, int color) {
        if (w<=0 || h<=0) return;
        fill(ms, x, y, x+w, y+h, color);
    }

    /** Линейная интерполяция двух ARGB цветов */
    private int lerpColor(int c1, int c2, float t) {
        int a1=(c1>>24)&0xFF, r1=(c1>>16)&0xFF, g1=(c1>>8)&0xFF, b1=c1&0xFF;
        int a2=(c2>>24)&0xFF, r2=(c2>>16)&0xFF, g2=(c2>>8)&0xFF, b2=c2&0xFF;
        int a=(int)(a1+(a2-a1)*t), r=(int)(r1+(r2-r1)*t),
            g=(int)(g1+(g2-g1)*t), b=(int)(b1+(b2-b1)*t);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }

    private void fillGradV(MatrixStack ms, int x1, int y1, int x2, int y2, int top, int bot) {
        drawGradient(ms, x1, y1, x2, y2, top, bot, true);
    }

    private void fillGradH(MatrixStack ms, int x1, int y1, int x2, int y2, int left, int right) {
        drawGradient(ms, x1, y1, x2, y2, left, right, false);
    }

    private void drawGradient(MatrixStack ms, int x1, int y1, int x2, int y2, int c1, int c2, boolean vertical) {
        float a1=((c1>>24)&0xFF)/255f, r1=((c1>>16)&0xFF)/255f, g1=((c1>>8)&0xFF)/255f, b1=(c1&0xFF)/255f;
        float a2=((c2>>24)&0xFF)/255f, r2=((c2>>16)&0xFF)/255f, g2=((c2>>8)&0xFF)/255f, b2=(c2&0xFF)/255f;
        RenderSystem.enableBlend(); RenderSystem.disableTexture(); RenderSystem.defaultBlendFunc();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        if (vertical) {
            buf.vertex(x1,y1,0).color(r1,g1,b1,a1).endVertex();
            buf.vertex(x1,y2,0).color(r2,g2,b2,a2).endVertex();
            buf.vertex(x2,y2,0).color(r2,g2,b2,a2).endVertex();
            buf.vertex(x2,y1,0).color(r1,g1,b1,a1).endVertex();
        } else {
            buf.vertex(x1,y1,0).color(r1,g1,b1,a1).endVertex();
            buf.vertex(x1,y2,0).color(r1,g1,b1,a1).endVertex();
            buf.vertex(x2,y2,0).color(r2,g2,b2,a2).endVertex();
            buf.vertex(x2,y1,0).color(r1,g1,b1,a1).endVertex();
        }
        tess.end();
        RenderSystem.enableTexture(); RenderSystem.disableBlend();
    }
}
