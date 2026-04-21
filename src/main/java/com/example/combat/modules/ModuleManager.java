package com.example.combat.modules;

import com.example.combat.modules.combat.AutoTotem;
import com.example.combat.modules.combat.Criticals;
import com.example.combat.modules.combat.KillAura;
import com.example.combat.modules.hud.Notifications;
import com.example.combat.modules.player.FastPlace;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        register(new KillAura());
        register(new Criticals());
        register(new AutoTotem());
        register(new FastPlace());
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
