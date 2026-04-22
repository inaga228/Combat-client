package com.example.combat.utils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class RenderUtil {

    public static void drawRect(MatrixStack ms, float x, float y, float w, float h, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = (color         & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buf.vertex(x,     y,     0).color(r,g,b,a).endVertex();
        buf.vertex(x,     y + h, 0).color(r,g,b,a).endVertex();
        buf.vertex(x + w, y + h, 0).color(r,g,b,a).endVertex();
        buf.vertex(x + w, y,     0).color(r,g,b,a).endVertex();
        tess.end();

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawOutline(MatrixStack ms, float x, float y, float w, float h, float lineW, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = (color         & 0xFF) / 255f;

        RenderSystem.lineWidth(lineW);
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        buf.vertex(x,     y,     0).color(r,g,b,a).endVertex();
        buf.vertex(x,     y + h, 0).color(r,g,b,a).endVertex();
        buf.vertex(x + w, y + h, 0).color(r,g,b,a).endVertex();
        buf.vertex(x + w, y,     0).color(r,g,b,a).endVertex();
        tess.end();

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static int rgba(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Читает FPS через reflection (поле fps приватное в 1.16.5 official mappings). */
    public static int getFps() {
        try {
            java.lang.reflect.Field f = net.minecraft.client.Minecraft.class.getDeclaredField("fps");
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Exception e) {
            return 0;
        }
    }
}
