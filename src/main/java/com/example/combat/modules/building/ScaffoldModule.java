package com.example.combat.modules.building;

import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
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
    private static final Direction[] PLACE_DIRECTIONS = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN
    };

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

        // Ищем подходящий блок в хотбаре
        int blockSlot = findBlockSlot(mc);
        if (blockSlot < 0) return;

        // Предиктивная позиция (чуть вперед по скорости), чтобы мост строился стабильнее.
        BlockPos below = getPredictedBelowPos(mc);

        // Если блок уже есть — ничего не делаем
        if (!mc.world.getBlockState(below).isAir()) return;

        // Ищем опорный блок рядом
        for (Direction dir : PLACE_DIRECTIONS) {
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

    private static BlockPos getPredictedBelowPos(MinecraftClient mc) {
        Vec3d velocity = mc.player.getVelocity();
        double predictX = mc.player.getX() + velocity.x * 0.7;
        double predictZ = mc.player.getZ() + velocity.z * 0.7;

        BlockPos predicted = new BlockPos(predictX, mc.player.getY() - 1, predictZ);
        BlockPos current   = new BlockPos(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ());

        // Если предиктивная точка уже занята, работаем от текущей.
        return mc.world.getBlockState(predicted).isAir() ? predicted : current;
    }

    /** Ищет лучший слот с блоком в хотбаре */
    private static int findBlockSlot(MinecraftClient mc) {
        int selectedSlot = mc.player.inventory.selectedSlot;
        ItemStack current = mc.player.inventory.getStack(selectedSlot);
        if (isPlaceableScaffoldBlock(current, mc)) return selectedSlot;

        int bestSlot = -1;
        int bestCount = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStack(i);
            if (!isPlaceableScaffoldBlock(stack, mc)) continue;

            if (stack.getCount() > bestCount) {
                bestCount = stack.getCount();
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static boolean isPlaceableScaffoldBlock(ItemStack stack, MinecraftClient mc) {
        if (stack == null || !(stack.getItem() instanceof BlockItem)) return false;
        if (stack.getCount() <= 0) return false;

        BlockItem blockItem = (BlockItem) stack.getItem();
        if (blockItem.getBlock() instanceof FallingBlock) return false;

        BlockState defaultState = blockItem.getBlock().getDefaultState();
        return defaultState.isFullCube(mc.world, BlockPos.ORIGIN);
    }
}
