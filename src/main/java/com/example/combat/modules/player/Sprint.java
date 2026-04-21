package com.example.combat.modules.player;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Sprint extends Module {

    public enum Mode { LEGIT, OMNI }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.LEGIT);

    public Sprint() {
        super("Sprint", "Auto-sprints in your movement direction", Category.PLAYER);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.player == null) return;
        if (mode.getValue() == Mode.LEGIT) {
            if (mc.player.moveForward > 0) mc.player.setSprinting(true);
        } else {
            // Omni: sprint even sideways
            boolean moving = mc.player.moveForward != 0 || mc.player.moveStrafing != 0;
            if (moving) mc.player.setSprinting(true);
        }
    }
}
