package com.example.combat.mixin;

import com.example.combat.modules.building.FastPlaceModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Перехватывает проверку itemUseCooldown в MinecraftClient.
 * В 1.21.1 метод называется interactItem (intermediary: method_1611).
 */
@Mixin(MinecraftClient.class)
public class MixinFastPlace {

    @Shadow private int itemUseCooldown;

    @Redirect(
        method = "method_1611",  // intermediary-имя interactItem в 1.21.1
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;itemUseCooldown:I",
            opcode = org.objectweb.asm.Opcodes.GETFIELD
        )
    )
    private int redirectItemUseCooldown(MinecraftClient instance) {
        if (!FastPlaceModule.enabled) return this.itemUseCooldown;
        if (FastPlaceModule.onlyBlocks) {
            var player = instance.player;
            if (player == null) return this.itemUseCooldown;
            if (!(player.getMainHandStack().getItem() instanceof BlockItem)) return this.itemUseCooldown;
        }
        return FastPlaceModule.cooldown;
    }
}
