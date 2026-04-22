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
 * Combat Client — ClickGUI v3
 *
 * Layout: одна полоска вверху с таб-кнопками по категориям.
 * Ниже — одна узкая панель с модулями активной категории.
 * Справа — панель настроек при ПКМ.
 *
 * Всё помещается на экран при любом разрешении.
 */
public class ClickGUI extends Screen {

    // ── Цвета ─────────────────────────────────────────────────────────
    private static final int BG      = rgb(8,   8,  14, 220);
    private static final int LINE    = rgb(30,  28,  50, 255);
    private static final int ACT     = rgb(88,  80, 220, 255); // активная вкладка
    private static final int ACT2    = rgb(140, 80, 230, 255);
    private static final int ROW_ON  = rgb(22,  20,  42, 255);
    private static final int ROW_H   = rgb(255,255,255,  18);
    private static final int TXT_ON  = rgb(210,205,255, 255);
    private static final int TXT_OFF = rgb(100, 95, 140, 255);
    private static final int SEP     = rgb(255,255,255,  14);
    private static final int DOT_ON  = rgb(110,100,255, 255);
    private static final int DOT_OFF = rgb(50,  48,  80, 255);
    private static final int SBG     = rgb(6,   6,  12, 248);
    private static final int SHDR    = rgb(14,  12,  26, 255);
    private static final int SUB     = rgb(110, 105,155, 255);
    private static final int GOLD    = rgb(255, 200,  70, 255);
    private static final int ON_C    = rgb(70,  220, 100, 255);
    private static final int OFF_C   = rgb(220,  70,  70, 255);
    private static final int SLBG    = rgb(20,  18,  38, 255);

    // ── Размеры ───────────────────────────────────────────────────────
    private static final int TAB_H  = 18; // высота таб-бара
    private static final int TAB_PD = 10; // горизонтальный паддинг таба
    private static final int PW     = 110; // ширина панели модулей
    private static final int RH     = 13;  // строка модуля
    private static final int SW     = 140; // ширина настроек
    private static final int SR     = 19;  // строка настройки
    private static final int BH     = 16;  // строка бинда

    // ── Состояние ─────────────────────────────────────────────────────
    private Module.Category activeTab = Module.Category.COMBAT;
    private float  panelScroll = 0;

    // Панель настроек
    private Module  setMod   = null;
    private int     sx, sy;
    private boolean sDrag    = false;
    private int     sdox, sdoy;
    private boolean waitBind = false;
    private int     dragIdx  = -1;
    private float   sSroll   = 0;

    // Позиция всей панели (перетаскиваемая)
    private int panelX = -1, panelY = -1;
    private boolean pDrag = false;
    private int pdox, pdoy;

    // Анимация
    private float openT = 0;

    private FontRenderer fr() { return Minecraft.getInstance().font; }

    public ClickGUI() { super(new StringTextComponent("")); }

    @Override
    protected void init() {
        // Центрируем панель при первом открытии
        if (panelX < 0) {
            panelX = (width - PW) / 2 - 100;
            panelY = 30;
        }
        openT = 0;
    }

    @Override public boolean isPauseScreen() { return false; }

    // ══════════════════════════════════════════════════════════════════
    //  RENDER
    // ══════════════════════════════════════════════════════════════════
    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        openT = Math.min(1f, openT + 0.1f);

        // Полупрозрачный фон
        grad(ms, 0,0,width,height, rgb(0,0,10,160), rgb(4,0,18,160));

        drawPanel(ms, mx, my);
        if (setMod != null) drawSettings(ms, mx, my);

