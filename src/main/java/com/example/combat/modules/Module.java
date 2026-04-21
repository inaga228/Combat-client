package com.example.combat.modules;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public abstract class Module {

    public enum Category {
        COMBAT, RENDERER, HUD, PLAYER, MISC
    }

    protected final Minecraft mc = Minecraft.getInstance();

    private final String name;
    private final String description;
    private final Category category;

    private boolean enabled = false;
    private int keyBind = GLFW.GLFW_KEY_UNKNOWN;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public void onUpdate() {}
    public void onRender() {}
    public void onEnable() {}
    public void onDisable() {}

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public Category getCategory()  { return category; }
    public boolean isEnabled()     { return enabled; }
    public void setEnabled(boolean v) { if (v != enabled) toggle(); }
    public int getKeyBind()        { return keyBind; }
    public void setKeyBind(int key){ this.keyBind = key; }

    public String getKeyName() {
        if (keyBind == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        String n = GLFW.glfwGetKeyName(keyBind, 0);
        return n != null ? n.toUpperCase() : "KEY_" + keyBind;
    }
}
