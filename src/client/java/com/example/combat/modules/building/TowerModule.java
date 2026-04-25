package com.example.combat.modules.building;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Tower — быстро строит башню вверх при зажатом прыжке.
 * Логика из LiquidBounce ScaffoldTowerMotion.
 */
public class TowerModule {

    public static boolean enabled = false;

    // Настройки
    public static float motion      = 0.42f;  // вертикальный импульс (как в LB)
    public static float triggerH    = 0.78f;  // высота подъёма до укладки блока
    public static float slowXZ      = 0.6f;   // замедление по X/Z во время башни

    private static double jumpOffY  = Double.NaN;

    public static void tick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        ClientPlayerEntity player = mc.player;
        boolean jumpPressed = mc.options.jumpKey.isPressed();

        if (!jumpPressed) { jumpOffY = Double.NaN; return; }
        if (ScaffoldModule.findBlockSlot(player) < 0) { jumpOffY = Double.NaN; return; }

        // Есть ли твёрдый блок под ногами
        BlockPos below = player.getBlockPos().down();
        boolean blockBelow = !mc.world.getBlockState(below).isAir();
        if (!blockBelow) { jumpOffY = Double.NaN; return; }

        // Запомнить позицию прыжка
        if (Double.isNaN(jumpOffY)) {
            jumpOffY = player.getY();
            return;
        }

        // Когда поднялись достаточно — укладываем блок и добавляем импульс
        if (player.getY() > jumpOffY + triggerH) {
            placeBlockBelow(mc, player);
            var vel = player.getVelocity();
            player.setVelocity(vel.x * slowXZ, motion, vel.z * slowXZ);
            jumpOffY = player.getY();
        }
    }

    private static void placeBlockBelow(MinecraftClient mc, ClientPlayerEntity player) {
        int blockSlot = ScaffoldModule.findBlockSlot(player);
        if (blockSlot < 0) return;

        BlockPos targetPos = player.getBlockPos().down();
        BlockPos supportPos = null;
        Direction supportFace = Direction.UP;

        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH,
                                             Direction.EAST,  Direction.WEST}) {
            BlockPos nb = targetPos.offset(dir);
            if (!mc.world.getBlockState(nb).isAir()) {
                supportPos = nb;
                supportFace = dir.getOpposite();
                break;
            }
        }
        if (supportPos == null) return;

        int prevSlot = player.getInventory().selectedSlot;
        if (blockSlot != prevSlot) player.getInventory().selectedSlot = blockSlot;

        Vec3d hitVec = Vec3d.ofCenter(supportPos)
                .add(Vec3d.of(supportFace.getVector()).multiply(0.5));
        BlockHitResult hit = new BlockHitResult(hitVec, supportFace, supportPos, false);

        mc.getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(player.getYaw(), 90.0f, player.isOnGround())
        );

        var result = mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        if (result.isAccepted()) player.swingHand(Hand.MAIN_HAND);

        if (blockSlot != prevSlot) player.getInventory().selectedSlot = prevSlot;
        mc.getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(player.getYaw(), player.getPitch(), player.isOnGround())
        );
    }
}
