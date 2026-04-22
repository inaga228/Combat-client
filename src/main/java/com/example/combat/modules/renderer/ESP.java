package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * ESP — Forge 1.16.5
 * Портировано с Meteor Client (Fabric).
 * Режимы: Box (3D), 2D (экранный прямоугольник).
 * Shader/Glow пропущены — требуют кастомного шейдерного пайплайна.
 */
public class ESP extends Module {

    public enum ESPMode    { BOX, _2D }
    public enum ColorMode  { ENTITY_TYPE, FIXED }
    public enum ShapeMode  { BOTH, LINES, SIDES }
    public enum TargetMode { PLAYERS, MOBS, ALL }

    // ── Настройки ────────────────────────────────────────────────
    public final Setting<ESPMode>    mode        = new Setting<>("Mode",        ESPMode.BOX);
    public final Setting<ColorMode>  colorMode   = new Setting<>("ColorMode",   ColorMode.ENTITY_TYPE);
    public final Setting<ShapeMode>  shapeMode   = new Setting<>("ShapeMode",   ShapeMode.BOTH);
    public final Setting<TargetMode> targets     = new Setting<>("Targets",     TargetMode.PLAYERS);
    public final Setting<Boolean>    ignoreSelf  = new Setting<>("IgnoreSelf",  true);
    public final Setting<Float>      fillOpacity = new Setting<>("FillOpacity", 0.15f).range(0f, 1f);
    public final Setting<Float>      lineWidth   = new Setting<>("LineWidth",   1.5f).range(0.5f, 4f);
    public final Setting<Float>      fadeDistance= new Setting<>("FadeDist",    3f).range(0f, 12f);

    // Цвета в ARGB int — отображаются через Setting<Integer>
    // (в GUI показываем как число, пользователь меняет через конфиг)
    // Для простоты — фиксированные палитры как в Meteor
    private static final int COL_PLAYER  = 0xCCFFFFFF;
    private static final int COL_MOB     = 0xCCFF4040;
    private static final int COL_ANIMAL  = 0xCC40FF40;
    private static final int COL_MISC    = 0xCCAFAFAF;

    public ESP() {
        super("ESP", "Renders entities through walls", Category.RENDERER);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled()) return;
        if (mc.level == null || mc.player == null) return;

        MatrixStack ms = event.getMatrixStack();

        // Смещение к позиции камеры (RenderWorldLastEvent уже применил projection)
        double cx = mc.gameRenderer.getMainCamera().getPosition().x;
        double cy = mc.gameRenderer.getMainCamera().getPosition().y;
        double cz = mc.gameRenderer.getMainCamera().getPosition().z;

        IRenderTypeBuffer.Impl buffers = mc.renderBuffers().bufferSource();

