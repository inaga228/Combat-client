package com.example.combat.modules.building;

/**
 * FastPlace — убирает задержку установки блоков.
 * Работает через MixinMinecraftClient.
 */
public class FastPlaceModule {
    public static boolean enabled    = false;
    public static int     cooldown   = 0;      // 0..4 тиков
    public static boolean onlyBlocks = true;
}
