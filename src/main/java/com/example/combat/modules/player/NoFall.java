package com.example.combat.modules.player;

import com.example.combat.modules.Module;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;

public class NoFall extends Module {
    public NoFall() {
        super("NoFall", "Prevents fall damage by sending onGround=true", Category.PLAYER);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.player == null) return;
        if (mc.player.fallDistance > 2.0f) {
            mc.getConnection().sendPacket(new CPlayerPacket(true));
        }
    }
}
