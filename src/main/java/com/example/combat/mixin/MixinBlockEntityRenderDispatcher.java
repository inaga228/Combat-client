package com.example.combat.mixin;

import com.example.combat.modules.client.OptimizationModule;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void cancelBlockEntityRender(E blockEntity,
                                                                 float tickDelta,
                                                                 MatrixStack matrices,
                                                                 VertexConsumerProvider vertexConsumers,
                                                                 CallbackInfo ci) {
        if (OptimizationModule.enabled && OptimizationModule.shouldDisableBlockEntities()) {
            ci.cancel();
        }
    }
}
