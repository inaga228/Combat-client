package com.example.combat.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * Combat Client — Custom Main Menu
 *
 * Дизайн: тёмный фон с анимированными частицами,
 * большой заголовок слева, кнопки вертикально по центру.
 */
public class MainMenu extends Screen {

    // ── Цвета ─────────────────────────────────────────────────────────
    private static final int C_BG1  = rgb(4,   4,  10, 255);
    private static final int C_BG2  = rgb(8,   5,  18, 255);
    private static final int C_ACC  = rgb(100, 90, 255, 255);
    private static final int C_ACC2 = rgb(160, 90, 240, 255);
    private static final int C_TXT  = rgb(220, 215,255, 255);
    private static final int C_SUB  = rgb(110, 100,160, 255);
    private static final int C_BTN  = rgb(16,  14,  30, 230);
    private static final int C_BHOV = rgb(26,  22,  48, 240);
    private static final int C_BORD = rgb(70,  60, 180, 200);

    // ── Частицы ───────────────────────────────────────────────────────
    private static final int PARTICLE_COUNT = 55;
    private final float[] px  = new float[PARTICLE_COUNT];
    private final float[] py2 = new float[PARTICLE_COUNT];
    private final float[] pvx = new float[PARTICLE_COUNT];
    private final float[] pvy = new float[PARTICLE_COUNT];
    private final float[] psize = new float[PARTICLE_COUNT];
    private final int[]   pclr = new int[PARTICLE_COUNT];
    private final Random  rng  = new Random();

    // ── Анимация ──────────────────────────────────────────────────────
    private float openT  = 0;
    private float titleX = -300;
    private float btnAlpha = 0;

    // ── Кнопки ────────────────────────────────────────────────────────
    private static final int BTN_W = 160, BTN_H = 20, BTN_GAP = 5;

    private FontRenderer fr() { return Minecraft.getInstance().font; }

    public MainMenu() { super(new StringTextComponent("")); }

    @Override
    protected void init() {
        // Инициализация частиц
        for (int i = 0; i < PARTICLE_COUNT; i++) respawn(i, true);

        // Кнопки — по центру по вертикали
        int bx = width/2 - BTN_W/2;
        int totalH = 4 * (BTN_H + BTN_GAP) - BTN_GAP;
        int startY = height/2 - totalH/2 + 10;

        clearWidgets();

        addButton(new CCButton(bx, startY,                          "Singleplayer", b -> minecraft.setScreen(new net.minecraft.client.gui.screen.WorldSelectionScreen(this))));
        addButton(new CCButton(bx, startY + (BTN_H+BTN_GAP),       "Multiplayer",  b -> minecraft.setScreen(new net.minecraft.client.gui.screen.MultiplayerScreen(this))));
        addButton(new CCButton(bx, startY + (BTN_H+BTN_GAP)*2,     "Options",      b -> minecraft.setScreen(new net.minecraft.client.gui.screen.OptionsScreen(this, minecraft.options))));
        addButton(new CCButton(bx, startY + (BTN_H+BTN_GAP)*3,     "Quit",         b -> minecraft.stop()));

        openT = 0; titleX = -300; btnAlpha = 0;
    }

    // ── Кастомная кнопка ──────────────────────────────────────────────
    private class CCButton extends Button {
        CCButton(int x, int y, String text, Button.OnPress onPress) {
            super(x, y, BTN_W, BTN_H, new StringTextComponent(text), onPress);
        }

        @Override
        public void renderButton(MatrixStack ms, int mx, int my, float pt) {
            boolean hov = isHovered();
            // Основной прямоугольник
            fillRect(ms, x, y, width, height, hov ? C_BHOV : C_BTN);
            // Левая акцент-полоска
            fillRect(ms, x, y, 2, height, hov ? C_ACC2 : C_BORD);
            // Верхняя граница
            fillRect(ms, x, y, width, 1, rgb(255,255,255, hov?30:15));
            // Нижняя граница
            fillRect(ms, x, y+height-1, width, 1, rgb(0,0,0,60));
            // Текст
            int tc = hov ? rgb(230,225,255,255) : rgb(170,165,210,255);
            fr().drawShadow(ms, getMessage().getString(), x+10, y+6, tc);

            // Маленький треугольник-стрелка справа при ховере
            if (hov) {
                fr().drawShadow(ms, ">", x+width-12, y+6, C_ACC);
            }
        }
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        openT = Math.min(1f, openT + 0.04f);

        // ── Фон ───────────────────────────────────────────────────────
        drawGradBg(ms);

        // ── Частицы ───────────────────────────────────────────────────
        updateAndDrawParticles(ms);

        // ── Анимация появления ────────────────────────────────────────
        titleX += (30f - titleX) * 0.12f;
        btnAlpha = Math.min(1f, btnAlpha + 0.04f);

        // ── Левая панель — заголовок ──────────────────────────────────
        int lx = (int) titleX;
        int ly = height/2 - 50;

        // Тонкая акцент-линия слева
        fillRect(ms, lx, ly-10, 3, 80, C_ACC);
        fillRect(ms, lx, ly-10, 3, 80, C_ACC2); // перекрыть для яркости

