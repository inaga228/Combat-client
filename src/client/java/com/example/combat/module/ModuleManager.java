package com.example.combat.module;

import com.example.combat.module.build.SafeWalkModule;
import com.example.combat.module.build.ScaffoldModule;

import java.util.List;

public class ModuleManager {
    private final ScaffoldModule scaffoldModule = new ScaffoldModule();
    private final SafeWalkModule safeWalkModule = new SafeWalkModule();
    private final List<Module> modules = List.of(scaffoldModule, safeWalkModule);

    public void tick() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public ScaffoldModule scaffold() {
        return scaffoldModule;
    }

    public SafeWalkModule safeWalk() {
        return safeWalkModule;
    }
}
