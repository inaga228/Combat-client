package com.example.combat.modules;

import com.example.combat.modules.combat.AutoTotem;
import com.example.combat.modules.combat.Criticals;
import com.example.combat.modules.combat.CrystalAura;
import com.example.combat.modules.combat.KillAura;
import com.example.combat.modules.hud.*;
import com.example.combat.modules.player.FastPlace;
import com.example.combat.modules.renderer.BetterTab;
import com.example.combat.modules.renderer.ESP;
import com.example.combat.modules.renderer.HandView;
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
        register(new CrystalAura());
        // Player
        register(new FastPlace());
        // Renderer
        register(new BetterTab());
        register(new ESP());
        register(new HandView());
        register(new ItemPhysics());
        // HUD
        register(new Notifications());
        register(new WatermarkHud());
        register(new FpsHud());
        register(new ModuleListHud());
        register(new TargetHud());
        register(new TotemHud());
        register(new CrystalHud());
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
