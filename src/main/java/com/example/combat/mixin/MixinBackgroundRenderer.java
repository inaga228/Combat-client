package com.example.combat.mixin;

import com.example.combat.modules.client.OptimizationModule;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer {

    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void cancelFog(Camera camera,
                                  BackgroundRenderer.FogType fogType,
                                  float viewDistance,
                                  boolean thickFog,
                                  CallbackInfo ci) {
        if (OptimizationModule.enabled && OptimizationModule.disableFog) {
            ci.cancel();
        }
    }
}
