package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;

public class Chams extends Module {

    public enum Style { FLAT, SHINY, WIREFRAME }

    public final Setting<Style>   style   = new Setting<>("Style", Style.FLAT);
    public final Setting<Boolean> players = new Setting<>("Players", true);
    public final Setting<Boolean> throughWalls = new Setting<>("ThroughWalls", true);
    public final Setting<Float>   r       = new Setting<>("R", 0.0f).range(0, 1);
    public final Setting<Float>   g       = new Setting<>("G", 0.8f).range(0, 1);
    public final Setting<Float>   b       = new Setting<>("B", 1.0f).range(0, 1);

    public Chams() {
        super("Chams", "Replaces entity textures with solid colors", Category.RENDERER);
    }
    // Applied via RenderLivingEvent
}