        // "COMBAT" — большой, разбитый на буквы с акцентом
        drawBigTitle(ms, lx+12, ly);

        // Подзаголовок
        fr().drawShadow(ms, "CLIENT", lx+14, ly+22, C_ACC2);
        fr().drawShadow(ms, "Forge 1.16.5", lx+14, ly+36, C_SUB);

        // Версия
        fr().drawShadow(ms, "v1.0.0", lx+14, ly+48, rgb(70,65,110,255));

        // Тонкая горизонтальная линия под заголовком
        fillRect(ms, lx+12, ly+60, 120, 1, rgb(80,70,180,160));

        // ── Кнопки с альфой ───────────────────────────────────────────
        // Кнопки рендерим через super который вызывает их renderButton
        RenderSystem.color4f(1,1,1, btnAlpha);
        super.render(ms, mx, my, pt);
        RenderSystem.color4f(1,1,1,1);

        // ── Копирайт снизу ────────────────────────────────────────────
        fr().drawShadow(ms, "Combat Client  |  Not affiliated with Mojang", 4, height-10, rgb(50,45,80,255));
    }

    // ── Большой заголовок (имитация крупного шрифта через масштаб) ────
    private void drawBigTitle(MatrixStack ms, int x, int y) {
        ms.pushPose();
        ms.translate(x, y, 0);
        ms.scale(2.0f, 2.0f, 1.0f);
        // Тень
        fr().drawShadow(ms, "COMBAT", 1, 1, rgb(0,0,0,150));
        // Основной текст с градиентом через два вызова
        fr().drawShadow(ms, "COMBAT", 0, 0, C_ACC);
        ms.popPose();
    }

    // ── Частицы ───────────────────────────────────────────────────────
    private void respawn(int i, boolean randomY) {
        px[i]    = rng.nextFloat() * width;
        py2[i]   = randomY ? rng.nextFloat() * height : height + 2;
        pvx[i]   = (rng.nextFloat() - 0.5f) * 0.3f;
        pvy[i]   = -(0.2f + rng.nextFloat() * 0.5f);
        psize[i] = 0.5f + rng.nextFloat() * 1.5f;

        int brightness = 60 + rng.nextInt(80);
        int alpha      = 80 + rng.nextInt(100);
        boolean purple = rng.nextBoolean();
        pclr[i] = purple
            ? rgb(brightness/2, brightness/2, brightness+60, alpha)
            : rgb(brightness, brightness-20, brightness+80, alpha);
    }

    private void updateAndDrawParticles(MatrixStack ms) {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            px[i]  += pvx[i];
            py2[i] += pvy[i];
            if (py2[i] < -4) respawn(i, false);

            float s  = psize[i];
            float fa = a(pclr[i]);
            float fr2 = r(pclr[i]), fg = g(pclr[i]), fb = b(pclr[i]);
            buf.vertex(ms.last().pose(), px[i],   py2[i],   0).color(fr2,fg,fb,fa).endVertex();
            buf.vertex(ms.last().pose(), px[i],   py2[i]+s, 0).color(fr2,fg,fb,fa).endVertex();
            buf.vertex(ms.last().pose(), px[i]+s, py2[i]+s, 0).color(fr2,fg,fb,fa).endVertex();
            buf.vertex(ms.last().pose(), px[i]+s, py2[i],   0).color(fr2,fg,fb,fa).endVertex();
        }
        tess.end();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    // ── Фон ───────────────────────────────────────────────────────────
    private void drawGradBg(MatrixStack ms) {
        float at=a(C_BG1),rt=r(C_BG1),gt=g(C_BG1),bt=b(C_BG1);
        float ab=a(C_BG2),rb=r(C_BG2),gb=g(C_BG2),bb=b(C_BG2);
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        Tessellator tess=Tessellator.getInstance();
        BufferBuilder buf=tess.getBuilder();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buf.vertex(ms.last().pose(),0,0,0).color(rt,gt,bt,at).endVertex();
        buf.vertex(ms.last().pose(),0,height,0).color(rb,gb,bb,ab).endVertex();
        buf.vertex(ms.last().pose(),width,height,0).color(rb,gb,bb,ab).endVertex();
        buf.vertex(ms.last().pose(),width,0,0).color(rt,gt,bt,at).endVertex();
        tess.end();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static void fillRect(MatrixStack ms,int x,int y,int w,int h,int c){
        if(w<=0||h<=0) return;
        Screen.fill(ms,x,y,x+w,y+h,c);
    }

    // ── Цвет ──────────────────────────────────────────────────────────
    private static int  rgb(int r,int g,int b,int a){return(a&0xFF)<<24|(r&0xFF)<<16|(g&0xFF)<<8|(b&0xFF);}
    private static float r(int c){return((c>>16)&0xFF)/255f;}
    private static float g(int c){return((c>>8)&0xFF)/255f;}
    private static float b(int c){return(c&0xFF)/255f;}
    private static float a(int c){return((c>>24)&0xFF)/255f;}

    @Override public boolean isPauseScreen() { return false; }

    private void clearWidgets() { this.buttons.clear(); this.children.clear(); }
}
