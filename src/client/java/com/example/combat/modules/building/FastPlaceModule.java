package com.example.combat.modules.building;

/**
 * FastPlace — убирает кулдаун укладки блоков.
 * Логика из LiquidBounce ModuleFastPlace.
 * Фактическая работа — через MixinFastPlace который читает enabled.
 */
public class FastPlaceModule {

    public static boolean enabled    = false;
    public static boolean onlyBlocks = true;   // только блоки, не еда и т.д.
    public static int     cooldown   = 0;       // 0-4 тиков (0 = мгновенно)
}
