package com.example.combat.gui;

import com.example.combat.CombatClient;
import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {

    // ══ ПАЛИТРА ══════════════════════════════════════════════════════════
    private static final int BG        = rgb(10,  10,  18);   // основной фон
    private static final int PANEL_BG  = rgb(14,  14,  24);   // фон панели
    private static final int HDR_BG    = rgb(18,  16,  32);   // шапка
    private static final int HDR_BG2   = rgb(22,  18,  40);
    private static final int ACCENT    = rgb(100, 88, 255);   // основной акцент
    private static final int ACCENT2   = rgb(180, 80, 255);   // второй акцент
    private static final int MOD_ON    = rgb(22,  20,  44);
    private static final int MOD_HOV   = rgba(255,255,255, 18);
    private static final int TXT_WHITE = rgb(230, 225, 255);
    private static final int TXT_GRAY  = rgb(110, 105, 150);
    private static final int TXT_GREEN = rgb(100, 230, 120);
    private static final int TXT_RED   = rgb(230, 100, 100);
    private static final int SEP       = rgba(255,255,255, 20);
    private static final int SETTINGS_BG = rgb(8, 8, 16);
    private static final int GOLD      = rgb(255, 210, 80);
    private static final int SHADOW    = rgba(0, 0, 0, 120);

    // ══ РАЗМЕРЫ ══════════════════════════════════════════════════════════
    private static final int PW  = 140;   // ширина панели
    private static final int HH  = 22;    // высота шапки
    private static final int RH  = 16;    // строка модуля
    private static final int GAP = 8;
    private static final int SW  = 160;   // ширина окна настроек
    private static final int SR  = 22;    // строка настройки

    // Иконки категорий
    private static String catIcon(Module.Category c) {
        switch (c) {
            case COMBAT:   return "⚔";
            case RENDERER: return "◈";
            case HUD:      return "▣";
            case PLAYER:   return "☻";
            default:       return "✦";
        }
    }

    // ══ ПАНЕЛИ ═══════════════════════════════════════════════════════════
    private static class Panel {
        Module.Category cat;
        int x; float y, targetY;
        boolean collapsed = false;
        boolean dragging  = false;
        int dox, doy;
        float animH = 0;
        float scroll = 0;
        Panel(Module.Category c, int x, float y) {
            this.cat = c; this.x = x; this.y = y; this.targetY = y;
        }
    }

    private final List<Panel> panels = new ArrayList<>();

    // Настройки
    private Module  setMod  = null;
    private int     sx, sy;
    private boolean sDrag   = false;
    private int     sdox, sdoy;
    private boolean waitBind = false;
    private int     dragIdx  = -1;
    private float   sScroll  = 0;

    private FontRenderer fr() { return Minecraft.getInstance().font; }

    public ClickGUI() { super(new StringTextComponent("")); }

    @Override
    protected void init() {
        panels.clear();
        int x = GAP;
        for (Module.Category cat : Module.Category.values()) {
            if (CombatClient.moduleManager.getByCategory(cat).isEmpty()) continue;
            int mods = CombatClient.moduleManager.getByCategory(cat).size();
            Panel p = new Panel(cat, x, GAP);
            p.animH = mods * RH; // стартуем развёрнутыми без анимации при первом открытии
            panels.add(p);
            x += PW + GAP;
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    // ══ РЕНДЕР ═══════════════════════════════════════════════════════════
    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        // Затемнённый фон с виньеткой
        fillGrad(ms, 0, 0, width, height/2, rgba(0,0,14,170), rgba(0,0,14,170));
        fillGrad(ms, 0, height/2, width, height, rgba(0,0,14,170), rgba(5,0,30,180));

        for (Panel p : panels) {
            // Плавная анимация позиции и высоты
            p.y += (p.targetY - p.y) * 0.18f;
            int target = p.collapsed ? 0 : CombatClient.moduleManager.getByCategory(p.cat).size() * RH;
            p.animH += (target - p.animH) * 0.22f;
            if (Math.abs(p.animH - target) < 0.5f) p.animH = target;
            renderPanel(ms, p, mx, my);
        }

        if (setMod != null) renderSettings(ms, mx, my);
        super.render(ms, mx, my, pt);
    }

    // ── Панель ───────────────────────────────────────────────────────────
    private void renderPanel(MatrixStack ms, Panel p, int mx, int my) {
        int py  = (int) p.y;
        int vis = (int) p.animH;
        int tot = HH + vis;

        List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);
        int maxSc = Math.max(0, mods.size() * RH - vis);
        p.scroll = clamp(p.scroll, 0, maxSc);

        // ── Тень ────────────────────────────────────────────────────────
        rect(ms, p.x+4, py+4, PW, tot, SHADOW);

        // ── Фон панели ──────────────────────────────────────────────────
        rect(ms, p.x, py, PW, tot, PANEL_BG);

        // ── Шапка ───────────────────────────────────────────────────────
        fillGrad(ms, p.x, py, p.x+PW, py+HH, HDR_BG, HDR_BG2);

        // Левая цветная полоска
        fillGrad(ms, p.x, py, p.x+2, py+HH, ACCENT, ACCENT2);

        // Иконка + название
        boolean hdrHov = in(mx, my, p.x, py, PW, HH);
        String icon = catIcon(p.cat);
        String name = cap(p.cat.name());
        // В MC шрифте unicode иконки могут не рендериться, используем просто имя
        drawStr(ms, name, p.x + 10, py + 7, hdrHov ? TXT_WHITE : rgba(200,195,240,255));

        // Стрелка справа
        String arrow = p.collapsed ? "+" : "-";
        drawStr(ms, arrow, p.x + PW - 12, py + 7, TXT_GRAY);

        // Нижняя линия шапки
        rect(ms, p.x+2, py+HH, PW-2, 1, rgba(255,255,255,15));

        if (vis <= 0) {
            // Нижняя полоска акцента когда свёрнуто
            fillGrad(ms, p.x, py+HH, p.x+PW, py+HH+1, ACCENT, ACCENT2);
            return;
        }

        int clipT = py + HH;
        int clipB = clipT + vis;
        scissor(p.x, clipT, p.x + PW, clipB);

        for (int i = 0; i < mods.size(); i++) {
            Module m  = mods.get(i);
            int    ry = clipT + i * RH - (int) p.scroll;
            if (ry + RH < clipT || ry > clipB) continue;

            boolean hov = in(mx, my, p.x, ry, PW, RH);
            boolean on  = m.isEnabled();

            // Фон строки
            if (on) {
                fillGrad(ms, p.x, ry, p.x+PW, ry+RH,
                    rgba(50,40,120,60), rgba(30,20,80,40));
                // Левая полоска включённого модуля
                fillGrad(ms, p.x, ry, p.x+2, ry+RH, ACCENT, ACCENT2);
            } else if (hov) {
                rect(ms, p.x, ry, PW, RH, MOD_HOV);
            }

            // Текст модуля
            int tc = on ? TXT_WHITE : TXT_GRAY;
            drawStr(ms, m.getName(), p.x + 8, ry + 4, tc);

            // Индикатор ON/OFF — маленький квадрат справа
            int dotC = on ? ACCENT : rgba(60,60,80,200);
            int dotX = p.x + PW - 10;
            rect(ms, dotX, ry+5, 5, 5, dotC);
            if (on) rect(ms, dotX+1, ry+6, 3, 3, rgba(200,195,255,180));

            // Бинд
            if (m.getKeyBind() != GLFW.GLFW_KEY_UNKNOWN) {
                String kb = m.getKeyName();
                drawStr(ms, kb, p.x + PW - fr().width(kb) - 16, ry + 4, rgba(90,85,140,200));
            }

            // Разделитель
            if (i < mods.size()-1)
                rect(ms, p.x+6, ry+RH-1, PW-12, 1, SEP);
        }

        noScissor();

        // Нижняя акцент-линия
        fillGrad(ms, p.x, py+tot, p.x+PW, py+tot+1, ACCENT, ACCENT2);

        // Скроллбар
        if (maxSc > 0) {
            int tH  = vis;
            int thH = Math.max(12, tH * tH / (mods.size() * RH));
            int thY = clipT + (int)((tH - thH) * (p.scroll / maxSc));
            rect(ms, p.x+PW-3, clipT, 2, tH, rgba(255,255,255,15));
            fillGrad(ms, p.x+PW-3, thY, p.x+PW-1, thY+thH, ACCENT, ACCENT2);
        }
    }

    // ── Настройки ────────────────────────────────────────────────────────
    private void renderSettings(MatrixStack ms, int mx, int my) {
        List<Setting<?>> sets = getSettings(setMod);
        int contentH = sets.size() * SR + 22;
        int visH     = Math.min(contentH, height - sy - HH - 10);
        int maxSc    = Math.max(0, contentH - visH);
        sScroll      = clamp(sScroll, 0, maxSc);
        int tot      = HH + visH;

        // Тень + фон
        rect(ms, sx+4, sy+4, SW, tot, SHADOW);
        rect(ms, sx, sy, SW, tot, SETTINGS_BG);

        // Шапка
        fillGrad(ms, sx, sy, sx+SW, sy+HH, HDR_BG2, HDR_BG);
        fillGrad(ms, sx, sy, sx+2, sy+HH, ACCENT2, ACCENT);
        rect(ms, sx+2, sy+HH, SW-2, 1, rgba(255,255,255,12));

        // Название модуля в настройках
        String title = "  " + setMod.getName();
        drawStr(ms, title, sx+6, sy+7, rgba(200,180,255,255));

        // Кнопка закрыть
        boolean xHov = in(mx, my, sx+SW-14, sy+5, 10, 12);
        drawStr(ms, "x", sx+SW-11, sy+7, xHov ? TXT_RED : TXT_GRAY);

        int clipT = sy + HH;
        int clipB = clipT + visH;
        scissor(sx, clipT, sx+SW, clipB);

        for (int i = 0; i < sets.size(); i++) {
            int ry = clipT + i * SR - (int) sScroll;
            if (ry+SR < clipT || ry > clipB) continue;
            renderSettingRow(ms, sets.get(i), i, ry, mx, my);
        }

        // Строка бинда
        int by = clipT + sets.size() * SR - (int) sScroll;
        if (by >= clipT-22 && by <= clipB) {
            boolean bh = in(mx, my, sx, by, SW, 20);
            if (bh) rect(ms, sx, by, SW, 20, MOD_HOV);
            rect(ms, sx+6, by, SW-12, 1, SEP);
            String bl = waitBind ? ">> Press key..." : "Bind: " + setMod.getKeyName();
            drawStr(ms, bl, sx+8, by+6, waitBind ? GOLD : TXT_GRAY);
        }

        noScissor();
        fillGrad(ms, sx, sy+tot, sx+SW, sy+tot+1, ACCENT2, ACCENT);

        // Скроллбар настроек
        if (maxSc > 0) {
            int tH  = visH;
            int thH = Math.max(10, tH*tH/contentH);
            int thY = clipT + (int)((tH-thH)*(sScroll/maxSc));
            rect(ms, sx+SW-3, clipT, 2, tH, rgba(255,255,255,12));
            fillGrad(ms, sx+SW-3, thY, sx+SW-1, thY+thH, ACCENT2, ACCENT);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderSettingRow(MatrixStack ms, Setting<?> s, int idx, int ry, int mx, int my) {
        boolean hov = in(mx, my, sx, ry, SW, SR);
        if (hov) rect(ms, sx, ry, SW, SR, MOD_HOV);
        if (idx > 0) rect(ms, sx+6, ry, SW-12, 1, SEP);

        drawStr(ms, s.getName(), sx+8, ry+7, TXT_GRAY);

        Setting.Type t = s.getType();
        if (t == Setting.Type.TOGGLE) {
            boolean v = (Boolean) s.getValue();
            int bx = sx + SW - 26, by2 = ry + 6;
            // Toggle pill
            rect(ms, bx, by2, 20, 10, v ? rgba(80,60,200,180) : rgba(40,40,60,200));
            rect(ms, bx + (v ? 11 : 1), by2+1, 8, 8, v ? rgba(200,190,255,255) : rgba(100,100,120,200));
            String lbl = v ? "ON" : "OFF";
            drawStr(ms, lbl, bx - fr().width(lbl) - 3, ry+7, v ? TXT_GREEN : TXT_RED);

        } else if (t == Setting.Type.ENUM) {
            String val = ((Enum<?>)s.getValue()).name();
            int vx = sx + SW - fr().width(val) - 8;
            rect(ms, vx-3, ry+4, fr().width(val)+6, SR-8, rgba(60,50,100,150));
            drawStr(ms, val, vx, ry+7, GOLD);

        } else if (t == Setting.Type.SLIDER_INT || t == Setting.Type.SLIDER_FLOAT) {
            double val  = s.getValue() instanceof Float ? (Float)s.getValue() : (Integer)s.getValue();
            int bx = sx+8, bw = SW-18;
            int by2 = ry + SR - 7, bht = 4;
            int fill = (int)((val - s.getMin()) / (s.getMax() - s.getMin()) * bw);
            fill = Math.max(0, Math.min(fill, bw));

            // Track
            rect(ms, bx, by2, bw, bht, rgba(30,25,55,200));
            // Fill gradient
            fillGrad(ms, bx, by2, bx+fill, by2+bht, ACCENT, ACCENT2);
            // Thumb
            rect(ms, bx+fill-3, by2-2, 7, bht+4, rgba(220,210,255,240));

            String vs = s.getValue() instanceof Float
                ? String.format("%.2f", val) : String.valueOf((int)val);
            drawStr(ms, vs, sx+SW-fr().width(vs)-8, ry+7, TXT_WHITE);
        }
    }

    // ══ MOUSE ════════════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        // Закрыть настройки кнопкой X
        if (setMod != null && in((int)mx,(int)my, sx+SW-14, sy+5, 10, 12) && btn==0) {
            setMod=null; return true;
        }
        // Drag шапки настроек
        if (setMod!=null && in((int)mx,(int)my,sx,sy,SW-14,HH) && btn==0) {
            sDrag=true; sdox=(int)(mx-sx); sdoy=(int)(my-sy); return true;
        }
        if (setMod!=null) {
            if (handleSetClick((int)mx,(int)my,btn)) return true;
            setMod=null; waitBind=false; return true;
        }

        for (Panel p : panels) {
            int py = (int)p.y;
            if (in((int)mx,(int)my,p.x,py,PW,HH)) {
                if (btn==0) { p.dragging=true; p.dox=(int)(mx-p.x); p.doy=(int)(my-py); }
                else if (btn==1) p.collapsed = !p.collapsed;
                return true;
            }
            if (!p.collapsed && p.animH>1) {
                int ct=(int)p.y+HH, cb=ct+(int)p.animH;
                if ((int)mx<p.x||(int)mx>=p.x+PW||(int)my<ct||(int)my>=cb) continue;
                List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);
                for (int i=0;i<mods.size();i++) {
                    int ry=ct+i*RH-(int)p.scroll;
                    if (!in((int)mx,(int)my,p.x,ry,PW,RH)) continue;
                    if (btn==0) mods.get(i).toggle();
                    else if (btn==1) {
                        setMod=mods.get(i);
                        sx=Math.min((int)mx+6, width-SW-4);
                        sy=Math.max(4,(int)my-8);
                        waitBind=false; dragIdx=-1; sScroll=0;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx,my,btn);
    }

    @SuppressWarnings("unchecked")
    private boolean handleSetClick(int mx, int my, int btn) {
        List<Setting<?>> sets = getSettings(setMod);
        int ct = sy+HH;
        for (int i=0;i<sets.size();i++) {
            Setting<?> s=sets.get(i);
            int ry=ct+i*SR-(int)sScroll;
            if (!in(mx,my,sx,ry,SW,SR)) continue;
            Setting.Type t=s.getType();
            if (t==Setting.Type.TOGGLE&&btn==0) ((Setting<Boolean>)s).setValue(!(Boolean)s.getValue());
            else if (t==Setting.Type.ENUM&&btn==0) s.cycleEnum();
            else if (t==Setting.Type.SLIDER_INT||t==Setting.Type.SLIDER_FLOAT) { dragIdx=i; applySlider(s,mx); }
            return true;
        }
        int by=ct+sets.size()*SR-(int)sScroll;
        if (in(mx,my,sx,by,SW,20)&&btn==0) { waitBind=true; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (sDrag) { sx=(int)(mx-sdox); sy=(int)(my-sdoy); return true; }
        for (Panel p : panels) {
            if (p.dragging) { p.x=(int)(mx-p.dox); p.y=(float)(my-p.doy); p.targetY=p.y; return true; }
        }
        if (dragIdx>=0&&setMod!=null) {
            List<Setting<?>> s=getSettings(setMod);
            if (dragIdx<s.size()) applySlider(s.get(dragIdx),(int)mx);
            return true;
        }
        return super.mouseDragged(mx,my,btn,dx,dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        sDrag=false; dragIdx=-1;
        for (Panel p : panels) p.dragging=false;
        return super.mouseReleased(mx,my,btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (setMod!=null&&in((int)mx,(int)my,sx,sy,SW+20,height)) { sScroll-=(float)(delta*14); return true; }
        for (Panel p : panels) {
            if (in((int)mx,(int)my,p.x,(int)p.y,PW,HH+(int)p.animH)) { p.scroll-=(float)(delta*14); return true; }
        }
        return super.mouseScrolled(mx,my,delta);
    }

    // ══ KEYBOARD ═════════════════════════════════════════════════════════
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (waitBind&&setMod!=null) {
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

    // ══ HELPERS ══════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void applySlider(Setting<?> s, int mx) {
        double ratio = clamp01((mx-sx-8)/(double)(SW-18));
        double val   = s.getMin() + ratio*(s.getMax()-s.getMin());
        if (s.getValue() instanceof Float) ((Setting<Float>)s).setValue((float)val);
        else ((Setting<Integer>)s).setValue((int)Math.round(val));
    }

    private List<Setting<?>> getSettings(Module m) {
        List<Setting<?>> list = new ArrayList<>();
        for (java.lang.reflect.Field f : m.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            try { Object v=f.get(m); if (v instanceof Setting) list.add((Setting<?>)v); }
            catch (IllegalAccessException ignored) {}
        }
        return list;
    }

    private boolean in(int mx,int my,int x,int y,int w,int h) {
        return mx>=x&&mx<x+w&&my>=y&&my<y+h;
    }

    // ── Рисование ────────────────────────────────────────────────────────
    private void rect(MatrixStack ms, int x, int y, int w, int h, int c) {
        if (w<=0||h<=0) return;
        fill(ms, x, y, x+w, y+h, c);
    }

    private void drawStr(MatrixStack ms, String t, int x, int y, int c) {
        fr().drawShadow(ms, t, x, y, c);
    }

    private void fillGrad(MatrixStack ms, int x1, int y1, int x2, int y2, int top, int bot) {
        if (x2<=x1||y2<=y1) return;
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        Tessellator tess=Tessellator.getInstance();
        BufferBuilder buf=tess.getBuilder();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buf.vertex(ms.last().pose(),x1,y1,0).color(rf(top),gf(top),bf(top),af(top)).endVertex();
        buf.vertex(ms.last().pose(),x1,y2,0).color(rf(bot),gf(bot),bf(bot),af(bot)).endVertex();
        buf.vertex(ms.last().pose(),x2,y2,0).color(rf(bot),gf(bot),bf(bot),af(bot)).endVertex();
        buf.vertex(ms.last().pose(),x2,y1,0).color(rf(top),gf(top),bf(top),af(top)).endVertex();
        tess.end();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private void scissor(int x1,int y1,int x2,int y2) {
        double sc=Minecraft.getInstance().getWindow().getGuiScale();
        int wh=Minecraft.getInstance().getWindow().getGuiScaledHeight();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int)(x1*sc),(int)(wh*sc-y2*sc),(int)((x2-x1)*sc),(int)((y2-y1)*sc));
    }
    private void noScissor() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }

    // ── Цвет ─────────────────────────────────────────────────────────────
    private static int rgb(int r, int g, int b)       { return rgba(r,g,b,255); }
    private static int rgba(int r, int g, int b, int a){ return (a&0xFF)<<24|(r&0xFF)<<16|(g&0xFF)<<8|(b&0xFF); }
    private static float rf(int c){ return ((c>>16)&0xFF)/255f; }
    private static float gf(int c){ return ((c>>8) &0xFF)/255f; }
    private static float bf(int c){ return ( c     &0xFF)/255f; }
    private static float af(int c){ return ((c>>24)&0xFF)/255f; }

    private static float clamp(float v, float lo, float hi) { return v<lo?lo:v>hi?hi:v; }
    private static double clamp01(double v) { return v<0?0:v>1?1:v; }
    private static String cap(String s) { return s.isEmpty()?s:s.charAt(0)+s.substring(1).toLowerCase(); }
}
