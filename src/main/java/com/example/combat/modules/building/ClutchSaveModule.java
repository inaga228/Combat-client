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
 * Отдельный модуль спасения при падении/сталкивании (clutch).
 */
public class ClutchSaveModule {
    public static boolean enabled = false;
    public static int turnSpeed = 85; // 25..120 deg/tick
    public static double fallSpeed = 0.12; // trigger when vy < -fallSpeed

    private static final Direction[] DIRS = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public static void tick(MinecraftClient mc) {
        if (!enabled || mc == null || mc.player == null || mc.world == null) return;
        if (!shouldActivate(mc)) return;

        int slot = findBlockSlot(mc);
        if (slot < 0) return;

        BlockPos placePos = findBestBelow(mc);
        if (!mc.world.getBlockState(placePos).isAir()) return;

        PlacementTarget target = findTarget(mc, placePos);
        if (target == null) return;

        int prevSlot = mc.player.inventory.selectedSlot;
        float prevYaw = mc.player.yaw;
        float prevPitch = mc.player.pitch;
        setHotbarSlot(mc, slot);

        float[] ang = calcAngles(mc, target.hitVec);
        mc.player.yaw = step(prevYaw, ang[0], turnSpeed);
        mc.player.pitch = step(prevPitch, ang[1], turnSpeed);

        BlockHitResult hit = new BlockHitResult(target.hitVec, target.face, target.neighbor, false);
        ActionResult result = mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, hit);
        if (result.isAccepted()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        setHotbarSlot(mc, prevSlot);
    }

    private static boolean shouldActivate(MinecraftClient mc) {
        if (mc.player.isOnGround()) return false;
        BlockPos below = new BlockPos(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
        if (!mc.world.getBlockState(below).isAir()) return false;

        Vec3d v = mc.player.getVelocity();
        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
        return v.y <= -Math.abs(fallSpeed) || horizontal > 0.17;
    }

    private static BlockPos findBestBelow(MinecraftClient mc) {
        double px = mc.player.getX();
        double pz = mc.player.getZ();
        int y = (int) Math.floor(mc.player.getY() - 1.0);
        BlockPos[] candidates = new BlockPos[]{
            new BlockPos(px, y, pz),
            new BlockPos(px + 0.65, y, pz), new BlockPos(px - 0.65, y, pz),
            new BlockPos(px, y, pz + 0.65), new BlockPos(px, y, pz - 0.65),
            new BlockPos(px + 0.65, y, pz + 0.65), new BlockPos(px + 0.65, y, pz - 0.65),
            new BlockPos(px - 0.65, y, pz + 0.65), new BlockPos(px - 0.65, y, pz - 0.65)
        };

        BlockPos best = candidates[0];
        double bestScore = Double.MAX_VALUE;
        for (BlockPos c : candidates) {
            if (!mc.world.getBlockState(c).isAir()) continue;
            PlacementTarget t = findTarget(mc, c);
            if (t == null) continue;
            float[] ang = calcAngles(mc, t.hitVec);
            float yawDiff = Math.abs(MathHelper.wrapDegrees(ang[0] - mc.player.yaw));
            double score = cornerDistanceSq(px, pz, c) + yawDiff * 0.02;
            if (score < bestScore) { bestScore = score; best = c; }
        }
        return best;
    }

    private static PlacementTarget findTarget(MinecraftClient mc, BlockPos below) {
        for (Direction dir : DIRS) {
            BlockPos neighbor = below.offset(dir);
            BlockState state = mc.world.getBlockState(neighbor);
            if (state.isAir() || !state.getMaterial().isSolid()) continue;
            Direction face = dir.getOpposite();
            Vec3d hitVec = new Vec3d(neighbor.getX() + 0.5, neighbor.getY() + 0.5, neighbor.getZ() + 0.5)
                .add(new Vec3d(face.getOffsetX(), face.getOffsetY(), face.getOffsetZ()).multiply(0.40));
            Vec3d eye = mc.player.getPos().add(0, mc.player.getStandingEyeHeight(), 0);
            if (eye.distanceTo(hitVec) > 4.25) continue;
            return new PlacementTarget(neighbor, face, hitVec);
        }
        return null;
    }

    private static float[] calcAngles(MinecraftClient mc, Vec3d hitVec) {
        Vec3d eye = mc.player.getPos().add(0, mc.player.getStandingEyeHeight(), 0);
        Vec3d d = hitVec.subtract(eye);
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, h));
        return new float[]{yaw, MathHelper.clamp(pitch, -89f, 89f)};
    }

    private static float step(float from, float to, float maxStep) {
        float diff = MathHelper.wrapDegrees(to - from);
        diff = MathHelper.clamp(diff, -maxStep, maxStep);
        return from + diff;
    }

    private static int findBlockSlot(MinecraftClient mc) {
        int sel = mc.player.inventory.selectedSlot;
        if (isGoodBlock(mc.player.inventory.getStack(sel), mc)) return sel;
        int best = -1, cnt = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.inventory.getStack(i);
            if (!isGoodBlock(s, mc)) continue;
            if (s.getCount() > cnt) { cnt = s.getCount(); best = i; }
        }
        return best;
    }

    private static void setHotbarSlot(MinecraftClient mc, int slot) {
        if (mc.player.inventory.selectedSlot == slot) return;
        mc.player.inventory.selectedSlot = slot;
        if (mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private static boolean isGoodBlock(ItemStack stack, MinecraftClient mc) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem)) return false;
        BlockItem bi = (BlockItem) stack.getItem();
        if (bi.getBlock() instanceof FallingBlock) return false;
        return bi.getBlock().getDefaultState().isFullCube(mc.world, BlockPos.ORIGIN);
    }

    private static double cornerDistanceSq(double px, double pz, BlockPos pos) {
        double x0 = pos.getX();
        double z0 = pos.getZ();
        double best = Double.MAX_VALUE;
        double[] xs = {x0, x0 + 1.0};
        double[] zs = {z0, z0 + 1.0};
        for (double x : xs) for (double z : zs) {
            double dx = px - x, dz = pz - z;
            best = Math.min(best, dx * dx + dz * dz);
        }
        return best;
    }

    private static class PlacementTarget {
        final BlockPos neighbor;
        final Direction face;
        final Vec3d hitVec;
        PlacementTarget(BlockPos n, Direction f, Vec3d h) { neighbor = n; face = f; hitVec = h; }
    }
}
