package com.example.combat.module.setting;

public class NumberSetting extends Setting {
    private final int min;
    private final int max;
    private final int step;
    private int value;

    public NumberSetting(String name, int defaultValue, int min, int max, int step) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.value = Math.max(min, Math.min(max, defaultValue));
    }

    public int get() {
        return value;
    }

    public void increase() {
        value = Math.min(max, value + step);
    }

    public void decrease() {
        value = Math.max(min, value - step);
    }

    @Override
    public String getDisplayValue() {
        return Integer.toString(value);
    }
}
