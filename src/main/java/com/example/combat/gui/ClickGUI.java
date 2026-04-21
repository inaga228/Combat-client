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
 * ClickGUI — современный тёмный дизайн.
 * Left-click: toggle | Right-click: open settings | Drag header: move panel
 */
public class ClickGUI extends Screen {

    // ── Цветовая палитра ────────────────────────────────────────────
    private static final int C_BG       = 0xF0101018;
    private static final int C_HEADER   = 0xFF1A1A2A;
    private static final int C_ACCENT   = 0xFF5B5BFF;
    private static final int C_ACCENT2  = 0xFF8A2BE2;
    private static final int C_ON       = 0xFF3A3A6A;
    private static final int C_HOVER    = 0x22FFFFFF;
    private static final int C_TEXT     = 0xFFE0E0FF;
    private static final int C_SUBTEXT  = 0xFF888899;
    private static final int C_SETS_BG  = 0xF2141422;
    private static final int C_SETS_HDR = 0xFF1C1C30;
    private static final int C_SL_BG   = 0xFF222233;
    private static final int C_SL_FG   = 0xFF5B5BFF;

    // ── Размеры ──────────────────────────────────────────────────────
    private static final int PW    = 110;
    private static final int HDRH  = 16;
    private static final int ROWH  = 13;
    private static final int SW    = 140;
    private static final int SROW  = 16;
    private static final int SBIND = 14;

    private static class Panel {
        Module.Category cat;
        int x, y, dox, doy;
        boolean collapsed = false, dragging = false;
        Panel(Module.Category cat, int x, int y) { this.cat=cat; this.x=x; this.y=y; }
    }

    private final List<Panel> panels = new ArrayList<>();
    private Module  settingsMod  = null;
    private int     sx, sy, sdox, sdoy;
    private boolean sDrag        = false;
    private boolean waitBind     = false;
    private int     dragSliderIdx = -1;

