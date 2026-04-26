package com.example.combat.modules.building;

import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * ScaffoldModule — Fabric 1.16.5
 *
 * Почему блоки исчезали раньше:
 *  1. hitVec = center + normal*0.5 — это ровно на краю AABB опорного блока.
 *     Сервер иногда считает это невалидным и отклоняет постановку.
 *  2. ClientPlayerInteractionManager.interactBlock() делает внутренний
 *     raycast от глаз игрока. Если yaw/pitch не смотрят на hitVec —
 *     возвращает ActionResult.PASS и блок не ставится.
 *  3. Предикция позиции (velocity * factor) давала рассинхрон с сервером.
 *
 * Решение:
 *  - hitVec строго внутри грани (normal * 0.45)
 *  - Перед interactBlock выставляем yaw/pitch точно на hitVec,
 *    после — мгновенно возвращаем. Камера не дёргается (1 тик).
 *  - Позиция строго текущая, без предикции.
 */
public class ScaffoldModule {

    // ── Публичные настройки ──────────────────────────────────────────────────
    public static boolean enabled      = false;
    public static int     delay        = 0;      // 0..10 тиков
    public static boolean safeWalk     = true;
    public static boolean onlyOnGround = true;
    public static int     mode         = 0;      // 0=Normal 1=Bridge 2=Eagle
    public static boolean autoJump     = false;
    public static boolean eagleSneak   = true;
    public static boolean clutchRescue = true;
    public static int     clutchTurnSpeed = 70; // 25..120 deg/tick
    public static double  clutchFallSpeed = 0.12; // срабатывание при vy < -value

    // ── Внутреннее ──────────────────────────────────────────────────────────
    private static int ticksSinceLast = 0;

    // Порядок важен: сначала DOWN — самый надёжный опорный блок
    private static final Direction[] DIRS = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    // ════════════════════════════════════════════════════════════════════════
    public static void tick(MinecraftClient mc) {
        if (!enabled || mc.player == null || mc.world == null) return;
        boolean clutchActive = clutchRescue && shouldClutch(mc);

        // Только на земле
        if (onlyOnGround && !mc.player.isOnGround() && !clutchActive) return;

        // SafeWalk
        if (safeWalk) mc.player.setSprinting(false);

        // Задержка
        if (!clutchActive && ticksSinceLast < delay) { ticksSinceLast++; return; }

        // Слот с блоком
        int blockSlot = findBlockSlot(mc);
        if (blockSlot < 0) return;

        // Позиция под ногами
        double px = mc.player.getX();
        double pz = mc.player.getZ();
        double py = mc.player.getY();

        // Eagle: приседать у края
        if (mode == 2) {
            BlockPos cur = new BlockPos(px, py - 1.0, pz);
            if (eagleSneak) {
                mc.options.keySneak.setPressed(mc.world.getBlockState(cur).isAir());
            }
        }

        // Bridge: смещаемся на 0.5 в направлении движения (а не только "назад")
        if (mode == 1) {
            float yaw = mc.player.yaw;
            boolean forward = mc.options.keyForward.isPressed();
            boolean back = mc.options.keyBack.isPressed();
            boolean left = mc.options.keyLeft.isPressed();
            boolean right = mc.options.keyRight.isPressed();

            if (!forward && !back && !left && !right) {
                // Если игрок стоит, используем взгляд как дефолт.
                float yawRad = (float) Math.toRadians(yaw);
                px += Math.sin(yawRad) * 0.5;
                pz -= Math.cos(yawRad) * 0.5;
            } else {
                if (back) yaw += 180f;
                if (left) yaw += back ? -45f : (forward ? -45f : -90f);
                if (right) yaw += back ? 45f : (forward ? 45f : 90f);

                float yawRad = (float) Math.toRadians(yaw);
                px += Math.sin(yawRad) * 0.5;
                pz -= Math.cos(yawRad) * 0.5;
            }
        }

        BlockPos below = clutchActive ? findClutchBelowPos(mc) : new BlockPos(px, py - 1.0, pz);

        // Уже заполнено
        if (!mc.world.getBlockState(below).isAir()) return;

        // Найти точку постановки
        PlacementTarget target = findTarget(mc, below);
        if (target == null) return;

        // Меняем слот, сохраняем старый
        int prevSlot = mc.player.inventory.selectedSlot;
        setHotbarSlot(mc, blockSlot);

        // Вычисляем углы на hitVec и временно выставляем
        // (interactBlock делает raycast, углы должны совпадать)
        float prevYaw   = mc.player.yaw;
        float prevPitch = mc.player.pitch;
        float[] angles  = calcAngles(mc, target.hitVec);
        if (clutchActive) {
            mc.player.yaw = rotateStep(prevYaw, angles[0], clutchTurnSpeed);
            mc.player.pitch = rotateStep(prevPitch, angles[1], clutchTurnSpeed);
        } else {
            mc.player.yaw   = angles[0];
            mc.player.pitch = angles[1];
        }

        // Постановка
        BlockHitResult hit = new BlockHitResult(
            target.hitVec, target.face, target.neighbor, false
        );
        ActionResult result = mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, hit);
        if (result.isAccepted()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        // Для обычного Scaffold возвращаем углы, для clutch — оставляем быстрый довод камеры.
        if (!clutchActive) {
            mc.player.yaw   = prevYaw;
            mc.player.pitch = prevPitch;
        }
        setHotbarSlot(mc, prevSlot);

        ticksSinceLast = 0;

        if (autoJump && mc.player.isOnGround() && mc.options.keyForward.isPressed()) {
            mc.player.jump();
        }
    }

