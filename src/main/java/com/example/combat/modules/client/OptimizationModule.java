package com.example.combat.modules.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;

/**
 * Optimization module with visual toggles similar to classic OptiFine options.
 * Keeps everything client-side and does not touch server packets.
 */
public class OptimizationModule {

    public static boolean enabled = false;

    public static boolean disableParticles = true;
    public static boolean disableSky       = true;
    public static boolean disableClouds    = true;
    public static boolean disableFog       = true;
    public static boolean disableWeather   = false;
    public static boolean renderBoost      = true;

    private static ParticlesMode prevParticles = null;
    private static Boolean prevEntityShadows   = null;

    public static void tick(MinecraftClient mc) {
        if (mc == null || mc.options == null) return;

        if (enabled) {
            apply(mc);
        } else {
            restore(mc);
        }
    }

    private static void apply(MinecraftClient mc) {
        if (disableParticles) {
            if (prevParticles == null) prevParticles = mc.options.particles;
            mc.options.particles = ParticlesMode.MINIMAL;
        } else if (prevParticles != null) {
            mc.options.particles = prevParticles;
            prevParticles = null;
        }

        // Clouds are disabled by MixinWorldRenderer#cancelClouds to stay mapping-safe.
        if (renderBoost) {
            if (prevEntityShadows == null) prevEntityShadows = mc.options.entityShadows;
            mc.options.entityShadows = false;
        } else if (prevEntityShadows != null) {
            mc.options.entityShadows = prevEntityShadows;
            prevEntityShadows = null;
        }
    }

    private static void restore(MinecraftClient mc) {
        if (prevParticles != null) {
            mc.options.particles = prevParticles;
            prevParticles = null;
        }
        if (prevEntityShadows != null) {
            mc.options.entityShadows = prevEntityShadows;
            prevEntityShadows = null;
        }
    }
}
