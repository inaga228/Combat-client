package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;

public class Velocity extends Module {
    public final Setting<Integer> horizontal = new Setting<>("Horizontal%", 0).range(0, 100);
    public final Setting<Integer> vertical   = new Setting<>("Vertical%",   0).range(0, 100);

    public Velocity() {
        super("Velocity", "Reduces knockback from attacks", Category.COMBAT);
    }
}
