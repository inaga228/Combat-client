package com.example.combat.modules.player;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;

public class FastPlace extends Module {
    public final Setting<Integer> delay = new Setting<>("Delay", 0).range(0, 4);

    private static Field rightClickDelayField;

    static {
        try {
            rightClickDelayField = net.minecraft.client.Minecraft.class.getDeclaredField("rightClickDelay");
            rightClickDelayField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public FastPlace() {
        super("FastPlace", "Reduces block placement delay", Category.PLAYER);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.player == null) return;
        try {
            if (rightClickDelayField != null) {
                rightClickDelayField.set(mc, delay.getValue());
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
