package com.example.combat.modules.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Простая клиентская ESP-подсветка игроков через glowing-эффект.
 */
public class PlayerEspModule {
    public static boolean enabled = false;
    public static boolean includeInvisible = false;
    public static double maxDistance = 48.0;

    private static final Set<UUID> tracked = new HashSet<>();

    public static void tick(MinecraftClient mc) {
        if (mc == null || mc.world == null || mc.player == null) return;

        if (!enabled) {
            clearTracked(mc);
            return;
        }

        Set<UUID> seenNow = new HashSet<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == null || p == mc.player) continue;
            if (!includeInvisible && p.isInvisible()) continue;
            if (mc.player.squaredDistanceTo(p) > maxDistance * maxDistance) continue;

            p.setGlowing(true);
            seenNow.add(p.getUuid());
        }

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == null || p == mc.player) continue;
            UUID id = p.getUuid();
            if (tracked.contains(id) && !seenNow.contains(id)) {
                p.setGlowing(false);
            }
        }

        tracked.clear();
        tracked.addAll(seenNow);
    }

    private static void clearTracked(MinecraftClient mc) {
        if (tracked.isEmpty()) return;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == null || p == mc.player) continue;
            if (tracked.contains(p.getUuid())) {
                p.setGlowing(false);
            }
        }
        tracked.clear();
    }
}
