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

/**
 * Combat Client — ClickGUI
 * Красивый панельный GUI с анимациями, скроллом, настройками и биндами.
 */
public class ClickGUI extends Screen {

    // ══════════ ПАЛИТРА ════════════════════════════════════════════════
    private static final int C_BG       = color(12, 12, 20,  238);
    private static final int C_HDR      = color(18, 16, 30,  255);
    private static final int C_ACC      = color(108, 99, 255, 255); // фиолетовый
    private static final int C_ACC2     = color(157, 78, 221, 255); // пурпурный
    private static final int C_ON       = color(30, 27, 51,  255);
    private static final int C_HOV      = color(255,255,255,  28);
    private static final int C_TXT      = color(224,220,255, 255);
    private static final int C_SUB      = color(120,116,160, 255);
    private static final int C_SEP      = color(255,255,255,  22);
    private static final int C_SBG      = color(9,  9,  18,  245);
    private static final int C_SHDR     = color(16, 14, 28,  255);
    private static final int C_SLBG    = color(26, 26, 46,  255);
    private static final int C_SLFG    = color(108, 99, 255, 255);
    private static final int C_ON_TXT   = color(180,240,180, 255);
    private static final int C_OFF_TXT  = color(240,120,120, 255);
    private static final int C_GOLD     = color(255,210, 80,  255);
    private static final int C_SHAD     = color(0,  0,  0,   100);

    // ══════════ РАЗМЕРЫ ════════════════════════════════════════════════
    private static final int PW    = 120; // ширина панели
    private static final int HH    = 20;  // высота шапки
    private static final int RH    = 15;  // строка модуля
    private static final int GAP   = 6;
    private static final int SW    = 150; // ширина настроек
    private static final int SR    = 20;  // строка настройки
    private static final int BH    = 18;  // строка бинда

    // ══════════ ДАННЫЕ ПАНЕЛЕЙ ══════════════════════════════════════════
    private static class Panel {
        Module.Category cat;
        int x;
        float y, targetY;
        boolean collapsed = false;
        boolean dragging  = false;
        int dox, doy;
        float animH  = 0;
        float scroll = 0;
        Panel(Module.Category cat, int x, float y, float ty) {
            this.cat = cat; this.x = x; this.y = y; this.targetY = ty;
        }
    }

    private final List<Panel> panels = new ArrayList<>();

    // Настройки-панель
    private Module  setMod   = null;
    private int     sx, sy;
    private boolean sDrag    = false;
    private int     sdox, sdoy;
    private boolean waitBind = false;
    private int     dragIdx  = -1;
    private float   sSroll   = 0;

    private float openT = 0;

    // FontRenderer — берём напрямую, не через this.font чтобы избежать NPE
    private FontRenderer fr() { return Minecraft.getInstance().font; }

    public ClickGUI() { super(new StringTextComponent("")); }

    @Override
    protected void init() {
        panels.clear();
        int x = GAP;
        for (Module.Category cat : Module.Category.values()) {
            if (CombatClient.moduleManager.getByCategory(cat).isEmpty()) continue;
            panels.add(new Panel(cat, x, -80, GAP));
            x += PW + GAP;
        }
        openT = 0;
    }

    @Override public boolean isPauseScreen() { return false; }

    // ══════════════════════════════════════════════════════════════════
    //  РЕНДЕР
    // ══════════════════════════════════════════════════════════════════
    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        openT = Math.min(1f, openT + 0.08f);

        // Фон
        drawGrad(ms, 0, 0, width, height, color(0,0,14,150), color(5,0,24,150));

        for (Panel p : panels) {
            p.y += (p.targetY - p.y) * 0.2f;
            int mods = CombatClient.moduleManager.getByCategory(p.cat).size();
            float tgt = p.collapsed ? 0 : mods * RH;
            p.animH += (tgt - p.animH) * 0.25f;
            if (Math.abs(p.animH - tgt) < 0.5f) p.animH = tgt;
            drawPanel(ms, p, mx, my);
        }

