package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;

public class AntiKnockback extends Module {
    public final Setting<Integer> percent = new Setting<>("Percent", 100).range(0, 100);

    public AntiKnockback() {
        super("AntiKnockback", "Reduces knockback you receive", Category.COMBAT);
    }
}
