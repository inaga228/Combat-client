package com.example.combat.module.setting;

import java.util.List;

public class ModeSetting extends Setting {
    private final List<String> modes;
    private int index;

    public ModeSetting(String name, String defaultMode, List<String> modes) {
        super(name);
        this.modes = List.copyOf(modes);
        int idx = this.modes.indexOf(defaultMode);
        this.index = idx >= 0 ? idx : 0;
    }

    public String get() {
        return modes.get(index);
    }

    public void next() {
        index = (index + 1) % modes.size();
    }

    public void prev() {
        index = (index - 1 + modes.size()) % modes.size();
    }

    @Override
    public String getDisplayValue() {
        return get();
    }
}
