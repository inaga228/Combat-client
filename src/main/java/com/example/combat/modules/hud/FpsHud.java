package com.example.combat.modules.hud;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.example.combat.utils.RenderUtil;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FpsHud extends Module {

    public final Setting<Integer> posX       = new Setting<>("PosX", 4).range(0, 1000);
    public final Setting<Integer> posY       = new Setting<>("PosY", 16).range(0, 1000);
    public final Setting<Boolean> background = new Setting<>("Background", true);
    public final Setting<Boolean> colored    = new Setting<>("Colored",    true);

    public FpsHud() {
        super("FpsHud", "Shows FPS counter", Category.HUD);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled()) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        int fps = com.example.combat.utils.RenderUtil.getFps();
        String text = "FPS: " + fps;

        int x = posX.getValue();
        int y = posY.getValue();

        MatrixStack ms = event.getMatrixStack();

        if (background.getValue()) {
            RenderUtil.drawRect(ms, x - 2, y - 1, mc.fontRenderer.width(text) + 4, 10, 0x80000000);
        }

        int color;
        if (colored.getValue()) {
            color = fps >= 60 ? 0xFF55FF55 : fps >= 30 ? 0xFFFFFF55 : 0xFFFF5555;
        } else {
            color = 0xFFFFFFFF;
        }

        mc.fontRenderer.drawShadow(ms, text, x, y, color);
    }
}
