package com.example.combat.mixin;

import com.example.combat.modules.client.OptimizationModule;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void cancelSky(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (OptimizationModule.enabled && OptimizationModule.shouldDisableSky()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void cancelClouds(MatrixStack matrices, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (OptimizationModule.enabled && OptimizationModule.shouldDisableClouds()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void cancelWeather(LightmapTextureManager manager,
                               float tickDelta,
                               double cameraX,
                               double cameraY,
                               double cameraZ,
                               CallbackInfo ci) {
        if (OptimizationModule.enabled && OptimizationModule.shouldDisableWeather()) {
            ci.cancel();
        }
    }
}
