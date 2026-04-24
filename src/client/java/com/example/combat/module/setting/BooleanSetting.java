package com.example.combat.module.setting;

public class BooleanSetting extends Setting {
    private boolean value;

    public BooleanSetting(String name, boolean defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public boolean get() {
        return value;
    }

    public void toggle() {
        value = !value;
    }

    @Override
    public String getDisplayValue() {
        return value ? "ON" : "OFF";
    }
}