        if (setMod != null) drawSettings(ms, mx, my);
        super.render(ms, mx, my, pt);
    }

    // ── Панель ────────────────────────────────────────────────────────
    private void drawPanel(MatrixStack ms, Panel p, int mx, int my) {
        int py  = (int) p.y;
        int vis = (int) p.animH;
        int tot = HH + vis;
        List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);

        // Ограничить скролл
        int maxSc = Math.max(0, mods.size() * RH - vis);
        p.scroll = Math.max(0, Math.min(p.scroll, maxSc));

        // Тень
        rect(ms, p.x+3, py+3, PW, tot, C_SHAD);
        // Фон
        rect(ms, p.x,   py,   PW, tot, C_BG);
        // Шапка
        drawGrad(ms, p.x, py, p.x+PW, py+HH, C_HDR, color(22,18,36,255));
        // Акцент-полоска сверху
        rect(ms, p.x, py, PW, 2, C_ACC);
        // Акцент-линия снизу шапки
        rect(ms, p.x, py+HH-1, PW, 1, color(255,255,255,20));

        String cat = cap(p.cat.name());
        drawStr(ms, cat, p.x+8, py+6, C_ACC);
        drawStr(ms, p.collapsed ? "\u25B6" : "\u25BC", p.x+PW-12, py+6, C_SUB);

        if (vis <= 0) { rect(ms, p.x, py+tot, PW, 1, C_ACC2); return; }

        int clipT = py + HH;
        int clipB = clipT + vis;
        scissor(p.x, clipT, p.x+PW, clipB);

        for (int i = 0; i < mods.size(); i++) {
            Module m   = mods.get(i);
            int    ry  = clipT + i*RH - (int) p.scroll;
            if (ry+RH < clipT || ry > clipB) continue;

            boolean hov = in(mx, my, p.x, ry, PW, RH);
            if (m.isEnabled()) {
                drawGrad(ms, p.x, ry, p.x+PW, ry+RH, C_ON, color(25,20,45,255));
                rect(ms, p.x, ry, 2, RH, C_ACC2);
            } else if (hov) {
                rect(ms, p.x, ry, PW, RH, C_HOV);
            }

            int tc = m.isEnabled() ? C_TXT : C_SUB;
            drawStr(ms, m.getName(), p.x+8, ry+4, tc);

            // Статус: маленький кружок
            int dot = m.isEnabled() ? C_ACC : color(80,80,80,255);
            rect(ms, p.x+PW-9, ry+5, 4, 4, dot);

            if (m.getKeyBind() != GLFW.GLFW_KEY_UNKNOWN) {
                String kb = "["+m.getKeyName()+"]";
                drawStr(ms, kb, p.x+PW-fr().width(kb)-14, ry+4, color(100,100,160,180));
            }

            if (i < mods.size()-1) rect(ms, p.x+6, ry+RH-1, PW-12, 1, C_SEP);
        }

        noScissor();
        rect(ms, p.x, py+tot, PW, 1, C_ACC2);

        // Скроллбар
        if (maxSc > 0) {
            int tH  = vis;
            int thH = Math.max(10, tH*tH/(mods.size()*RH));
            int thY = clipT + (int)((tH-thH)*(p.scroll/maxSc));
            rect(ms, p.x+PW-3, clipT, 2, tH,  color(255,255,255,18));
            rect(ms, p.x+PW-3, thY,   2, thH, C_ACC);
        }
    }

    // ── Настройки ─────────────────────────────────────────────────────
    private void drawSettings(MatrixStack ms, int mx, int my) {
        List<Setting<?>> sets = getSettings(setMod);
        int cH   = sets.size()*SR + BH + 6;
        int visH = Math.min(cH, height - sy - HH - 6);
        int maxSc= Math.max(0, cH-visH);
        sSroll   = Math.max(0, Math.min(sSroll, maxSc));
        int tot  = HH + visH;

        // Тень + фон
        rect(ms, sx+4, sy+4, SW, tot, C_SHAD);
        rect(ms, sx,   sy,   SW, tot, C_SBG);
        drawGrad(ms, sx, sy, sx+SW, sy+HH, C_SHDR, color(14,12,24,255));
        rect(ms, sx, sy, SW, 2, C_ACC2);
        rect(ms, sx, sy+HH-1, SW, 1, color(255,255,255,18));

        drawStr(ms, "\u2699 "+setMod.getName(), sx+7, sy+6, C_ACC2);

        int clipT = sy+HH, clipB = clipT+visH;
        scissor(sx, clipT, sx+SW, clipB);

        for (int i = 0; i < sets.size(); i++) {
            int ry = clipT + i*SR - (int) sSroll;
            if (ry+SR >= clipT && ry <= clipB) drawSetRow(ms, sets.get(i), i, ry, mx, my);
        }

        // Бинд
        int by = clipT + sets.size()*SR - (int) sSroll;
        if (by >= clipT-BH && by <= clipB) {
            boolean bh = in(mx,my,sx,by,SW,BH);
            if (bh) rect(ms, sx, by, SW, BH, C_HOV);
            rect(ms, sx, by, SW, 1, C_SEP);
            String bl = waitBind ? "\u23F3 Press key..." : "\uD83D\uDD11 Bind: "+setMod.getKeyName();
            // emoji won't render in MC font, use plain text
            bl = waitBind ? ">> Press key..." : "Bind: "+setMod.getKeyName();
            drawStr(ms, bl, sx+8, by+5, waitBind ? C_GOLD : C_SUB);
        }

        noScissor();
        rect(ms, sx, sy+tot, SW, 1, C_ACC2);

        if (maxSc > 0) {
            int tH  = visH;
            int thH = Math.max(10, tH*tH/cH);
            int thY = clipT + (int)((tH-thH)*(sSroll/maxSc));
            rect(ms, sx+SW-3, clipT, 2, tH,  color(255,255,255,18));
            rect(ms, sx+SW-3, thY,   2, thH, C_ACC2);
        }
    }

    @SuppressWarnings("unchecked")
    private void drawSetRow(MatrixStack ms, Setting<?> s, int idx, int ry, int mx, int my) {
        boolean hov = in(mx,my,sx,ry,SW,SR);
        if (hov) rect(ms, sx, ry, SW, SR, C_HOV);
        if (idx > 0) rect(ms, sx+6, ry, SW-12, 1, C_SEP);

        drawStr(ms, s.getName(), sx+8, ry+6, C_SUB);

        Setting.Type t = s.getType();
        if (t == Setting.Type.TOGGLE) {
            boolean v = (Boolean) s.getValue();
            String  lbl = v ? "ON" : "OFF";
            int     lx  = sx + SW - fr().width(lbl) - 8;
            // Маленький бейдж
            int badgeC = v ? color(60,200,90,200) : color(200,60,60,200);
            rect(ms, lx-3, ry+4, fr().width(lbl)+6, SR-8, badgeC);
            drawStr(ms, lbl, lx, ry+6, color(255,255,255,255));

        } else if (t == Setting.Type.ENUM) {
            String lbl = ((Enum<?>)s.getValue()).name();
            drawStr(ms, lbl, sx+SW-fr().width(lbl)-8, ry+6, C_GOLD);

        } else if (t==Setting.Type.SLIDER_INT || t==Setting.Type.SLIDER_FLOAT) {
            double val  = s.getValue() instanceof Float ? (Float)s.getValue() : (Integer)s.getValue();
            int    bx   = sx+8, bw = SW-18, by2 = ry+SR-6, bht = 3;
            int    fill = (int)((val-s.getMin())/(s.getMax()-s.getMin())*bw);
            // Track
            rect(ms, bx, by2, bw, bht, C_SLBG);
            // Fill
            drawGrad(ms, bx, by2, bx+Math.max(0,fill), by2+bht, C_ACC, C_ACC2);
            // Thumb
            rect(ms, bx+fill-2, by2-2, 5, bht+4, color(220,215,255,255));
            String vs = s.getValue() instanceof Float
                ? String.format("%.2f", val) : String.valueOf((int)val);
            drawStr(ms, vs, sx+SW-fr().width(vs)-8, ry+6, C_TXT);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  MOUSE
    // ══════════════════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (setMod != null && in((int)mx,(int)my, sx, sy, SW, HH) && btn==0) {
            sDrag=true; sdox=(int)(mx-sx); sdoy=(int)(my-sy); return true;
        }
        if (setMod != null) {
            if (handleSetClick((int)mx,(int)my,btn)) return true;
            setMod=null; waitBind=false; return true;
        }
        for (Panel p : panels) {
            int py = (int) p.y;
            if (in((int)mx,(int)my,p.x,py,PW,HH)) {
                if (btn==0) { p.dragging=true; p.dox=(int)(mx-p.x); p.doy=(int)(my-py); }
                else if (btn==1) p.collapsed = !p.collapsed;
                return true;
            }
            if (!p.collapsed && p.animH>1) {
                int ct = py+HH, cb = ct+(int)p.animH;
                if ((int)mx<p.x||(int)mx>=p.x+PW||(int)my<ct||(int)my>=cb) continue;
                List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);
                for (int i=0; i<mods.size(); i++) {
                    int ry = ct + i*RH - (int)p.scroll;
                    if (!in((int)mx,(int)my,p.x,ry,PW,RH)) continue;
                    if (btn==0) mods.get(i).toggle();
                    else if (btn==1) {
                        setMod=mods.get(i);
                        sx=Math.min((int)mx+4, width-SW-4);
                        sy=Math.max(4,(int)my-10);
                        waitBind=false; dragIdx=-1; sSroll=0;
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
        for (int i=0; i<sets.size(); i++) {
            Setting<?> s = sets.get(i);
            int ry = ct + i*SR - (int)sSroll;
            if (!in(mx,my,sx,ry,SW,SR)) continue;
            Setting.Type t = s.getType();
            if (t==Setting.Type.TOGGLE && btn==0) ((Setting<Boolean>)s).setValue(!(Boolean)s.getValue());
            else if (t==Setting.Type.ENUM && btn==0) s.cycleEnum();
            else if (t==Setting.Type.SLIDER_INT||t==Setting.Type.SLIDER_FLOAT) { dragIdx=i; applySlider(s,mx); }
            return true;
        }
        int by = ct + sets.size()*SR - (int)sSroll;
        if (in(mx,my,sx,by,SW,BH) && btn==0) { waitBind=true; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (sDrag) { sx=(int)(mx-sdox); sy=(int)(my-sdoy); return true; }
        for (Panel p : panels) {
            if (p.dragging) { p.x=(int)(mx-p.dox); p.y=(int)(my-p.doy); p.targetY=p.y; return true; }
        }
        if (dragIdx>=0 && setMod!=null) {
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
        if (setMod!=null && in((int)mx,(int)my,sx,sy,SW,height)) { sSroll-=(float)(delta*12); return true; }
        for (Panel p : panels) {
            int py=(int)p.y;
            if (in((int)mx,(int)my,p.x,py,PW,HH+(int)p.animH)) { p.scroll-=(float)(delta*12); return true; }
        }
        return super.mouseScrolled(mx,my,delta);
    }

    // ══════════════════════════════════════════════════════════════════
    //  KEYBOARD
    // ══════════════════════════════════════════════════════════════════
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

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void applySlider(Setting<?> s, int mx) {
        double r = Math.max(0,Math.min(1.0,(mx-sx-8)/(double)(SW-18)));
        double v = s.getMin() + r*(s.getMax()-s.getMin());
        if (s.getValue() instanceof Float) ((Setting<Float>)s).setValue((float)v);
        else ((Setting<Integer>)s).setValue((int)Math.round(v));
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
        return mx>=x && mx<x+w && my>=y && my<y+h;
    }

    // ── Рисование ─────────────────────────────────────────────────────

    /** rect через Screen.fill */
    private void rect(MatrixStack ms, int x, int y, int w, int h, int c) {
        if (w<=0||h<=0) return;
        fill(ms, x, y, x+w, y+h, c);
    }

    /** Текст через Minecraft.font напрямую — единственный надёжный способ в 1.16.5 */
    private void drawStr(MatrixStack ms, String text, int x, int y, int color) {
        fr().drawShadow(ms, text, x, y, color);
    }

    /** Градиентный прямоугольник */
    private void drawGrad(MatrixStack ms, int x1, int y1, int x2, int y2, int top, int bot) {
        if (x2<=x1||y2<=y1) return;
        float at=a(top),rt=r(top),gt=g(top),bt2=b(top);
        float ab=a(bot), rb=r(bot), gb=g(bot), bb2=b(bot);
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buf.vertex(ms.last().pose(), x1,y1,0).color(rt,gt,bt2,at).endVertex();
        buf.vertex(ms.last().pose(), x1,y2,0).color(rb,gb,bb2,ab).endVertex();
        buf.vertex(ms.last().pose(), x2,y2,0).color(rb,gb,bb2,ab).endVertex();
        buf.vertex(ms.last().pose(), x2,y1,0).color(rt,gt,bt2,at).endVertex();
        tess.end();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    /** Scissor с учётом GUI scale */
    private void scissor(int x1, int y1, int x2, int y2) {
        double sc = Minecraft.getInstance().getWindow().getGuiScale();
        int wh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            (int)(x1*sc),
            (int)(wh*sc - y2*sc),
            (int)((x2-x1)*sc),
            (int)((y2-y1)*sc)
        );
    }

    private void noScissor() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }

    // ── Цвет ──────────────────────────────────────────────────────────
    private static int color(int r, int g, int b, int a) {
        return (a&0xFF)<<24 | (r&0xFF)<<16 | (g&0xFF)<<8 | (b&0xFF);
    }
    private static float r(int c){ return ((c>>16)&0xFF)/255f; }
    private static float g(int c){ return ((c>>8)&0xFF)/255f; }
    private static float b(int c){ return (c&0xFF)/255f; }
    private static float a(int c){ return ((c>>24)&0xFF)/255f; }

    private static String cap(String s) {
        return s.isEmpty() ? s : s.charAt(0)+s.substring(1).toLowerCase();
    }
}
