package com.example.combat.modules.player;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;

public class FastPlace extends Module {

    public final Setting<Integer> delay = new Setting<>("Delay", 0).range(0, 4);

    // Forge MCP обфусцированное имя поля rightClickDelay в классе Minecraft
    private static final String[] FIELD_NAMES = {
        "rightClickDelay", "field_71429_W", "W"
    };
    private static Field rightClickDelayField;

    static {
        for (String name : FIELD_NAMES) {
            try {
                Field f = net.minecraft.client.Minecraft.class.getDeclaredField(name);
                f.setAccessible(true);
                rightClickDelayField = f;
                break;
            } catch (NoSuchFieldException ignored) {}
        }
        // Если по имени не нашли — перебираем все int-поля
        if (rightClickDelayField == null) {
            for (Field f : net.minecraft.client.Minecraft.class.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    try {
                        // rightClickDelay обычно равно 0 или 4 — ищем нужный
                        // Просто сохраняем первый int-поле с "delay" в имени
                        if (f.getName().toLowerCase().contains("delay")
                                || f.getName().toLowerCase().contains("click")) {
                            rightClickDelayField = f;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    public FastPlace() {
        super("FastPlace", "Reduces block placement delay", Category.PLAYER);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.player == null) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (rightClickDelayField != null) {
            try {
                int current = (int) rightClickDelayField.get(mc);
                if (current > delay.getValue()) {
                    rightClickDelayField.set(mc, delay.getValue());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
