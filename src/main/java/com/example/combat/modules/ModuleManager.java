package com.example.combat.modules;

import com.example.combat.modules.combat.AutoTotem;
import com.example.combat.modules.combat.Criticals;
import com.example.combat.modules.combat.KillAura;
import com.example.combat.modules.hud.Notifications;
import com.example.combat.modules.hud.ModuleListHud;
import com.example.combat.modules.hud.WatermarkHud;
import com.example.combat.modules.hud.FpsHud;
import com.example.combat.modules.hud.TotemHud;
import com.example.combat.modules.hud.CrystalHud;
import com.example.combat.modules.hud.TargetHud;
import com.example.combat.modules.player.FastPlace;
import com.example.combat.modules.renderer.BetterTab;
import com.example.combat.modules.renderer.HandView;
import com.example.combat.modules.renderer.ESP;
import com.example.combat.modules.renderer.ItemPhysics;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        // Combat
        register(new KillAura());
        register(new Criticals());
        register(new AutoTotem());
        // Player
        register(new FastPlace());
        // Renderer
        register(new BetterTab());
        register(new HandView());
        register(new ESP());
        register(new ItemPhysics());
        // HUD
        register(new Notifications());
        register(new ModuleListHud());
        register(new WatermarkHud());
        register(new FpsHud());
        register(new TotemHud());
        register(new CrystalHud());
        register(new TargetHud());
    }

    private void register(Module m) { modules.add(m); }

    public List<Module> getModules() { return modules; }

    public List<Module> getByCategory(Module.Category cat) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == cat) list.add(m);
        return list;
    }

    public Module getByName(String name) {
        for (Module m : modules) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }
}
