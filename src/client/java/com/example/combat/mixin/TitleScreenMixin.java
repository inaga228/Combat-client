package com.example.combat.mixin;

import com.example.combat.gui.MainMenu;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        // Заменяем стандартный TitleScreen нашим MainMenu
        net.minecraft.client.MinecraftClient.getInstance().setScreen(new MainMenu());
        ci.cancel();
    }
}
