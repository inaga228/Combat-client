package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class KillAura extends Module {

    public enum TargetMode { PLAYERS, MOBS, ALL }

    public final Setting<Float>      range      = new Setting<>("Range",      3.0f).range(2.0, 6.0);
    public final Setting<TargetMode> targetMode = new Setting<>("Targets",    TargetMode.PLAYERS);
    public final Setting<Integer>    minCps     = new Setting<>("CPS Min",    8).range(1, 20);
    public final Setting<Integer>    maxCps     = new Setting<>("CPS Max",    12).range(1, 20);
    public final Setting<Boolean>    smoothRot  = new Setting<>("SmoothRot",  true);
    public final Setting<Float>      smoothSpd  = new Setting<>("SmoothSpd",  0.25f).range(0.05f, 1.0f);
    public final Setting<Boolean>    raytrace   = new Setting<>("Raytrace",   true);
    public final Setting<Boolean>    autoSwitch = new Setting<>("AutoSwitch", true);

    private static final Random random = new Random();
    private long  lastAttackMs     = 0;
    private long  lastSlotChangeMs = 0;
    private float targetYaw, targetPitch;

    public KillAura() {
        super("KillAura", "Automatically attacks nearby entities", Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.level == null) return;

        LivingEntity target = getBestTarget();
        if (target == null) return;

        // Автосвап оружия — не чаще раза в 2 секунды
        if (autoSwitch.getValue() && System.currentTimeMillis() - lastSlotChangeMs > 2000) {
            int best = getBestWeaponSlot();
            if (best != -1 && mc.player.inventory.selected != best) {
                mc.player.inventory.selected = best;
                lastSlotChangeMs = System.currentTimeMillis();
            }
        }

        // Расчёт углов + GCD коррекция
        calculateAngles(target);

        // Поворот
        if (smoothRot.getValue()) {
            float yawDiff   = MathHelper.wrapDegrees(targetYaw   - mc.player.yRot);
            float pitchDiff = MathHelper.wrapDegrees(targetPitch - mc.player.xRot);
            mc.player.yRot += yawDiff   * smoothSpd.getValue();
            mc.player.xRot += pitchDiff * smoothSpd.getValue();
            mc.player.xRot  = MathHelper.clamp(mc.player.xRot, -90f, 90f);
        } else {
            mc.player.yRot = targetYaw;
            mc.player.xRot = targetPitch;
        }

        // Рандомизированный CPS с джиттером
        long now = System.currentTimeMillis();
        int lo = Math.min(minCps.getValue(), maxCps.getValue());
        int hi = Math.max(minCps.getValue(), maxCps.getValue());
        int cps = lo + (lo == hi ? 0 : random.nextInt(hi - lo + 1));
        int delayMs = (int)(1000.0 / cps);
        delayMs += random.nextInt(Math.max(1, (int)(delayMs * 0.2f))) - (int)(delayMs * 0.1f);

        if (mc.player.getAttackStrengthScale(0) >= 0.98f && (now - lastAttackMs) >= delayMs) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(Hand.MAIN_HAND);
            lastAttackMs = now;
        }
    }

    private void calculateAngles(LivingEntity target) {
        Vector3d targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vector3d delta     = targetPos.subtract(mc.player.getEyePosition(1f));
        float yaw   = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float pitch = (float) -Math.toDegrees(
                Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        pitch = MathHelper.clamp(pitch, -90f, 90f);

        // GCD коррекция
        float div = 0.15f;
        yaw   = mc.player.yRot + (float)(Math.round((yaw   - mc.player.yRot) / div) * div);
        pitch = mc.player.xRot + (float)(Math.round((pitch - mc.player.xRot) / div) * div);

        targetYaw   = yaw;
        targetPitch = pitch;
    }

    private LivingEntity getBestTarget() {
        float r = range.getValue();
        AxisAlignedBB aabb = mc.player.getBoundingBox().inflate(r, r, r);
        Vector3d eyePos = mc.player.getEyePosition(1f);

        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(LivingEntity.class, aabb, e -> {
            if (e == mc.player) return false;
            if (!e.isAlive() || e.getHealth() <= 0) return false;
            if (mc.player.distanceTo(e) > r) return false;

            TargetMode mode = targetMode.getValue();
            if (mode == TargetMode.PLAYERS && !(e instanceof PlayerEntity)) return false;
            if (mode == TargetMode.MOBS    && !(e instanceof IMob))         return false;

            if (raytrace.getValue()) {
                float h = e.getBbHeight();
                Vector3d[] pts = {
                    e.position().add(0, h * 0.9, 0),
                    e.position().add(0, h * 0.5, 0),
                    e.position().add(0, h * 0.1, 0)
                };
                for (Vector3d pt : pts) {
                    RayTraceContext ctx = new RayTraceContext(
                            eyePos, pt,
                            RayTraceContext.BlockMode.COLLIDER,
                            RayTraceContext.FluidMode.NONE, mc.player);
                    if (mc.level.clip(ctx).getType() != RayTraceResult.Type.BLOCK)
                        return true;
                }
                return false;
            }
            return true;
        });

        return candidates.stream()
                .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
                .orElse(null);
    }

    private int getBestWeaponSlot() {
        int    bestSlot   = -1;
        double bestDamage = 0;
        for (int i = 0; i < 9; i++) {
            net.minecraft.item.ItemStack stack = mc.player.inventory.items.get(i);
            if (stack.getItem() instanceof net.minecraft.item.SwordItem) {
                double dmg = ((net.minecraft.item.SwordItem) stack.getItem()).getDamage();
                if (dmg > bestDamage) { bestDamage = dmg; bestSlot = i; }
            }
        }
        return bestSlot;
    }
}
