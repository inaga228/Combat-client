package com.example.combat.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;

/**
 * Простой PvP-модуль: помогает делать крит-удары, подпрыгивая в момент удара.
 */
public class AutoCritModule {
    public static boolean enabled = false;
    public static boolean playersOnly = true;
    public static boolean onlyWeapon = true;
    public static boolean syncWithTriggerBot = true;
    public static int jumpCooldownTicks = 8;

    private static int cooldown = 0;
    private static boolean wasAttackDown = false;

    public static void tick(MinecraftClient mc) {
        if (mc == null || mc.player == null || mc.world == null || !enabled) return;

        if (cooldown > 0) cooldown--;

        boolean attackDown = mc.options.keyAttack.isPressed();
        if (!attackDown || wasAttackDown) {
            wasAttackDown = attackDown;
            return;
        }
        wasAttackDown = true;

        if (cooldown > 0 || !mc.player.isOnGround() || mc.player.isSneaking() || mc.player.isTouchingWater()) return;
        if (onlyWeapon && !isWeaponHeld(mc)) return;

        Entity target = mc.targetedEntity;
        if (!(target instanceof LivingEntity) || target == mc.player) return;
        if (playersOnly && !(target instanceof PlayerEntity)) return;
        if (mc.player.squaredDistanceTo(target) > 16.0) return;

        mc.player.jump();
        cooldown = jumpCooldownTicks;
    }

    public static boolean prepareCritForTriggerBot(MinecraftClient mc, Entity target) {
        if (!enabled || !syncWithTriggerBot || mc == null || mc.player == null || target == null) return false;
        if (!(target instanceof LivingEntity) || target == mc.player) return false;
        if (playersOnly && !(target instanceof PlayerEntity)) return false;
        if (onlyWeapon && !isWeaponHeld(mc)) return false;
        if (cooldown > 0 || !mc.player.isOnGround() || mc.player.isSneaking() || mc.player.isTouchingWater()) return false;
        if (mc.player.squaredDistanceTo(target) > 16.0) return false;

        mc.player.jump();
        cooldown = jumpCooldownTicks;
        return true;
    }

    private static boolean isWeaponHeld(MinecraftClient mc) {
        return mc.player.getMainHandStack().getItem() instanceof SwordItem
            || mc.player.getMainHandStack().getItem() instanceof AxeItem;
    }
}
