package com.example.combat.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssistModule {
    public static boolean enabled = false;
    public static double range = 4.0;      // 2.5..6.0
    public static int strength = 35;       // 5..90 (max deg/tick)
    public static int fov = 90;            // 30..180
    public static boolean requireClick = true;
    public static boolean playersOnly = true;

    public static void tick(MinecraftClient mc) {
        if (!enabled || mc == null || mc.player == null || mc.world == null) return;
        if (requireClick && !mc.options.keyAttack.isPressed()) return;

        LivingEntity target = findClosestTarget(mc);
        if (target == null) return;

        Vec3d eye = mc.player.getPos().add(0.0, mc.player.getStandingEyeHeight(), 0.0);
        Vec3d dst = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.9, 0.0).subtract(eye);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dst.z, dst.x)) - 90.0);
        float diff = MathHelper.wrapDegrees(targetYaw - mc.player.yaw);
        if (Math.abs(diff) > fov) return;
        float step = MathHelper.clamp(diff, -strength, strength);
        mc.player.yaw += step;
    }

    private static LivingEntity findClosestTarget(MinecraftClient mc) {
        LivingEntity best = null;
        double bestDist = range * range;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity)) continue;
            if (e == mc.player || !e.isAlive()) continue;
            if (playersOnly && !(e instanceof PlayerEntity)) continue;

            double dist = mc.player.squaredDistanceTo(e);
            if (dist < bestDist) {
                bestDist = dist;
                best = (LivingEntity) e;
            }
        }
        return best;
    }
}
