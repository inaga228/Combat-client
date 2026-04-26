package com.example.combat.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * TriggerBot — автоматически бьёт цель под прицелом.
 * Клиентская логика, без ротаций и без packet-спуфинга.
 */
public class TriggerBotModule {
    public static boolean enabled = false;
    public static int cps = 8; // 4..14
    public static boolean onlyWeapon = true;
    public static boolean playersOnly = true;
    public static double range = 3.4; // 2.5..6.0

    private static int ticks = 0;

    public static void tick(MinecraftClient mc) {
        if (!enabled || mc == null || mc.player == null || mc.world == null) return;

        int interval = Math.max(1, 20 / Math.max(1, cps));
        ticks++;
        if (ticks < interval) return;
        ticks = 0;

        if (!(mc.crosshairTarget instanceof EntityHitResult)) return;
        HitResult hr = mc.crosshairTarget;
        if (hr.getType() != HitResult.Type.ENTITY) return;

        EntityHitResult hit = (EntityHitResult) hr;
        if (!(hit.getEntity() instanceof LivingEntity)) return;
        if (hit.getEntity() == mc.player) return;
        if (playersOnly && !(hit.getEntity() instanceof PlayerEntity)) return;
        if (mc.player.distanceTo(hit.getEntity()) > range) return;

        if (onlyWeapon && !isWeapon(mc.player.getMainHandStack())) return;
        if (mc.player.getAttackCooldownProgress(0.0f) < 0.92f) return;
        if (AutoCritModule.prepareCritForTriggerBot(mc, hit.getEntity())) return;

        mc.interactionManager.attackEntity(mc.player, hit.getEntity());
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }

    private static boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }
}
