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
    public enum AimMode    { CAMERA, BODY, HYBRID }
    public enum CritMode   { OFF, JUMP }

    public final Setting<Float>      range      = new Setting<>("Range",      3.5f).range(2.0, 6.0);
    public final Setting<TargetMode> targetMode = new Setting<>("Targets",    TargetMode.PLAYERS);
    public final Setting<AimMode>    aimMode    = new Setting<>("AimMode",    AimMode.CAMERA);
    public final Setting<Integer>    minCps     = new Setting<>("CPS Min",    8).range(1, 20);
    public final Setting<Integer>    maxCps     = new Setting<>("CPS Max",    12).range(1, 20);
    public final Setting<Boolean>    smoothRot  = new Setting<>("SmoothRot",  true);
    public final Setting<Float>      smoothSpd  = new Setting<>("SmoothSpd",  0.18f).range(0.05f, 1.0f);
    public final Setting<Boolean>    raytrace   = new Setting<>("Raytrace",   true);
    public final Setting<Boolean>    autoSwitch = new Setting<>("AutoSwitch", true);
    public final Setting<CritMode>   critMode   = new Setting<>("Crits",      CritMode.OFF);

    private static final Random RAND = new Random();

    private long    lastAttackMs     = 0;
    private long    lastSlotMs       = 0;
    private long    lastJumpMs       = 0;
    private boolean jumpingForCrit   = false;

    // Плавный прицел с интерполяцией
    private float currentYaw   = 0;
    private float currentPitch = 0;
    private boolean firstTarget = true;

    public KillAura() {
        super("KillAura", "Automatically attacks nearby entities", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        firstTarget = true;
        jumpingForCrit = false;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.level == null) return;

        if (critMode.getValue() == CritMode.JUMP) handleCritJump();

        LivingEntity target = getBestTarget();
        if (target == null) {
            firstTarget = true;
            return;
        }

        // Автосвап
        if (autoSwitch.getValue() && System.currentTimeMillis() - lastSlotMs > 2000) {
            int best = getBestWeaponSlot();
            if (best != -1 && mc.player.inventory.selected != best) {
                mc.player.inventory.selected = best;
                lastSlotMs = System.currentTimeMillis();
            }
        }

        // Целевые углы
        float[] angles = calcAngles(target);
        float tYaw   = angles[0];
        float tPitch = angles[1];

        // Инициализация при новом таргете
        if (firstTarget) {
            currentYaw   = mc.player.yRot;
            currentPitch = mc.player.xRot;
            firstTarget  = false;
        }

        // Плавная интерполяция с эффектом замедления (ease-out)
        if (smoothRot.getValue()) {
            float spd = smoothSpd.getValue();
            float yawDiff   = MathHelper.wrapDegrees(tYaw   - currentYaw);
            float pitchDiff = MathHelper.wrapDegrees(tPitch - currentPitch);

            // Ease-out: скорость пропорциональна расстоянию, но не меньше мин. шага
            float yawStep   = yawDiff   * spd + (yawDiff   > 0 ? 0.3f : -0.3f) * (1f - spd);
            float pitchStep = pitchDiff * spd + (pitchDiff > 0 ? 0.3f : -0.3f) * (1f - spd);

            // Микро-рандомизация — «человеческий» тремор
            yawStep   += (RAND.nextFloat() - 0.5f) * 0.05f;
            pitchStep += (RAND.nextFloat() - 0.5f) * 0.03f;

            currentYaw   += yawStep;
            currentPitch += pitchStep;
            currentPitch  = MathHelper.clamp(currentPitch, -90f, 90f);
        } else {
            // Без сглаживания — мгновенный поворот
            currentYaw   = tYaw;
            currentPitch = tPitch;
        }

        // GCD коррекция (анти-анализ движения мыши)
        float div = 0.15f;
        float gcdYaw   = mc.player.yRot + (float)(Math.round((currentYaw   - mc.player.yRot)   / div) * div);
        float gcdPitch = mc.player.xRot + (float)(Math.round((currentPitch - mc.player.xRot)   / div) * div);

        switch (aimMode.getValue()) {
            case CAMERA:
                mc.player.yRot = gcdYaw;
                mc.player.xRot = gcdPitch;
                break;
            case BODY:
                mc.player.yBodyRot = gcdYaw;
                mc.player.yHeadRot = gcdYaw;
                break;
            case HYBRID:
                mc.player.yRot     = gcdYaw;
                mc.player.xRot     = gcdPitch;
                mc.player.yBodyRot = gcdYaw;
                mc.player.yHeadRot = gcdYaw;
                break;
        }

        // CPS с рандомизацией
        int lo  = Math.min(minCps.getValue(), maxCps.getValue());
        int hi  = Math.max(minCps.getValue(), maxCps.getValue());
        int cps = lo + (lo == hi ? 0 : RAND.nextInt(hi - lo + 1));
        int delayMs = (int)(1000.0 / cps);
        // ±10% случайный джиттер
        delayMs += (int)((RAND.nextFloat() - 0.5f) * delayMs * 0.20f);

        long now      = System.currentTimeMillis();
        boolean canHit = mc.player.getAttackStrengthScale(0) >= 0.98f;
        boolean critOk = (critMode.getValue() != CritMode.JUMP)
                       || !jumpingForCrit
                       || mc.player.fallDistance > 0.05f;

        if (canHit && critOk && (now - lastAttackMs) >= delayMs) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(Hand.MAIN_HAND);
            lastAttackMs = now;
            if (critMode.getValue() == CritMode.JUMP) jumpingForCrit = false;
        }
    }

    // ── Крит-прыжок ──────────────────────────────────────────────
    private void handleCritJump() {
        if (mc.player.isOnGround() && !mc.player.isCrouching()
                && !mc.player.isInWater() && !mc.player.onClimbable()) {
            long now = System.currentTimeMillis();
            if (now - lastJumpMs > 500 && !jumpingForCrit) {
                mc.player.jump();
                lastJumpMs = now;
                jumpingForCrit = true;
            }
        }
    }

    // ── Углы на цель ──────────────────────────────────────────────
    private float[] calcAngles(LivingEntity e) {
        Vector3d pos   = e.position().add(0, e.getBbHeight() * 0.5, 0);
        Vector3d delta = pos.subtract(mc.player.getEyePosition(1f));
        float yaw   = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float pitch = (float) -Math.toDegrees(
                Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        return new float[]{ yaw, MathHelper.clamp(pitch, -90f, 90f) };
    }

    // ── Выбор цели ───────────────────────────────────────────────
    private LivingEntity getBestTarget() {
        float r = range.getValue();
        AxisAlignedBB box = mc.player.getBoundingBox().inflate(r, r, r);
        Vector3d eye = mc.player.getEyePosition(1f);

        List<LivingEntity> candidates = mc.level.getEntitiesOfClass(LivingEntity.class, box, e -> {
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
                    RayTraceContext ctx = new RayTraceContext(eye, pt,
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

    // ── Лучшее оружие в хотбаре ──────────────────────────────────
    private int getBestWeaponSlot() {
        int best = -1; double dmg = 0;
        for (int i = 0; i < 9; i++) {
            net.minecraft.item.ItemStack s = mc.player.inventory.items.get(i);
            if (s.getItem() instanceof net.minecraft.item.SwordItem) {
                double d = ((net.minecraft.item.SwordItem) s.getItem()).getDamage();
                if (d > dmg) { dmg = d; best = i; }
            }
        }
        return best;
    }
}
