package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.item.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Trajectories extends Module {

    public final Setting<Integer> steps   = new Setting<>("Steps", 60).range(10, 200);
    public final Setting<Float>   gravity = new Setting<>("Gravity", 0.03f).range(0.01f, 0.1f);

    public Trajectories() {
        super("Trajectories", "Shows projectile trajectory preview", Category.RENDERER);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.player == null) return;
        ItemStack held = mc.player.getHeldItemMainhand();
        Item item = held.getItem();
        if (!(item instanceof BowItem) && !(item instanceof CrossbowItem)
            && !(item instanceof SnowballItem) && !(item instanceof EggItem)
            && !(item instanceof ThrowablePotionItem)) return;

        Vector3d pos   = mc.player.getEyePosition(event.getPartialTicks());
        Vector3d look  = mc.player.getLookVec();
        float    speed = 1.5f;
        double   grav  = gravity.getValue();

        double vx = look.x * speed;
        double vy = look.y * speed;
        double vz = look.z * speed;

        double cx = pos.x, cy = pos.y, cz = pos.z;

        double px = mc.getRenderManager().info.getProjectedView().x;
        double py = mc.getRenderManager().info.getProjectedView().y;
        double pz = mc.getRenderManager().info.getProjectedView().z;

        RenderSystem.pushMatrix();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2f);
        RenderSystem.color4f(1f, 1f, 0f, 1f);

        com.mojang.blaze3d.vertex.BufferBuilder buf =
            net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR == null
            ? null : new com.mojang.blaze3d.vertex.BufferBuilder(512);

        for (int i = 0; i < steps.getValue(); i++) {
            double nx = cx + vx;
            double ny = cy + vy;
            double nz = cz + vz;
            vy -= grav;
            vx *= 0.99; vy *= 0.99; vz *= 0.99;

            // draw segment from (cx,cy,cz) to (nx,ny,nz)
            cx = nx; cy = ny; cz = nz;
        }

        RenderSystem.enableDepthTest();
        RenderSystem.popMatrix();
    }
}
