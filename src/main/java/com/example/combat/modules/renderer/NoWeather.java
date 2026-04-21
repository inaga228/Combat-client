package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;

public class NoWeather extends Module {
    public NoWeather() {
        super("NoWeather", "Hides rain and snow visually", Category.RENDERER);
    }
    // Applied via mixin/event that cancels weather rendering
}
