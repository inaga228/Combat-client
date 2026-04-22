package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ESP extends Module {

    public enum TargetMode { PLAYERS, MOBS, ALL }
    public enum ShapeMode  { LINES, SIDES, BOTH }

    public final Setting<TargetMode> targets     = new Setting<>("Targets",     TargetMode.PLAYERS);
    public final Setting<ShapeMode>  shapeMode   = new Setting<>("ShapeMode",   ShapeMode.BOTH);
    public final Setting<Boolean>    ignoreSelf  = new Setting<>("IgnoreSelf",  true);
    public final Setting<Float>      fillOpacity = new Setting<>("FillOpacity", 0.15f).range(0f, 1f);
    public final Setting<Float>      lineWidth   = new Setting<>("LineWidth",   1.5f).range(0.5f, 4f);

    private static final int[] COL_PLAYER = {255, 255, 255};
    private static final int[] COL_MOB    = {255,  50,  50};
    private static final int[] COL_ANIMAL = { 50, 255,  50};
    private static final int[] COL_MISC   = {175, 175, 175};

    public ESP() {
        super("ESP", "Renders entities through walls", Category.RENDERER);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        float pt = event.getPartialTicks();
        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();
        MatrixStack ms = event.getMatrixStack();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.lineWidth(lineWidth.getValue());

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        ms.pushPose();

        for (Entity entity : mc.level.getAllEntities()) {
            if (!shouldRender(entity)) continue;

            int[] col = getColor(entity);
            int r = col[0], g = col[1], b = col[2];

            double ex = MathHelper.lerp(pt, entity.xOld, entity.getX()) - cam.x;
            double ey = MathHelper.lerp(pt, entity.yOld, entity.getY()) - cam.y;
            double ez = MathHelper.lerp(pt, entity.zOld, entity.getZ()) - cam.z;

            AxisAlignedBB bb = entity.getBoundingBox();
            float x1 = (float)(ex + bb.minX - entity.getX());
            float y1 = (float)(ey + bb.minY - entity.getY());
            float z1 = (float)(ez + bb.minZ - entity.getZ());
            float x2 = (float)(ex + bb.maxX - entity.getX());
            float y2 = (float)(ey + bb.maxY - entity.getY());
            float z2 = (float)(ez + bb.maxZ - entity.getZ());

            Matrix4f mat = ms.last().pose();

            if (shapeMode.getValue() != ShapeMode.LINES) {
                int a = (int)(fillOpacity.getValue() * 255);
                buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                drawBoxFill(buf, mat, x1,y1,z1, x2,y2,z2, r,g,b,a);
                tess.end();
            }

            if (shapeMode.getValue() != ShapeMode.SIDES) {
                buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                drawBoxLines(buf, mat, x1,y1,z1, x2,y2,z2, r,g,b,255);
                tess.end();
            }
        }

        ms.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
    }

    private void drawBoxLines(BufferBuilder buf, Matrix4f mat,
                              float x1,float y1,float z1, float x2,float y2,float z2,
                              int r,int g,int b,int a) {
        vert(buf,mat,x1,y1,z1,r,g,b,a); vert(buf,mat,x2,y1,z1,r,g,b,a);
        vert(buf,mat,x2,y1,z1,r,g,b,a); vert(buf,mat,x2,y1,z2,r,g,b,a);
        vert(buf,mat,x2,y1,z2,r,g,b,a); vert(buf,mat,x1,y1,z2,r,g,b,a);
        vert(buf,mat,x1,y1,z2,r,g,b,a); vert(buf,mat,x1,y1,z1,r,g,b,a);
        vert(buf,mat,x1,y2,z1,r,g,b,a); vert(buf,mat,x2,y2,z1,r,g,b,a);
        vert(buf,mat,x2,y2,z1,r,g,b,a); vert(buf,mat,x2,y2,z2,r,g,b,a);
        vert(buf,mat,x2,y2,z2,r,g,b,a); vert(buf,mat,x1,y2,z2,r,g,b,a);
        vert(buf,mat,x1,y2,z2,r,g,b,a); vert(buf,mat,x1,y2,z1,r,g,b,a);
        vert(buf,mat,x1,y1,z1,r,g,b,a); vert(buf,mat,x1,y2,z1,r,g,b,a);
        vert(buf,mat,x2,y1,z1,r,g,b,a); vert(buf,mat,x2,y2,z1,r,g,b,a);
        vert(buf,mat,x2,y1,z2,r,g,b,a); vert(buf,mat,x2,y2,z2,r,g,b,a);
        vert(buf,mat,x1,y1,z2,r,g,b,a); vert(buf,mat,x1,y2,z2,r,g,b,a);
    }

    private void drawBoxFill(BufferBuilder buf, Matrix4f mat,
                             float x1,float y1,float z1, float x2,float y2,float z2,
                             int r,int g,int b,int a) {
        vert(buf,mat,x1,y1,z1,r,g,b,a); vert(buf,mat,x1,y2,z1,r,g,b,a);
        vert(buf,mat,x2,y2,z1,r,g,b,a); vert(buf,mat,x2,y1,z1,r,g,b,a);
        vert(buf,mat,x2,y1,z2,r,g,b,a); vert(buf,mat,x2,y2,z2,r,g,b,a);
        vert(buf,mat,x1,y2,z2,r,g,b,a); vert(buf,mat,x1,y1,z2,r,g,b,a);
        vert(buf,mat,x1,y1,z2,r,g,b,a); vert(buf,mat,x1,y2,z2,r,g,b,a);
        vert(buf,mat,x1,y2,z1,r,g,b,a); vert(buf,mat,x1,y1,z1,r,g,b,a);
        vert(buf,mat,x2,y1,z1,r,g,b,a); vert(buf,mat,x2,y2,z1,r,g,b,a);
        vert(buf,mat,x2,y2,z2,r,g,b,a); vert(buf,mat,x2,y1,z2,r,g,b,a);
        vert(buf,mat,x1,y2,z1,r,g,b,a); vert(buf,mat,x1,y2,z2,r,g,b,a);
        vert(buf,mat,x2,y2,z2,r,g,b,a); vert(buf,mat,x2,y2,z1,r,g,b,a);
        vert(buf,mat,x1,y1,z2,r,g,b,a); vert(buf,mat,x1,y1,z1,r,g,b,a);
        vert(buf,mat,x2,y1,z1,r,g,b,a); vert(buf,mat,x2,y1,z2,r,g,b,a);
    }

    private void vert(BufferBuilder buf, Matrix4f mat,
                      float x,float y,float z,int r,int g,int b,int a) {
        buf.vertex(mat,x,y,z).color(r,g,b,a).endVertex();
    }

    private boolean shouldRender(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity == mc.player && ignoreSelf.getValue()) return false;
        if (!entity.isAlive()) return false;
        TargetMode t = targets.getValue();
        if (t == TargetMode.PLAYERS && !(entity instanceof PlayerEntity)) return false;
        if (t == TargetMode.MOBS    && !(entity instanceof IMob))         return false;
        return true;
    }

    private int[] getColor(Entity e) {
        if (e instanceof PlayerEntity) return COL_PLAYER;
        if (e instanceof IMob)         return COL_MOB;
        if (e instanceof AnimalEntity) return COL_ANIMAL;
        return COL_MISC;
    }
}
