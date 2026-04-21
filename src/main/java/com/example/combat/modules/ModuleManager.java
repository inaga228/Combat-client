package com.example.combat.modules;

import com.example.combat.modules.combat.*;
import com.example.combat.modules.renderer.*;
import com.example.combat.modules.hud.*;
import com.example.combat.modules.player.*;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        // Combat
        register(new KillAura());
        register(new Reach());
        register(new AntiKnockback());
        register(new AutoCrystal());
        register(new Velocity());

        // Renderer
        register(new ESP());
        register(new Trajectories());
        register(new Chams());
        register(new FullBright());
        register(new NoWeather());

        // HUD
        register(new HUDModule());
        register(new ArmorHUD());
        register(new PotionHUD());
        register(new Notifications());

        // Player
        register(new NoFall());
        register(new FastPlace());
        register(new AutoEat());
        register(new Sprint());
        register(new AntiVoid());
    }

    private void register(Module m) {
        modules.add(m);
    }

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
