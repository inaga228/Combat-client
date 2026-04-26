package com.example.combat;

import com.example.combat.config.ModuleConfig;
import com.example.combat.gui.ClickGUI;
import com.example.combat.modules.building.FastPlaceModule;
import com.example.combat.modules.building.ScaffoldModule;
import com.example.combat.modules.building.TowerModule;
import com.example.combat.modules.building.ClutchSaveModule;
import com.example.combat.modules.combat.AimAssistModule;
import com.example.combat.modules.combat.AutoCritModule;
import com.example.combat.modules.combat.BedwarsModule;
import com.example.combat.modules.combat.TriggerBotModule;
import com.example.combat.modules.client.OptimizationModule;
import com.example.combat.modules.client.PlayerEspModule;
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
    private static final int[] moduleBinds = {
        GLFW.GLFW_KEY_UNKNOWN, GLFW.GLFW_KEY_UNKNOWN, GLFW.GLFW_KEY_UNKNOWN,
        GLFW.GLFW_KEY_UNKNOWN, GLFW.GLFW_KEY_UNKNOWN, GLFW.GLFW_KEY_UNKNOWN, GLFW.GLFW_KEY_UNKNOWN,
        GLFW.GLFW_KEY_UNKNOWN,
        GLFW.GLFW_KEY_UNKNOWN,
        GLFW.GLFW_KEY_UNKNOWN
    };
    private static final boolean[] moduleBindWasDown = {false, false, false, false, false, false, false, false, false, false};

    @Override
    public void onInitializeClient() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.combat-client.opengui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "Combat Client"
        ));

        clickGUI = new ClickGUI();
        ModuleConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.openScreen(clickGUI);
                }
            }

            if (client.world == null || client.player == null) return;

            handleModuleBinds(client);
            OptimizationModule.tick(client);
            AimAssistModule.tick(client);
            TriggerBotModule.tick(client);
            AutoCritModule.tick(client);
            BedwarsModule.tick(client);
            PlayerEspModule.tick(client);
            ClutchSaveModule.tick(client);
            ScaffoldModule.tick(client);
            TowerModule.tick(client);
        });
    }

    private static void handleModuleBinds(net.minecraft.client.MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        for (int i = 0; i < moduleBinds.length; i++) {
            int key = moduleBinds[i];
            if (key == GLFW.GLFW_KEY_UNKNOWN) continue;

            boolean isDown = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
            if (isDown && !moduleBindWasDown[i]) {
                toggleModule(i);
            }
            moduleBindWasDown[i] = isDown;
        }
    }

    private static void toggleModule(int i) {
        switch (i) {
            case 0: ScaffoldModule.enabled = !ScaffoldModule.enabled; break;
            case 1: TowerModule.enabled = !TowerModule.enabled; break;
            case 2: FastPlaceModule.enabled = !FastPlaceModule.enabled; break;
            case 3: OptimizationModule.enabled = !OptimizationModule.enabled; break;
            case 4: TriggerBotModule.enabled = !TriggerBotModule.enabled; break;
            case 5: AimAssistModule.enabled = !AimAssistModule.enabled; break;
            case 6: ClutchSaveModule.enabled = !ClutchSaveModule.enabled; break;
            case 7: BedwarsModule.enabled = !BedwarsModule.enabled; break;
            case 8: PlayerEspModule.enabled = !PlayerEspModule.enabled; break;
            case 9: AutoCritModule.enabled = !AutoCritModule.enabled; break;
            default: break;
        }
        ModuleConfig.save();
    }

    public static int getModuleBind(int moduleIndex) {
        return moduleBinds[moduleIndex];
    }

    public static void setModuleBind(int moduleIndex, int key) {
        moduleBinds[moduleIndex] = key;
        moduleBindWasDown[moduleIndex] = false;
        ModuleConfig.save();
    }

    public static void saveConfig() {
        ModuleConfig.save();
    }
}
