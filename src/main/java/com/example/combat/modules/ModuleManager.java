package com.example.combat.modules;

import com.example.combat.modules.combat.AutoTotem;
import com.example.combat.modules.combat.Criticals;
import com.example.combat.modules.combat.KillAura;
import com.example.combat.modules.hud.Notifications;
import com.example.combat.modules.player.FastPlace;
import com.example.combat.modules.renderer.BetterTab;
import com.example.combat.modules.renderer.HandView;

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
        // HUD
        register(new Notifications());
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