    private static boolean shouldClutch(MinecraftClient mc) {
        if (mc.player.isOnGround()) return false;
        if (mc.player.getVelocity().y > -Math.abs(clutchFallSpeed)) return false;

        BlockPos below = new BlockPos(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
        return mc.world.getBlockState(below).isAir();
    }

    private static BlockPos findClutchBelowPos(MinecraftClient mc) {
        double px = mc.player.getX();
        double pz = mc.player.getZ();
        double py = mc.player.getY();
        int y = (int) Math.floor(py - 1.0);

        BlockPos[] candidates = new BlockPos[] {
            new BlockPos(px, y, pz),
            new BlockPos(px + 0.65, y, pz),
            new BlockPos(px - 0.65, y, pz),
            new BlockPos(px, y, pz + 0.65),
            new BlockPos(px, y, pz - 0.65),
            new BlockPos(px + 0.65, y, pz + 0.65),
            new BlockPos(px + 0.65, y, pz - 0.65),
            new BlockPos(px - 0.65, y, pz + 0.65),
            new BlockPos(px - 0.65, y, pz - 0.65)
        };

        BlockPos best = candidates[0];
        double bestScore = Double.MAX_VALUE;
        for (BlockPos c : candidates) {
            if (!mc.world.getBlockState(c).isAir()) continue;
            if (findTarget(mc, c) == null) continue;

            double score = cornerDistanceSq(px, pz, c);
            if (score < bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    private static double cornerDistanceSq(double px, double pz, BlockPos pos) {
        double x0 = pos.getX();
        double z0 = pos.getZ();
        double[] xs = {x0, x0 + 1.0};
        double[] zs = {z0, z0 + 1.0};
        double best = Double.MAX_VALUE;
        for (double cx : xs) {
            for (double cz : zs) {
                double dx = px - cx;
                double dz = pz - cz;
                double d = dx * dx + dz * dz;
                if (d < best) best = d;
            }
        }
        return best;
    }

    private static float rotateStep(float from, float to, float maxStep) {
        float diff = MathHelper.wrapDegrees(to - from);
        diff = MathHelper.clamp(diff, -maxStep, maxStep);
        return from + diff;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ПОИСК ОПОРНОГО БЛОКА
    // ════════════════════════════════════════════════════════════════════════
    private static PlacementTarget findTarget(MinecraftClient mc, BlockPos below) {
        PlacementTarget best = null;
        double bestScore = Double.MAX_VALUE;

        for (Direction dir : DIRS) {
            BlockPos neighbor = below.offset(dir);
            BlockState nState = mc.world.getBlockState(neighbor);

            if (nState.isAir() || !nState.getMaterial().isSolid()) continue;

            Direction face = dir.getOpposite();

            // 0.40 вместо 0.5 — глубже внутри грани, меньше отклонений античитом
            Vec3d faceNormal = new Vec3d(face.getOffsetX(), face.getOffsetY(), face.getOffsetZ());
            Vec3d neighborCenter = new Vec3d(
                neighbor.getX() + 0.5,
                neighbor.getY() + 0.5,
                neighbor.getZ() + 0.5
            );
            Vec3d hitVec = neighborCenter.add(faceNormal.multiply(0.40));

            // Консервативный reach для античита
            Vec3d eye = mc.player.getPos().add(0, mc.player.getStandingEyeHeight(), 0);
            if (eye.distanceTo(hitVec) > 4.25) continue;
            float[] ang = calcAngles(mc, hitVec);
            float yawDiff = Math.abs(MathHelper.wrapDegrees(ang[0] - mc.player.yaw));
            double score = yawDiff * 0.03 + eye.distanceTo(hitVec);
            if (dir == Direction.DOWN) score -= 0.18;

            if (score < bestScore) {
                bestScore = score;
                best = new PlacementTarget(neighbor, face, hitVec);
            }
        }
        return best;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ВЫЧИСЛЕНИЕ УГЛОВ
    // ════════════════════════════════════════════════════════════════════════
    private static float[] calcAngles(MinecraftClient mc, Vec3d hitVec) {
        Vec3d eye = mc.player.getPos().add(0, mc.player.getStandingEyeHeight(), 0);
        Vec3d d   = hitVec.subtract(eye);
        double h  = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw   = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, h));
        return new float[]{ yaw, MathHelper.clamp(pitch, -89f, 89f) };
    }

    private static void setHotbarSlot(MinecraftClient mc, int slot) {
        if (mc.player.inventory.selectedSlot == slot) return;
        mc.player.inventory.selectedSlot = slot;
        if (mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ПОИСК СЛОТА
    // ════════════════════════════════════════════════════════════════════════
    private static int findBlockSlot(MinecraftClient mc) {
        int sel = mc.player.inventory.selectedSlot;
        if (isGoodBlock(mc.player.inventory.getStack(sel), mc)) return sel;
        int best = -1, bestCount = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.inventory.getStack(i);
            if (!isGoodBlock(s, mc)) continue;
            if (s.getCount() > bestCount) { bestCount = s.getCount(); best = i; }
        }
        return best;
    }

    private static boolean isGoodBlock(ItemStack stack, MinecraftClient mc) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem)) return false;
        BlockItem bi = (BlockItem) stack.getItem();
        if (bi.getBlock() instanceof FallingBlock) return false;
        return bi.getBlock().getDefaultState().isFullCube(mc.world, BlockPos.ORIGIN);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GUI-хелперы
    // ════════════════════════════════════════════════════════════════════════
    public static String getModeName() {
        switch (mode) {
            case 1: return "Bridge";
            case 2: return "Eagle";
            default: return "Normal";
        }
    }

    public static void cycleMode(int delta) {
        mode = (mode + (delta >= 0 ? 1 : 2)) % 3;
    }

    private static class PlacementTarget {
        final BlockPos  neighbor;
        final Direction face;
        final Vec3d     hitVec;
        PlacementTarget(BlockPos neighbor, Direction face, Vec3d hitVec) {
            this.neighbor = neighbor;
            this.face     = face;
            this.hitVec   = hitVec;
        }
    }
}
