package com.example.combat.modules.player;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AntiVoid extends Module {
    public final Setting<Float> safeY = new Setting<>("SafeY", -60f).range(-128f, 0f);

    public AntiVoid() {
        super("AntiVoid", "Stops you from falling into the void", Category.PLAYER);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.player == null) return;
        if (mc.player.getPosY() < safeY.getValue() && mc.player.getMotion().y < 0) {
            mc.player.setMotion(mc.player.getMotion().x, 0.1, mc.player.getMotion().z);
        }
    }
}
