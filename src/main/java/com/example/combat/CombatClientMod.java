package com.example.combat;

import com.example.combat.gui.ClickGUI;
import com.example.combat.modules.building.FastPlaceModule;
import com.example.combat.modules.building.ScaffoldModule;
import com.example.combat.modules.building.TowerModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CombatClientMod implements ClientModInitializer {

    public static final String NAME    = "Combat Client";
    public static final String VERSION = "1.0.0";

    public static KeyBinding openGuiKey;
    public static ClickGUI   clickGUI;

    @Override
    public void onInitializeClient() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.combat-client.opengui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "Combat Client"
        ));

        clickGUI = new ClickGUI();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.openScreen(clickGUI);
                }
            }

            if (client.world == null || client.player == null) return;

            ScaffoldModule.tick(client);
            TowerModule.tick(client);
        });
    }
}