        super.render(ms, mx, my, pt);
    }

    // ── Основная панель ───────────────────────────────────────────────
    private void drawPanel(MatrixStack ms, int mx, int my) {
        List<Module> mods = CombatClient.moduleManager.getByCategory(activeTab);

        // Высота контента
        int contentH = mods.size() * RH;
        int maxPanH  = height - panelY - TAB_H - 16;
        int visH     = Math.min(contentH, maxPanH);
        int maxSc    = Math.max(0, contentH - visH);
        panelScroll  = Math.max(0, Math.min(panelScroll, maxSc));

        // ── Таб-бар ───────────────────────────────────────────────────
        // Считаем суммарную ширину всех табов
        Module.Category[] cats = Module.Category.values();
        int totalTabW = 0;
        int[] tabW = new int[cats.length];
        for (int i = 0; i < cats.length; i++) {
            tabW[i] = fr().width(cap(cats[i].name())) + TAB_PD * 2;
            totalTabW += tabW[i];
        }

        int tabBarW = totalTabW + 2;
        int tx = panelX;
        int ty = panelY;

        // Фон таб-бара
        rect(ms, tx, ty, tabBarW, TAB_H, LINE);
        rect(ms, tx, ty, tabBarW, 1, rgb(255,255,255,12));

        int cx = tx + 1;
        for (int i = 0; i < cats.length; i++) {
            Module.Category cat = cats[i];
            boolean active = cat == activeTab;
            int tw = tabW[i];
            if (active) {
                grad(ms, cx, ty, cx+tw, ty+TAB_H, ACT, rgb(70,55,180,255));
                rect(ms, cx, ty, tw, 2, ACT2);
            } else {
                boolean hov = in(mx,my, cx,ty, tw, TAB_H);
                if (hov) rect(ms, cx, ty, tw, TAB_H, ROW_H);
            }
            int tc = active ? TXT_ON : TXT_OFF;
            drawStr(ms, cap(cats[i].name()), cx + TAB_PD, ty + 5, tc);
            cx += tw;
        }

        // ── Панель модулей ────────────────────────────────────────────
        int px = tx, py2 = ty + TAB_H;

        // Тень
        rect(ms, px+3, py2+3, tabBarW, visH+1, rgb(0,0,0,80));
        // Фон
        rect(ms, px, py2, tabBarW, visH+1, BG);
        // Нижняя граница
        rect(ms, px, py2+visH, tabBarW, 1, ACT);

        // Заголовок-разделитель если есть место
        scissor(px, py2, px+tabBarW, py2+visH);

        for (int i = 0; i < mods.size(); i++) {
            Module m  = mods.get(i);
            int    ry = py2 + i*RH - (int)panelScroll;
            if (ry+RH < py2 || ry > py2+visH) continue;

            boolean hov = in(mx,my, px, ry, tabBarW, RH);
            if (m.isEnabled()) rect(ms, px, ry, tabBarW, RH, ROW_ON);
            else if (hov)      rect(ms, px, ry, tabBarW, RH, ROW_H);

            // Левая полоска если включён
            if (m.isEnabled()) rect(ms, px, ry, 2, RH, ACT);

            // Точка-статус
            int dotC = m.isEnabled() ? DOT_ON : DOT_OFF;
            rect(ms, px+5, ry+4, 4, 4, dotC);

            // Имя
            int tc = m.isEnabled() ? TXT_ON : TXT_OFF;
            drawStr(ms, m.getName(), px+13, ry+3, tc);

            // Кейбинд
            if (m.getKeyBind() != GLFW.GLFW_KEY_UNKNOWN) {
                String kb = m.getKeyName();
                drawStr(ms, kb, px+tabBarW-fr().width(kb)-5, ry+3, rgb(80,75,120,255));
            }

            if (i < mods.size()-1) rect(ms, px+3, ry+RH-1, tabBarW-6, 1, SEP);
        }

        noScissor();

        // Скроллбар
        if (maxSc > 0) {
            int tH  = visH;
            int thH = Math.max(8, tH*tH/(contentH+1));
            int thY = py2 + (int)((tH-thH)*(panelScroll/(float)maxSc));
            rect(ms, px+tabBarW-2, py2, 2, tH, rgb(255,255,255,12));
            rect(ms, px+tabBarW-2, thY, 2, thH, ACT);
        }

        // Запоминаем ширину для hit-testing
        lastPanelW = tabBarW;
        lastPanelH = TAB_H + visH + 1;
    }

    private int lastPanelW = 0, lastPanelH = 0;

    // ── Окно настроек ─────────────────────────────────────────────────
    private void drawSettings(MatrixStack ms, int mx, int my) {
        List<Setting<?>> sets = getSettings(setMod);
        int cH    = sets.size()*SR + BH + 4;
        int visH  = Math.min(cH, height - sy - 24);
        int maxSc = Math.max(0, cH - visH);
        sSroll    = Math.max(0, Math.min(sSroll, maxSc));
        int tot   = 18 + visH;

        // Тень
        rect(ms, sx+3, sy+3, SW, tot, rgb(0,0,0,90));
        // Фон
        rect(ms, sx, sy, SW, tot, SBG);
        // Шапка
        grad(ms, sx, sy, sx+SW, sy+18, SHDR, rgb(10,8,20,255));
        rect(ms, sx, sy, SW, 2, ACT2);
        rect(ms, sx, sy+17, SW, 1, SEP);

        // Заголовок
        drawStr(ms, setMod.getName(), sx+7, sy+5, TXT_ON);
        // Галочка закрытия
        drawStr(ms, "x", sx+SW-10, sy+5, TXT_OFF);

        int ct = sy+18, cb = ct+visH;
        scissor(sx, ct, sx+SW, cb);

        for (int i = 0; i < sets.size(); i++) {
            int ry = ct + i*SR - (int)sSroll;
            if (ry+SR >= ct && ry <= cb) drawSetRow(ms, sets.get(i), i, ry, mx, my);
        }

        // Бинд
        int by = ct + sets.size()*SR - (int)sSroll;
        if (by >= ct-BH && by <= cb+BH) {
            boolean bh = in(mx,my,sx,by,SW,BH);
            if (bh) rect(ms,sx,by,SW,BH,ROW_H);
            rect(ms,sx,by,SW,1,SEP);
            String bl = waitBind ? ">> Press key..." : "Bind: "+setMod.getKeyName();
            drawStr(ms, bl, sx+7, by+4, waitBind ? GOLD : SUB);
        }

        noScissor();
        rect(ms, sx, sy+tot, SW, 1, ACT2);

        if (maxSc > 0) {
            int tH=visH, thH=Math.max(8,tH*tH/(cH+1));
            int thY=ct+(int)((tH-thH)*(sSroll/(float)maxSc));
            rect(ms,sx+SW-2,ct,2,tH,rgb(255,255,255,12));
            rect(ms,sx+SW-2,thY,2,thH,ACT2);
        }
    }

    @SuppressWarnings("unchecked")
    private void drawSetRow(MatrixStack ms, Setting<?> s, int idx, int ry, int mx, int my) {
        if (idx>0) rect(ms,sx+4,ry,SW-8,1,SEP);
        boolean hov = in(mx,my,sx,ry,SW,SR);
        if (hov) rect(ms,sx,ry,SW,SR,ROW_H);

        drawStr(ms, s.getName(), sx+7, ry+5, SUB);

        Setting.Type t = s.getType();
        if (t == Setting.Type.TOGGLE) {
            boolean v = (Boolean)s.getValue();
            String lbl = v?"ON":"OFF";
            int bw = fr().width(lbl)+8, bx2 = sx+SW-bw-6;
            rect(ms, bx2, ry+3, bw, SR-7, v ? rgb(40,130,60,200) : rgb(130,40,40,200));
            drawStr(ms, lbl, bx2+4, ry+5, rgb(255,255,255,255));
        } else if (t == Setting.Type.ENUM) {
            String lbl = ((Enum<?>)s.getValue()).name();
            drawStr(ms, lbl, sx+SW-fr().width(lbl)-7, ry+5, GOLD);
        } else if (t==Setting.Type.SLIDER_INT || t==Setting.Type.SLIDER_FLOAT) {
            double val   = s.getValue() instanceof Float ? (Float)s.getValue() : (Integer)s.getValue();
            int    bx2   = sx+7, bw=SW-16, by2=ry+SR-6, bh=2;
            int    fill  = (int)((val-s.getMin())/(s.getMax()-s.getMin())*bw);
            rect(ms, bx2, by2, bw, bh, SLBG);
            grad(ms, bx2, by2, bx2+Math.max(0,fill), by2+bh, ACT, ACT2);
            rect(ms, bx2+fill-2, by2-2, 4, bh+4, rgb(200,195,255,255));
            String vs = s.getValue() instanceof Float
                ? String.format("%.2f", val) : String.valueOf((int)val);
            drawStr(ms, vs, sx+SW-fr().width(vs)-7, ry+5, TXT_ON);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  MOUSE
    // ══════════════════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int imx=(int)mx, imy=(int)my;

        // Закрыть настройки — клик по X
        if (setMod!=null && in(imx,imy, sx+SW-14,sy+1,14,16) && btn==0) {
            setMod=null; waitBind=false; return true;
        }

        // Тащим настройки
        if (setMod!=null && in(imx,imy,sx,sy,SW,18) && btn==0) {
            sDrag=true; sdox=imx-sx; sdoy=imy-sy; return true;
        }

        // Клик по настройкам
        if (setMod!=null) {
            if (handleSetClick(imx,imy,btn)) return true;
            if (!in(imx,imy,sx,sy,SW,18+(int)Math.min(getSettings(setMod).size()*SR+BH, height))) {
                setMod=null; waitBind=false;
            }
            return true;
        }

        // Таб-бар
        Module.Category[] cats = Module.Category.values();
        int cx = panelX+1;
        for (Module.Category cat : cats) {
            int tw = fr().width(cap(cat.name()))+TAB_PD*2;
            if (in(imx,imy,cx,panelY,tw,TAB_H)) {
                if (btn==0) { activeTab=cat; panelScroll=0; return true; }
            }
            cx+=tw;
        }

        // Тащим панель за таб-бар
        if (in(imx,imy,panelX,panelY,lastPanelW,TAB_H) && btn==0) {
            pDrag=true; pdox=imx-panelX; pdoy=imy-panelY; return true;
        }

        // Строки модулей
        List<Module> mods = CombatClient.moduleManager.getByCategory(activeTab);
        int py2 = panelY+TAB_H;
        int visH2 = Math.min(mods.size()*RH, height-py2-16);
        for (int i=0; i<mods.size(); i++) {
            int ry = py2+i*RH-(int)panelScroll;
            if (ry+RH<py2||ry>py2+visH2) continue;
            if (in(imx,imy,panelX,ry,lastPanelW,RH)) {
                if (btn==0) mods.get(i).toggle();
                else if (btn==1) {
                    setMod=mods.get(i);
                    sx=Math.min(imx+4, width-SW-4);
                    sy=Math.max(4, imy-10);
                    waitBind=false; dragIdx=-1; sSroll=0;
                }
                return true;
            }
        }
        return super.mouseClicked(mx,my,btn);
    }

    @SuppressWarnings("unchecked")
    private boolean handleSetClick(int mx, int my, int btn) {
        List<Setting<?>> sets = getSettings(setMod);
        int ct=sy+18;
        for (int i=0; i<sets.size(); i++) {
            Setting<?> s=sets.get(i);
            int ry=ct+i*SR-(int)sSroll;
            if (!in(mx,my,sx,ry,SW,SR)) continue;
            Setting.Type t=s.getType();
            if (t==Setting.Type.TOGGLE&&btn==0) ((Setting<Boolean>)s).setValue(!(Boolean)s.getValue());
            else if (t==Setting.Type.ENUM&&btn==0) s.cycleEnum();
            else if (t==Setting.Type.SLIDER_INT||t==Setting.Type.SLIDER_FLOAT){dragIdx=i;applySlider(s,mx);}
            return true;
        }
        int by=ct+sets.size()*SR-(int)sSroll;
        if (in(mx,my,sx,by,SW,BH)&&btn==0){waitBind=true;return true;}
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (sDrag){sx=(int)(mx-sdox);sy=(int)(my-sdoy);return true;}
        if (pDrag){panelX=(int)(mx-pdox);panelY=(int)(my-pdoy);return true;}
        if (dragIdx>=0&&setMod!=null){
            List<Setting<?>> s=getSettings(setMod);
            if (dragIdx<s.size()) applySlider(s.get(dragIdx),(int)mx);
            return true;
        }
        return super.mouseDragged(mx,my,btn,dx,dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn){
        sDrag=false; pDrag=false; dragIdx=-1;
        return super.mouseReleased(mx,my,btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta){
        if (setMod!=null&&in((int)mx,(int)my,sx,sy,SW,height)){sSroll-=(float)(delta*10);return true;}
        if (in((int)mx,(int)my,panelX,panelY,lastPanelW,lastPanelH)){panelScroll-=(float)(delta*10);return true;}
        return super.mouseScrolled(mx,my,delta);
    }

    @Override
    public boolean keyPressed(int key,int scan,int mods){
        if (waitBind&&setMod!=null){
            setMod.setKeyBind(key==GLFW.GLFW_KEY_ESCAPE||key==GLFW.GLFW_KEY_DELETE?GLFW.GLFW_KEY_UNKNOWN:key);
            waitBind=false; return true;
        }
        if (key==GLFW.GLFW_KEY_ESCAPE){
            if (setMod!=null){setMod=null;return true;}
            onClose(); return true;
        }
        return super.keyPressed(key,scan,mods);
    }

    // ══════════════════════════════════════════════════════════════════
    //  UTILS
    // ══════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void applySlider(Setting<?> s, int mx){
        double r=Math.max(0,Math.min(1.0,(mx-sx-7)/(double)(SW-16)));
        double v=s.getMin()+r*(s.getMax()-s.getMin());
        if (s.getValue() instanceof Float)((Setting<Float>)s).setValue((float)v);
        else ((Setting<Integer>)s).setValue((int)Math.round(v));
    }

    private List<Setting<?>> getSettings(Module m){
        List<Setting<?>> list=new ArrayList<>();
        for (java.lang.reflect.Field f:m.getClass().getDeclaredFields()){
            f.setAccessible(true);
            try{Object v=f.get(m);if(v instanceof Setting)list.add((Setting<?>)v);}
            catch(IllegalAccessException ignored){}
        }
        return list;
    }

    private boolean in(int mx,int my,int x,int y,int w,int h){
        return mx>=x&&mx<x+w&&my>=y&&my<y+h;
    }

    // ── Рисование ─────────────────────────────────────────────────────
    private void rect(MatrixStack ms,int x,int y,int w,int h,int c){
        if(w<=0||h<=0)return; fill(ms,x,y,x+w,y+h,c);
    }

    private void drawStr(MatrixStack ms,String text,int x,int y,int color){
        fr().drawShadow(ms,text,x,y,color);
    }

    private void grad(MatrixStack ms,int x1,int y1,int x2,int y2,int top,int bot){
        if(x2<=x1||y2<=y1)return;
        float at=a(top),rt=r(top),gt=g(top),bt=b(top);
        float ab=a(bot), rb=r(bot), gb=g(bot), bb=b(bot);
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        Tessellator tess=Tessellator.getInstance();
        BufferBuilder buf=tess.getBuilder();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buf.vertex(ms.last().pose(),x1,y1,0).color(rt,gt,bt,at).endVertex();
        buf.vertex(ms.last().pose(),x1,y2,0).color(rb,gb,bb,ab).endVertex();
        buf.vertex(ms.last().pose(),x2,y2,0).color(rb,gb,bb,ab).endVertex();
        buf.vertex(ms.last().pose(),x2,y1,0).color(rt,gt,bt,at).endVertex();
        tess.end();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private void scissor(int x1,int y1,int x2,int y2){
        double sc=Minecraft.getInstance().getWindow().getGuiScale();
        int wh=Minecraft.getInstance().getWindow().getGuiScaledHeight();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int)(x1*sc),(int)(wh*sc-y2*sc),(int)((x2-x1)*sc),(int)((y2-y1)*sc));
    }

    private void noScissor(){ GL11.glDisable(GL11.GL_SCISSOR_TEST); }

    // ── Цвет ──────────────────────────────────────────────────────────
    private static int rgb(int r,int g,int b,int a){return(a&0xFF)<<24|(r&0xFF)<<16|(g&0xFF)<<8|(b&0xFF);}
    private static float r(int c){return((c>>16)&0xFF)/255f;}
    private static float g(int c){return((c>>8)&0xFF)/255f;}
    private static float b(int c){return(c&0xFF)/255f;}
    private static float a(int c){return((c>>24)&0xFF)/255f;}
    private static String cap(String s){return s.isEmpty()?s:s.charAt(0)+s.substring(1).toLowerCase();}
}