    public ClickGUI() {
        super(new StringTextComponent(""));
        int x = 6;
        for (Module.Category cat : Module.Category.values()) {
            if (!CombatClient.moduleManager.getByCategory(cat).isEmpty()) {
                panels.add(new Panel(cat, x, 6));
                x += PW + 5;
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    // ════════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════════
    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        fillGradV(ms, 0, 0, width, height, 0xAA000010, 0xAA100020);
        for (Panel p : panels) renderPanel(ms, p, mx, my);
        if (settingsMod != null) renderSettings(ms, mx, my);
    }

    private void renderPanel(MatrixStack ms, Panel p, int mx, int my) {
        List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);
        int totalH = HDRH + (p.collapsed ? 0 : mods.size() * ROWH);

        fillRect(ms, p.x+2, p.y+2, PW, totalH, 0x55000000);   // тень
        fillRect(ms, p.x,   p.y,   PW, totalH, C_BG);
        fillRect(ms, p.x,   p.y,   PW, HDRH,   C_HEADER);
        fillRect(ms, p.x,   p.y,   2,  HDRH,   C_ACCENT);     // акцент полоска

        String catName = p.cat.name().charAt(0) + p.cat.name().substring(1).toLowerCase();
        font.draw(ms, catName, p.x + 7, p.y + 4, C_ACCENT);
        font.draw(ms, p.collapsed ? ">" : "v", p.x + PW - 10, p.y + 4, C_SUBTEXT);

        if (!p.collapsed) {
            for (int i = 0; i < mods.size(); i++) {
                Module m  = mods.get(i);
                int    ry = p.y + HDRH + i * ROWH;
                boolean hover = inBox(mx, my, p.x, ry, PW, ROWH);

                if (m.isEnabled())     fillRect(ms, p.x, ry, PW, ROWH, C_ON);
                else if (hover)        fillRect(ms, p.x, ry, PW, ROWH, C_HOVER);
                if (m.isEnabled())     fillRect(ms, p.x, ry, 2,  ROWH, C_ACCENT2);

                font.draw(ms, m.getName(), p.x + 7, ry + 3,
                        m.isEnabled() ? C_TEXT : C_SUBTEXT);

                if (m.getKeyBind() != GLFW.GLFW_KEY_UNKNOWN) {
                    String kb = m.getKeyName();
                    font.draw(ms, kb, p.x + PW - font.width(kb) - 4, ry + 3, 0x44AAAACC);
                }

                if (i < mods.size() - 1)
                    fillRect(ms, p.x + 2, ry + ROWH - 1, PW - 4, 1, 0x22FFFFFF);
            }
        }
        fillRect(ms, p.x, p.y + totalH, PW, 1, C_ACCENT);
    }

    private void renderSettings(MatrixStack ms, int mx, int my) {
        List<Setting<?>> settings = collectSettings(settingsMod);
        int sh = 14 + settings.size() * SROW + SBIND + 2;

        fillRect(ms, sx+3, sy+3, SW, sh, 0x66000000);
        fillRect(ms, sx,   sy,   SW, sh, C_SETS_BG);
        fillRect(ms, sx,   sy,   SW, 14, C_SETS_HDR);
        fillRect(ms, sx,   sy,   2,  14, C_ACCENT2);
        font.draw(ms, settingsMod.getName(), sx + 6, sy + 3, C_ACCENT);

        for (int i = 0; i < settings.size(); i++)
            renderSettingRow(ms, settings.get(i), sy + 14 + i * SROW, mx, my);

        int by = sy + 14 + settings.size() * SROW;
        if (inBox(mx, my, sx, by, SW, SBIND)) fillRect(ms, sx, by, SW, SBIND, C_HOVER);
        String bl = waitBind ? "Press key..." : "Bind: " + settingsMod.getKeyName();
        font.draw(ms, bl, sx + 6, by + 3, waitBind ? 0xFFFFAA00 : C_SUBTEXT);

        fillRect(ms, sx, sy + sh, SW, 1, C_ACCENT2);
    }

    @SuppressWarnings("unchecked")
    private void renderSettingRow(MatrixStack ms, Setting<?> s, int ry, int mx, int my) {
        if (inBox(mx, my, sx, ry, SW, SROW)) fillRect(ms, sx, ry, SW, SROW, C_HOVER);
        font.draw(ms, s.getName(), sx + 6, ry + 4, C_SUBTEXT);

        Setting.Type type = s.getType();
        if (type == Setting.Type.TOGGLE) {
            boolean v = (Boolean) s.getValue();
            String lbl = v ? "ON" : "OFF";
            font.draw(ms, lbl, sx + SW - font.width(lbl) - 6, ry + 4, v ? 0xFF55FF55 : 0xFFFF5555);

        } else if (type == Setting.Type.ENUM) {
            String lbl = ((Enum<?>) s.getValue()).name();
            font.draw(ms, lbl, sx + SW - font.width(lbl) - 6, ry + 4, 0xFFFFD700);

        } else if (type == Setting.Type.SLIDER_INT || type == Setting.Type.SLIDER_FLOAT) {
            double val    = s.getValue() instanceof Float ? (Float) s.getValue() : (Integer) s.getValue();
            int    barX   = sx + 6, barW = SW - 12, barY = ry + 10, barH = 3;
            int    filled = (int)((val - s.getMin()) / (s.getMax() - s.getMin()) * barW);

            fillRect(ms, barX, barY, barW, barH, C_SL_BG);
            fillRect(ms, barX, barY, filled, barH, C_SL_FG);
            fillRect(ms, barX + filled - 1, barY - 1, 3, barH + 2, 0xFFAAAAAA);

            String vs = s.getValue() instanceof Float
                    ? String.format("%.2f", val) : String.valueOf((int) val);
            font.draw(ms, vs, sx + SW - font.width(vs) - 6, ry + 4, C_TEXT);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MOUSE
    // ════════════════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (settingsMod != null && inBox((int)mx,(int)my, sx, sy, SW, 14) && btn == 0) {
            sDrag = true; sdox=(int)(mx-sx); sdoy=(int)(my-sy); return true;
        }
        if (settingsMod != null) {
            if (handleSettingsClick((int)mx,(int)my,btn)) return true;
            settingsMod = null; waitBind = false; return true;
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
                            settingsMod = mods.get(i);
                            sx=(int)mx+4; sy=(int)my-7; waitBind=false; dragSliderIdx=-1;
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
        List<Setting<?>> settings = collectSettings(settingsMod);
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            int ry = sy + 14 + i * SROW;
            if (!inBox(mx,my,sx,ry,SW,SROW)) continue;
            Setting.Type type = s.getType();
            if (type == Setting.Type.TOGGLE && btn==0) {
                Setting<Boolean> sb = (Setting<Boolean>) s; sb.setValue(!sb.getValue());
            } else if (type == Setting.Type.ENUM && btn==0) {
                s.cycleEnum();
            } else if (type==Setting.Type.SLIDER_INT || type==Setting.Type.SLIDER_FLOAT) {
                dragSliderIdx = i; applySlider(s, mx);
            }
            return true;
        }
        int by = sy + 14 + settings.size() * SROW;
        if (inBox(mx,my,sx,by,SW,SBIND) && btn==0) { waitBind=true; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (sDrag) { sx=(int)(mx-sdox); sy=(int)(my-sdoy); return true; }
        for (Panel p : panels) if (p.dragging) { p.x=(int)(mx-p.dox); p.y=(int)(my-p.doy); return true; }
        if (dragSliderIdx>=0 && settingsMod!=null) {
            List<Setting<?>> s = collectSettings(settingsMod);
            if (dragSliderIdx < s.size()) applySlider(s.get(dragSliderIdx),(int)mx);
            return true;
        }
        return super.mouseDragged(mx,my,btn,dx,dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        sDrag=false; dragSliderIdx=-1;
        for (Panel p : panels) p.dragging=false;
        return super.mouseReleased(mx,my,btn);
    }

    @SuppressWarnings("unchecked")
    private void applySlider(Setting<?> s, int mx) {
        double ratio = Math.max(0, Math.min(1.0, (mx - sx - 6) / (double)(SW - 12)));
        double val   = s.getMin() + ratio * (s.getMax() - s.getMin());
        if (s.getValue() instanceof Float) ((Setting<Float>)s).setValue((float)val);
        else ((Setting<Integer>)s).setValue((int)Math.round(val));
    }

    // ════════════════════════════════════════════════════════════════
    //  KEYS
    // ════════════════════════════════════════════════════════════════
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (waitBind && settingsMod!=null) {
            if (key==GLFW.GLFW_KEY_ESCAPE||key==GLFW.GLFW_KEY_DELETE)
                settingsMod.setKeyBind(GLFW.GLFW_KEY_UNKNOWN);
            else settingsMod.setKeyBind(key);
            waitBind=false; return true;
        }
        if (key==GLFW.GLFW_KEY_ESCAPE) {
            if (settingsMod!=null) { settingsMod=null; return true; }
            onClose(); return true;
        }
        return super.keyPressed(key,scan,mods);
    }

    // ════════════════════════════════════════════════════════════════
    //  УТИЛИТЫ
    // ════════════════════════════════════════════════════════════════
    private List<Setting<?>> collectSettings(Module m) {
        List<Setting<?>> list = new ArrayList<>();
        for (java.lang.reflect.Field f : m.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            try { Object v=f.get(m); if(v instanceof Setting) list.add((Setting<?>)v); }
            catch (IllegalAccessException ignored) {}
        }
        return list;
    }

    private boolean inBox(int mx,int my,int x,int y,int w,int h) {
        return mx>=x && mx<x+w && my>=y && my<y+h;
    }

    private void fillRect(MatrixStack ms, int x, int y, int w, int h, int color) {
        fill(ms, x, y, x+w, y+h, color);
    }

    /** Градиентный прямоугольник сверху вниз через GL-тесселятор. */
    private void fillGradV(MatrixStack ms, int x1, int y1, int x2, int y2, int top, int bot) {
        float at=((top>>24)&0xFF)/255f, rt=((top>>16)&0xFF)/255f,
              gt=((top>>8)&0xFF)/255f,  bt=(top&0xFF)/255f;
        float ab=((bot>>24)&0xFF)/255f, rb=((bot>>16)&0xFF)/255f,
              gb=((bot>>8)&0xFF)/255f,  bb=(bot&0xFF)/255f;

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buf.vertex(x1,y1,0).color(rt,gt,bt,at).endVertex();
        buf.vertex(x1,y2,0).color(rb,gb,bb,ab).endVertex();
        buf.vertex(x2,y2,0).color(rb,gb,bb,ab).endVertex();
        buf.vertex(x2,y1,0).color(rt,gt,bt,at).endVertex();
        tess.end();

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
