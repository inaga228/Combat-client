package com.example.combat.mixin;

import com.example.combat.modules.building.FastPlaceModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FastPlace mixin для Fabric 1.16.5.
 * В 1.16.5 Yarn: doItemUse называется handleBlockBreaking / doItemUse.
 * Самый надёжный способ — через @Inject в tick() сразу обнулять itemUseCooldown.
 */
@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    @Shadow private int itemUseCooldown;

    /**
     * Инжектируемся в конец tick() MinecraftClient.
     * Если FastPlace включён — принудительно выставляем itemUseCooldown = cooldown.
     * Это работает надёжно в 1.16.5 без привязки к конкретному Yarn-методу.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void fastPlaceTick(CallbackInfo ci) {
        if (!FastPlaceModule.enabled) return;

        MinecraftClient mc = (MinecraftClient)(Object) this;
        if (mc.player == null) return;

        if (FastPlaceModule.onlyBlocks) {
            if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        }

        if (this.itemUseCooldown > FastPlaceModule.cooldown) {
            this.itemUseCooldown = FastPlaceModule.cooldown;
        }
    }
}