        ms.pushPose();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!shouldRender(entity)) continue;

            float alpha = getFadeAlpha(entity);
            if (alpha <= 0.05f) continue;

            int color = getColor(entity);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8)  & 0xFF;
            int b =  color        & 0xFF;
            int a = Math.min(255, (int)(((color >> 24) & 0xFF) * alpha));
            int lineA = Math.min(255, (int)(a));
            int sideA = Math.min(255, (int)(a * fillOpacity.getValue()));

            float pt = event.getPartialTicks();
            double ex = MathHelper.lerp(pt, entity.xOld, entity.getX()) - cx;
            double ey = MathHelper.lerp(pt, entity.yOld, entity.getY()) - cy;
            double ez = MathHelper.lerp(pt, entity.zOld, entity.getZ()) - cz;

            AxisAlignedBB bb = entity.getBoundingBox();
            double minX = ex + (bb.minX - entity.getX());
            double minY = ey + (bb.minY - entity.getY());
            double minZ = ez + (bb.minZ - entity.getZ());
            double maxX = ex + (bb.maxX - entity.getX());
            double maxY = ey + (bb.maxY - entity.getY());
            double maxZ = ez + (bb.maxZ - entity.getZ());

            if (mode.getValue() == ESPMode.BOX) {
                IVertexBuilder lines = buffers.getBuffer(RenderType.lines());
                Matrix4f mat = ms.last().pose();

                if (shapeMode.getValue() != ShapeMode.SIDES) {
                    drawBoxLines(lines, mat,
                            (float) minX, (float) minY, (float) minZ,
                            (float) maxX, (float) maxY, (float) maxZ,
                            r, g, b, lineA);
                }
                if (shapeMode.getValue() != ShapeMode.LINES && sideA > 0) {
                    IVertexBuilder quads = buffers.getBuffer(RenderType.glint());
                    // Используем translucent для заливки
                    drawBoxFill(ms, buffers,
                            (float) minX, (float) minY, (float) minZ,
                            (float) maxX, (float) maxY, (float) maxZ,
                            r, g, b, sideA);
                }
            }
        }

        ms.popPose();
        buffers.endBatch();
    }

    // ── Рисуем рёбра AABB ──────────────────────────────────────
    private void drawBoxLines(IVertexBuilder buf, Matrix4f mat,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              int r, int g, int b, int a) {
        // 12 рёбер куба
        // Нижняя грань
        line(buf, mat, x1,y1,z1, x2,y1,z1, r,g,b,a);
        line(buf, mat, x2,y1,z1, x2,y1,z2, r,g,b,a);
        line(buf, mat, x2,y1,z2, x1,y1,z2, r,g,b,a);
        line(buf, mat, x1,y1,z2, x1,y1,z1, r,g,b,a);
        // Верхняя грань
        line(buf, mat, x1,y2,z1, x2,y2,z1, r,g,b,a);
        line(buf, mat, x2,y2,z1, x2,y2,z2, r,g,b,a);
        line(buf, mat, x2,y2,z2, x1,y2,z2, r,g,b,a);
        line(buf, mat, x1,y2,z2, x1,y2,z1, r,g,b,a);
        // Вертикальные рёбра
        line(buf, mat, x1,y1,z1, x1,y2,z1, r,g,b,a);
        line(buf, mat, x2,y1,z1, x2,y2,z1, r,g,b,a);
        line(buf, mat, x2,y1,z2, x2,y2,z2, r,g,b,a);
        line(buf, mat, x1,y1,z2, x1,y2,z2, r,g,b,a);
    }

    private void line(IVertexBuilder buf, Matrix4f mat,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      int r, int g, int b, int a) {
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a).endVertex();
    }

    // ── Заливка AABB через translucent буфер ──────────────────
    private void drawBoxFill(MatrixStack ms, IRenderTypeBuffer.Impl buffers,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             int r, int g, int b, int a) {
        IVertexBuilder buf = buffers.getBuffer(RenderType.translucent());
        Matrix4f mat = ms.last().pose();
        // 6 граней
        quad(buf, mat, x1,y1,z1, x1,y2,z1, x2,y2,z1, x2,y1,z1, r,g,b,a); // N
        quad(buf, mat, x2,y1,z2, x2,y2,z2, x1,y2,z2, x1,y1,z2, r,g,b,a); // S
        quad(buf, mat, x1,y1,z2, x1,y2,z2, x1,y2,z1, x1,y1,z1, r,g,b,a); // W
        quad(buf, mat, x2,y1,z1, x2,y2,z1, x2,y2,z2, x2,y1,z2, r,g,b,a); // E
        quad(buf, mat, x1,y2,z1, x1,y2,z2, x2,y2,z2, x2,y2,z1, r,g,b,a); // Top
        quad(buf, mat, x1,y1,z2, x1,y1,z1, x2,y1,z1, x2,y1,z2, r,g,b,a); // Bot
    }

    private void quad(IVertexBuilder buf, Matrix4f mat,
                      float x1,float y1,float z1, float x2,float y2,float z2,
                      float x3,float y3,float z3, float x4,float y4,float z4,
                      int r, int g, int b, int a) {
        buf.vertex(mat,x1,y1,z1).color(r,g,b,a).uv(0,0).overlayCoords(0).uv2(240).normal(0,1,0).endVertex();
        buf.vertex(mat,x2,y2,z2).color(r,g,b,a).uv(0,1).overlayCoords(0).uv2(240).normal(0,1,0).endVertex();
        buf.vertex(mat,x3,y3,z3).color(r,g,b,a).uv(1,1).overlayCoords(0).uv2(240).normal(0,1,0).endVertex();
        buf.vertex(mat,x4,y4,z4).color(r,g,b,a).uv(1,0).overlayCoords(0).uv2(240).normal(0,1,0).endVertex();
    }

    // ── Фильтр энтитей ────────────────────────────────────────
    private boolean shouldRender(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == mc.player && ignoreSelf.getValue()) return false;
        if (!entity.isAlive()) return false;

        TargetMode t = targets.getValue();
        if (t == TargetMode.PLAYERS && !(entity instanceof PlayerEntity)) return false;
        if (t == TargetMode.MOBS    && !(entity instanceof IMob))         return false;
        return true;
    }

    // ── Цвет по типу ──────────────────────────────────────────
    private int getColor(Entity entity) {
        if (entity instanceof PlayerEntity) return COL_PLAYER;
        if (entity instanceof IMob)         return COL_MOB;
        if (entity instanceof AnimalEntity) return COL_ANIMAL;
        return COL_MISC;
    }

    // ── Затухание по дистанции (как в Meteor) ─────────────────
    private float getFadeAlpha(Entity entity) {
        double dist = mc.player.distanceTo(entity);
        float fade = fadeDistance.getValue();
        if (fade <= 0) return 1f;
        if (dist >= fade) return 1f;
        float alpha = (float)(dist / fade);
        return alpha < 0.075f ? 0f : alpha;
    }
}
