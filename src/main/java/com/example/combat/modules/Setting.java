package com.example.combat.modules;

/**
 * Generic setting that can be displayed and edited in the ClickGUI.
 * Supports: boolean toggle, integer slider, float slider, enum.
 */
public class Setting<T> {

    public enum Type { TOGGLE, SLIDER_INT, SLIDER_FLOAT, ENUM }

    private final String name;
    private final Type type;
    private T value;

    // For sliders
    private double min, max;
    // For enums
    private final Object[] enumValues;

    public Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
        if (defaultValue instanceof Boolean) {
            this.type = Type.TOGGLE;
            this.enumValues = null;
        } else if (defaultValue instanceof Enum) {
            this.type = Type.ENUM;
            this.enumValues = defaultValue.getClass().getEnumConstants();
        } else if (defaultValue instanceof Float) {
            this.type = Type.SLIDER_FLOAT;
            this.enumValues = null;
        } else {
            this.type = Type.SLIDER_INT;
            this.enumValues = null;
        }
    }

    public Setting<T> range(double min, double max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public String getName() { return name; }
    public Type getType()   { return type; }
    public T getValue()     { return value; }
    public void setValue(T v) { this.value = v; }
    public double getMin()  { return min; }
    public double getMax()  { return max; }
    public Object[] getEnumValues() { return enumValues; }

    @SuppressWarnings("unchecked")
    public void cycleEnum() {
        if (type != Type.ENUM || enumValues == null) return;
        int idx = 0;
        for (int i = 0; i < enumValues.length; i++) {
            if (enumValues[i].equals(value)) { idx = i; break; }
        }
        value = (T) enumValues[(idx + 1) % enumValues.length];
    }
}
