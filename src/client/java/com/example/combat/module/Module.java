package com.example.combat.module;

import com.example.combat.module.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String name;
    private boolean enabled;
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    protected final List<Setting> settings = new ArrayList<>();

    protected Module(String name) {
        this.name = name;
    }

    protected <T extends Setting> T register(T setting) {
        settings.add(setting);
        return setting;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public List<Setting> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
}
