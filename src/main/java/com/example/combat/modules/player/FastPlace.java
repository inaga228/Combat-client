package com.example.combat.modules.player;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FastPlace extends Module {
    public final Setting<Integer> delay = new Setting<>("Delay", 0).range(0, 4);

    public FastPlace() {
        super("FastPlace", "Reduces block placement delay", Category.PLAYER);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.player == null) return;
        mc.rightClickDelayTimer = delay.getValue();
    }
}
