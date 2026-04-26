package com.example.combat.modules.building;

/**
 * FastPlaceModule — убирает задержку установки блоков.
 * Логика через MixinMinecraftClient (обнуление itemUseCooldown в tick).
 */
public class FastPlaceModule {
    public static boolean enabled    = false;
    public static int     cooldown   = 0;      // 0..4 тиков (0 = мгновенно)
    public static boolean onlyBlocks = true;   // только для блоков, не для предметов
}
