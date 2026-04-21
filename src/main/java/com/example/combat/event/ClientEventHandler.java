package com.example.combat.event;

import com.example.combat.CombatClient;
import com.example.combat.gui.ClickGUI;
import com.example.combat.modules.Module;
import com.example.combat.modules.hud.Notifications;
import com.example.combat.modules.renderer.BetterTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.overlay.PlayerTabOverlayGui;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class ClientEventHandler {

    private final Minecraft mc = Minecraft.getInstance();
    private static ClickGUI clickGUI;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.player == null || mc.level == null) return;
        for (Module m : CombatClient.moduleManager.getModules()) {
            if (m.isEnabled()) m.onUpdate();
        }
    }

    @SubscribeEvent
    public void onKeyInput(net.minecraftforge.client.event.InputEvent.KeyInputEvent event) {
        if (mc.screen != null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        int key = event.getKey();

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (clickGUI == null) clickGUI = new ClickGUI();
            mc.setScreen(clickGUI);
            return;
        }

        for (Module m : CombatClient.moduleManager.getModules()) {
            if (m.getKeyBind() != GLFW.GLFW_KEY_UNKNOWN && m.getKeyBind() == key) {
                m.toggle();
                Notifications.push(m.getName() + " " + (m.isEnabled() ? "ON" : "OFF"));
            }
        }
    }
}
