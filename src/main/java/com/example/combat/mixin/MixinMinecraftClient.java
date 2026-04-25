package com.example.combat.mixin;

import com.example.combat.modules.building.FastPlaceModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * FastPlace mixin для Fabric 1.16.5.
 * В 1.16.5 Yarn-маппинги: метод doItemUse → method_1592
 * Поле itemUseCooldown → field_1740
 */
@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Shadow private int itemUseCooldown;

    @Redirect(
        method = "method_1592",  // doItemUse в Yarn 1.16.5
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;itemUseCooldown:I",
            opcode = org.objectweb.asm.Opcodes.GETFIELD
        )
    )
    private int redirectFastPlace(MinecraftClient instance) {
        if (!FastPlaceModule.enabled) return this.itemUseCooldown;

        if (FastPlaceModule.onlyBlocks) {
            if (instance.player == null) return this.itemUseCooldown;
            if (!(instance.player.getMainHandStack().getItem() instanceof BlockItem)) {
                return this.itemUseCooldown;
            }
        }
        return FastPlaceModule.cooldown;
    }
}
