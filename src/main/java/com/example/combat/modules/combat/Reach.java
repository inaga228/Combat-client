package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;

public class Reach extends Module {
    public final Setting<Float> reachDistance = new Setting<>("Distance", 4.5f).range(3.0, 6.0);

    public Reach() {
        super("Reach", "Increases your attack reach", Category.COMBAT);
    }
    // Applied via event/mixin that reads this value
}
