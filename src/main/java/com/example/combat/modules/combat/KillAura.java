package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {

    public enum TargetMode { CLOSEST, ANGLE, HEALTH }

    public final Setting<Float>      range      = new Setting<>("Range", 4.0f).range(2.0, 6.0);
    public final Setting<Boolean>    players    = new Setting<>("Players", true);
    public final Setting<Boolean>    mobs       = new Setting<>("Mobs", true);
    public final Setting<TargetMode> targetMode = new Setting<>("Target", TargetMode.CLOSEST);
    public final Setting<Boolean>    rotate     = new Setting<>("Rotate", true);
    public final Setting<Integer>    cps        = new Setting<>("CPS", 12).range(1, 20);

    private long lastAttack = 0;

    public KillAura() {
        super("KillAura", "Automatically attacks nearby entities", Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();
        long delay = 1000L / cps.getValue();
        if (now - lastAttack < delay) return;

        Entity target = getTarget();
        if (target == null) return;

        if (rotate.getValue()) rotateToEntity(target);
        mc.playerController.attackEntity(mc.player, target);
        mc.player.swingArm(Hand.MAIN_HAND);
        lastAttack = now;
    }

    private Entity getTarget() {
        List<Entity> candidates = new ArrayList<>();
        float r = range.getValue();
        for (Entity e : mc.world.getAllEntities()) {
            if (e == mc.player) continue;
            if (mc.player.getDistanceSq(e) > r * r) continue;
            if (e instanceof PlayerEntity && players.getValue()) candidates.add(e);
            else if (e instanceof LivingEntity && mobs.getValue() && !(e instanceof PlayerEntity)) candidates.add(e);
        }
        if (candidates.isEmpty()) return null;

        switch (targetMode.getValue()) {
            case CLOSEST:
                candidates.sort(Comparator.comparingDouble(e -> mc.player.getDistanceSq(e)));
                break;
            case HEALTH:
                candidates.sort(Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth()));
                break;
            default:
                candidates.sort(Comparator.comparingDouble(e -> angleTo(e)));
                break;
        }
        return candidates.get(0);
    }

    private double angleTo(Entity e) {
        Vector3d diff = e.getPositionVec().subtract(mc.player.getPositionVec());
        double yaw = Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90;
        double angle = Math.abs(yaw - mc.player.rotationYaw) % 360;
        if (angle > 180) angle = 360 - angle;
        return angle;
    }

    private void rotateToEntity(Entity e) {
        Vector3d diff = e.getPositionVec().add(0, e.getEyeHeight(), 0)
                .subtract(mc.player.getEyePosition(1f));
        double dist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        mc.player.rotationYaw   = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f;
        mc.player.rotationPitch = (float) -Math.toDegrees(Math.atan2(diff.y, dist));
    }
}
