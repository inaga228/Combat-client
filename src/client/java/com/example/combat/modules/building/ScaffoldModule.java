package com.example.combat.modules.building;

import net.minecraft.block.FallingBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Scaffold — кладёт блоки под игрока при движении.
 * Портировано с логики LiquidBounce ModuleScaffold.
 */
public class ScaffoldModule {

    public static boolean enabled = false;

    // Настройки
    public static int  delayTicks  = 0; // задержка между блоками (тиков)
    public static boolean safeWalk = true; // не упасть с края

    private static int delayCounter = 0;

    public static void tick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        ClientPlayerEntity player = mc.player;

        // SafeWalk — не двигаться с края
        if (safeWalk) {
            // принудительно зажимаем снифт если блока нет под ногами впереди
            BlockPos front = getFrontEdgePos(player);
            if (front != null && mc.world.getBlockState(front).isAir()) {
                mc.options.sneakKey.setPressed(true);
            } else {
                mc.options.sneakKey.setPressed(false);
            }
        }

        // Есть ли блок под ногами
        BlockPos belowPos = player.getBlockPos().down();
        if (!mc.world.getBlockState(belowPos).isAir()) return;

        // Задержка
        if (delayCounter > 0) { delayCounter--; return; }

        // Найти блок в хотбаре
        int blockSlot = findBlockSlot(player);
        if (blockSlot < 0) return;

        // Найти опорный блок рядом
        BlockPos supportPos = null;
        Direction supportFace = Direction.UP;
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST,
                                             Direction.EAST, Direction.DOWN}) {
            BlockPos nb = belowPos.offset(dir);
            if (!mc.world.getBlockState(nb).isAir()) {
                supportPos = nb;
                supportFace = dir.getOpposite();
                break;
            }
        }
        if (supportPos == null) return;

        int prevSlot = player.getInventory().selectedSlot;
        if (blockSlot != prevSlot) {
            player.getInventory().selectedSlot = blockSlot;
        }

        float origYaw   = player.getYaw();
        float origPitch = player.getPitch();

        Vec3d hitVec = Vec3d.ofCenter(supportPos)
                .add(Vec3d.of(supportFace.getVector()).multiply(0.5));
        BlockHitResult hit = new BlockHitResult(hitVec, supportFace, supportPos, false);

        // Поворачиваем взгляд вниз через пакет (silent)
        mc.getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(origYaw, 88.0f, player.isOnGround())
        );

        var result = mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        if (result.isAccepted()) {
            player.swingHand(Hand.MAIN_HAND);
            delayCounter = delayTicks;
        }

        // Возвращаем слот и взгляд
        if (blockSlot != prevSlot) player.getInventory().selectedSlot = prevSlot;
        mc.getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(origYaw, origPitch, player.isOnGround())
        );
    }

    private static BlockPos getFrontEdgePos(ClientPlayerEntity player) {
        // Блок на 1 вперёд от игрока, уровень ног
        float yaw = player.getYaw();
        double rad = Math.toRadians(yaw);
        int dx = (int) Math.round(-Math.sin(rad));
        int dz = (int) Math.round( Math.cos(rad));
        return player.getBlockPos().add(dx, -1, dz);
    }

    public static int findBlockSlot(ClientPlayerEntity player) {
        var inv = player.getInventory();
        if (isGoodBlock(inv.getStack(inv.selectedSlot))) return inv.selectedSlot;
        for (int i = 0; i < 9; i++) {
            if (i == inv.selectedSlot) continue;
            if (isGoodBlock(inv.getStack(i))) return i;
        }
        return -1;
    }

    public static boolean isGoodBlock(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        var block = bi.getBlock();
        if (block instanceof FallingBlock) return false;
        if (block instanceof FluidBlock)   return false;
        return true;
    }
}
