package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import net.minecraft.client.GameSettings;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FullBright extends Module {

    private float savedGamma = 1.0f;

    public FullBright() {
        super("FullBright", "Maximizes brightness so you can see in the dark", Category.RENDERER);
    }

    @Override
    public void onEnable() {
        if (mc.gameSettings != null) {
            savedGamma = mc.gameSettings.gamma;
            mc.gameSettings.gamma = 16.0f;
        }
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            mc.gameSettings.gamma = savedGamma;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.gameSettings == null) return;
        mc.gameSettings.gamma = 16.0f;
    }
}
