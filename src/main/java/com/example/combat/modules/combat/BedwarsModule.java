package com.example.combat.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;

/**
 * Набор простых функций под Bedwars.
 */
public class BedwarsModule {
    public static boolean enabled = false;
    public static boolean autoSprint = true;
    public static boolean edgeStop = true;
    public static boolean autoBlockSwap = true;
    public static boolean bridgeJumpAssist = false;
    public static boolean voidAlert = true;

    private static int alertCooldown = 0;

    public static void tick(MinecraftClient mc) {
        if (!enabled || mc == null || mc.player == null || mc.world == null) return;

        if (autoSprint && mc.options.keyForward.isPressed() && mc.player.isOnGround() && !mc.player.isSneaking()) {
            mc.player.setSprinting(true);
        }

        if (edgeStop && mc.player.isOnGround()) {
            double yaw = Math.toRadians(mc.player.yaw);
            double frontX = mc.player.getX() + Math.sin(yaw) * 0.60;
            double frontZ = mc.player.getZ() - Math.cos(yaw) * 0.60;
            BlockPos frontBelow = new BlockPos(frontX, mc.player.getY() - 1.0, frontZ);
            if (mc.world.getBlockState(frontBelow).isAir()) {
                mc.player.setVelocity(mc.player.getVelocity().x * 0.15, mc.player.getVelocity().y, mc.player.getVelocity().z * 0.15);
                if (voidAlert && alertCooldown <= 0) {
                    mc.player.sendMessage(new LiteralText("§c[Bedwars] Осторожно, край!"), true);
                    alertCooldown = 30;
                }
            }
        }

        if (bridgeJumpAssist && mc.player.isOnGround() && mc.options.keyForward.isPressed() && !mc.player.isSneaking()) {
            double yaw = Math.toRadians(mc.player.yaw);
            BlockPos frontBelow = new BlockPos(
                mc.player.getX() + Math.sin(yaw) * 0.85,
                mc.player.getY() - 1.0,
                mc.player.getZ() - Math.cos(yaw) * 0.85
            );
            if (mc.world.getBlockState(frontBelow).isAir()) {
                mc.player.jump();
            }
        }

        if (autoBlockSwap && mc.options.keyUse.isPressed() && !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
            int slot = findBlockSlot(mc);
            if (slot >= 0) setHotbarSlot(mc, slot);
        }

        if (alertCooldown > 0) alertCooldown--;
    }

    private static int findBlockSlot(MinecraftClient mc) {
        int best = -1;
        int count = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStack(i);
            if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            if (stack.getCount() > count) {
                count = stack.getCount();
                best = i;
            }
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
}
