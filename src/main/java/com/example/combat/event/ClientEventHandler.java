package com.example.combat.event;

import com.example.combat.CombatClient;
import com.example.combat.gui.ClickGUI;
import com.example.combat.modules.Module;
import com.example.combat.modules.hud.Notifications;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class ClientEventHandler {

    private final Minecraft mc = Minecraft.getInstance();
    private static ClickGUI clickGUI;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.player == null || mc.world == null) return;
        for (Module m : CombatClient.moduleManager.getModules()) {
            if (m.isEnabled()) m.onUpdate();
        }
    }

    @SubscribeEvent
    public void onKeyInput(net.minecraftforge.client.event.InputEvent.KeyInputEvent event) {
        if (mc.currentScreen != null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        int key = event.getKey();

        // Right Shift opens ClickGUI
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (clickGUI == null) clickGUI = new ClickGUI();
            mc.displayGuiScreen(clickGUI);
            return;
        }

        // Module keybinds
        for (Module m : CombatClient.moduleManager.getModules()) {
            if (m.getKeyBind() != GLFW.GLFW_KEY_UNKNOWN && m.getKeyBind() == key) {
                m.toggle();
                Notifications.push(m.getName() + " " + (m.isEnabled() ? "§aON" : "§cOFF"));
            }
        }
    }
}
