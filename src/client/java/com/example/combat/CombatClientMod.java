package com.example.combat;

import com.example.combat.gui.ClickGUI;
import com.example.combat.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CombatClientMod implements ClientModInitializer {

    public static final String NAME    = "Combat Client";
    public static final String VERSION = "1.1.0";

    public static final ModuleManager moduleManager = new ModuleManager();

    public static KeyBinding openGuiKey;
    public static KeyBinding scaffoldKey;
    public static KeyBinding safeWalkKey;
    public static ClickGUI clickGUI;

    @Override
    public void onInitializeClient() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.combat-client.opengui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.combat-client"
        ));

        scaffoldKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.combat-client.scaffold",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "category.combat-client"
        ));

        safeWalkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.combat-client.safewalk",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.combat-client"
        ));

        clickGUI = new ClickGUI();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(clickGUI);
                }
            }

            while (scaffoldKey.wasPressed()) {
                moduleManager.scaffold().toggle();
            }

            while (safeWalkKey.wasPressed()) {
                moduleManager.safeWalk().toggle();
            }

            moduleManager.tick();
        });
    }
}
