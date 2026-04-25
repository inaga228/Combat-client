package com.example.combat.modules.building;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Tower — прыгает и ставит блок под ноги, строя башню вверх.
 * Порт из LiquidBounce под Fabric 1.16.5.
 */
public class TowerModule {

    public static boolean enabled = false;
    public static int     speed   = 2;   // блоков в секунду (1..5)
    public static boolean legitMovement = true; // ограниченный режим без "рывков"

    private static int tickCounter = 0;

    public static void tick(MinecraftClient mc) {
        if (!enabled || mc.player == null || mc.world == null) return;
        if (!mc.options.keySneak.isPressed()) return; // держи Shift для активации
        if (legitMovement && !mc.player.isOnGround()) return;

        int effectiveSpeed = legitMovement ? Math.min(speed, 2) : speed;
        int ticksPerBlock = Math.max(1, 20 / effectiveSpeed);
        tickCounter++;
        if (tickCounter < ticksPerBlock) return;
        tickCounter = 0;

        int blockSlot = findBlockSlot(mc);
        if (blockSlot < 0) return;

        // Нужно стоять на блоке
        BlockPos below = new BlockPos(mc.player.getX(), mc.player.getY() - 0.1, mc.player.getZ());
        if (mc.world.getBlockState(below).isAir()) return;

        // Размещаем блок под ноги (опора — блок ниже, грань — UP)
        int prevSlot = mc.player.inventory.selectedSlot;
        mc.player.inventory.selectedSlot = blockSlot;

        Vec3d hitVec = Vec3d.ofCenter(below).add(0, 0.5, 0);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, below, false);
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        // Прыгаем
        mc.player.jump();

        mc.player.inventory.selectedSlot = prevSlot;
    }

    private static int findBlockSlot(MinecraftClient mc) {
        ItemStack cur = mc.player.inventory.getStack(mc.player.inventory.selectedSlot);
        if (cur.getItem() instanceof BlockItem) return mc.player.inventory.selectedSlot;
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStack(i).getItem() instanceof BlockItem) return i;
        }
        return -1;
    }
}
