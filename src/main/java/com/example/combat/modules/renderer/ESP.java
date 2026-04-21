package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ESP extends Module {

    public enum Mode { BOX, OUTLINE }

    public final Setting<Mode>    mode    = new Setting<>("Mode", Mode.BOX);
    public final Setting<Boolean> players = new Setting<>("Players", true);
    public final Setting<Boolean> mobs    = new Setting<>("Mobs", false);
    public final Setting<Float>   red     = new Setting<>("R", 1.0f).range(0, 1);
    public final Setting<Float>   green   = new Setting<>("G", 0.0f).range(0, 1);
    public final Setting<Float>   blue    = new Setting<>("B", 0.0f).range(0, 1);
    public final Setting<Float>   alpha   = new Setting<>("Alpha", 0.3f).range(0, 1);

    public ESP() {
        super("ESP", "Renders boxes around entities through walls", Category.RENDERER);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.world == null || mc.player == null) return;
        for (Entity e : mc.world.getAllEntities()) {
            if (e == mc.player) continue;
            if (e instanceof PlayerEntity && !players.getValue()) continue;
            if (!(e instanceof PlayerEntity) && e instanceof LivingEntity && !mobs.getValue()) continue;
            renderBox(e, event);
        }
    }

    private void renderBox(Entity e, RenderWorldLastEvent event) {
        double px = mc.getRenderManager().info.getProjectedView().x;
        double py = mc.getRenderManager().info.getProjectedView().y;
        double pz = mc.getRenderManager().info.getProjectedView().z;

        double x = e.lastTickPosX + (e.getPosX() - e.lastTickPosX) * event.getPartialTicks() - px;
        double y = e.lastTickPosY + (e.getPosY() - e.lastTickPosY) * event.getPartialTicks() - py;
        double z = e.lastTickPosZ + (e.getPosZ() - e.lastTickPosZ) * event.getPartialTicks() - pz;

        double w = e.getWidth() / 2.0 + 0.1;
        double h = e.getHeight() + 0.1;

        RenderSystem.pushMatrix();
        RenderSystem.translated(x, y, z);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        drawBoxLines(-w, 0, -w, w, h, w,
            red.getValue(), green.getValue(), blue.getValue(), alpha.getValue());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    private void drawBoxLines(double x1, double y1, double z1,
                               double x2, double y2, double z2,
                               float r, float g, float b, float a) {
        RenderSystem.lineWidth(1.5f);
        com.mojang.blaze3d.vertex.IVertexBuilder buf =
            net.minecraft.client.renderer.IRenderTypeBuffer.getImpl(
                com.mojang.blaze3d.vertex.BufferBuilder.class.cast(
                    net.minecraft.client.Minecraft.getInstance()
                        .getRenderTypeBuffers().getBuffer(RenderType.getLines())
                )
            ).getBuffer(RenderType.getLines());
        // Simple line box — 12 edges
        float[] verts = {
            (float)x1,(float)y1,(float)z1, (float)x2,(float)y1,(float)z1,
            (float)x2,(float)y1,(float)z1, (float)x2,(float)y1,(float)z2,
            (float)x2,(float)y1,(float)z2, (float)x1,(float)y1,(float)z2,
            (float)x1,(float)y1,(float)z2, (float)x1,(float)y1,(float)z1,
            (float)x1,(float)y2,(float)z1, (float)x2,(float)y2,(float)z1,
            (float)x2,(float)y2,(float)z1, (float)x2,(float)y2,(float)z2,
            (float)x2,(float)y2,(float)z2, (float)x1,(float)y2,(float)z2,
            (float)x1,(float)y2,(float)z2, (float)x1,(float)y2,(float)z1,
            (float)x1,(float)y1,(float)z1, (float)x1,(float)y2,(float)z1,
            (float)x2,(float)y1,(float)z1, (float)x2,(float)y2,(float)z1,
            (float)x2,(float)y1,(float)z2, (float)x2,(float)y2,(float)z2,
            (float)x1,(float)y1,(float)z2, (float)x1,(float)y2,(float)z2
        };
        for (int i = 0; i < verts.length; i += 3) {
            buf.pos(verts[i], verts[i+1], verts[i+2]).color(r, g, b, a).endVertex();
        }
    }
}
