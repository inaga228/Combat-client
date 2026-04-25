package com.example.combat.modules.building;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Scaffold — автоматически кладёт блоки под ноги при ходьбе.
 * Порт из LiquidBounce под Fabric 1.16.5.
 */
public class ScaffoldModule {

    public static boolean enabled  = false;
    public static int     delay    = 0;     // задержка в тиках между блоками
    public static boolean safeWalk = true;  // снижение скорости на краях
    public static boolean legitMovement = true; // только "ванильный" режим без резких движений

    private static int ticksSinceLast = 0;

    public static void tick(MinecraftClient mc) {
        if (!enabled || mc.player == null || mc.world == null) return;
        if (legitMovement && !mc.player.isOnGround()) return;

        // SafeWalk — не падать с краёв
        if (safeWalk) {
            mc.player.setSprinting(false);
        }

        // Задержка
        if (ticksSinceLast < delay) {
            ticksSinceLast++;
            return;
        }

        // Ищем блок в руке
        int blockSlot = findBlockSlot(mc);
        if (blockSlot < 0) return;

        // Позиция под игроком
        BlockPos below = new BlockPos(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ());

        // Если блок уже есть — ничего не делаем
        if (!mc.world.getBlockState(below).isAir()) return;

        // Ищем опорный блок рядом
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue;
            BlockPos neighbor = below.offset(dir);
            BlockState state  = mc.world.getBlockState(neighbor);
            if (state.isAir()) continue;

            // Меняем слот
            int prevSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = blockSlot;

            // Размещаем
            Direction opposite = dir.getOpposite();
            Vec3d hitVec = Vec3d.ofCenter(neighbor).add(
                Vec3d.of(opposite.getVector()).multiply(0.5)
            );
            BlockHitResult hit = new BlockHitResult(hitVec, opposite, neighbor, false);
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, hit);
            mc.player.swingHand(Hand.MAIN_HAND);

            mc.player.inventory.selectedSlot = prevSlot;
            ticksSinceLast = 0;
            return;
        }
    }

    /** Ищет ближайший слот с блоком в хотбаре */
    private static int findBlockSlot(MinecraftClient mc) {
        // Сначала текущий
        ItemStack cur = mc.player.inventory.getStack(mc.player.inventory.selectedSlot);
        if (cur.getItem() instanceof BlockItem) return mc.player.inventory.selectedSlot;

        // Затем весь хотбар
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStack(i).getItem() instanceof BlockItem) return i;
        }
        return -1;
    }
}
