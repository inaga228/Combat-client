package com.example.combat.modules.player;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;

public class FastPlace extends Module {

    public final Setting<Integer> delay = new Setting<>("Delay", 0).range(0, 4);

    // В official mappings 1.16.5 поле называется rightClickDelay
    // Обфусцированное MCP имя — field_71429_W
    // Ищем оба варианта + перебором
    private static Field delayField = findField();

    private static Field findField() {
        // Пробуем все известные имена
        for (String name : new String[]{"rightClickDelay", "field_71429_W"}) {
            try {
                Field f = net.minecraft.client.Minecraft.class.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        // Перебираем все int-поля и ищем то что == 0 или 4 при старте
        // rightClickDelay — одно из немногих int полей в Minecraft класс
        // В official mappings оно называется rightClickDelay
        // Fallback: берём первое int-поле с названием содержащим нужные слова
        for (Field f : net.minecraft.client.Minecraft.class.getDeclaredFields()) {
            if (f.getType() != int.class) continue;
            String n = f.getName().toLowerCase();
            if (n.contains("rightclick") || n.contains("right_click") || n.contains("useitem")) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    public FastPlace() {
        super("FastPlace", "Reduces block placement delay", Category.PLAYER);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.player == null) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (delayField == null) return;
        try {
            int cur = (int) delayField.get(mc);
            if (cur > delay.getValue()) delayField.set(mc, delay.getValue());
        } catch (IllegalAccessException ignored) {}
    }
}
