package com.example.combat.modules.hud;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.example.combat.utils.RenderUtil;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WatermarkHud extends Module {

    public final Setting<Boolean> showVersion = new Setting<>("ShowVersion", true);
    public final Setting<Boolean> showFps     = new Setting<>("ShowFPS",     true);

    private static final String CLIENT_NAME    = "Combat Client";
    private static final String CLIENT_VERSION = "v1.0";
    private static final int    BG             = 0x80000000;
    private static final int    COL_NAME       = 0xFF00FFAA;  // акцентный
    private static final int    COL_INFO       = 0xFFAAAAAA;  // серый

    public WatermarkHud() {
        super("Watermark", "Shows client name in the corner", Category.HUD);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled()) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        MatrixStack ms = event.getMatrixStack();

        String name    = CLIENT_NAME;
        String version = showVersion.getValue() ? " " + CLIENT_VERSION : "";
        String fps     = showFps.getValue()
            ? "  " + net.minecraft.client.Minecraft.getInstance().getFps() + " fps"
            : "";

        int nameW    = mc.font.width(name);
        int versionW = mc.font.width(version);
        int fpsW     = mc.font.width(fps);
        int totalW   = nameW + versionW + fpsW;

        int x = 4, y = 4;
        RenderUtil.drawRect(ms, x - 2, y - 1, totalW + 4, 10, BG);
        // Рамка снизу акцентным цветом
        RenderUtil.drawRect(ms, x - 2, y + 9, totalW + 4, 1, COL_NAME);

        mc.font.drawShadow(ms, name,    x,             y, COL_NAME);
        mc.font.drawShadow(ms, version, x + nameW,     y, COL_INFO);
        mc.font.drawShadow(ms, fps,     x + nameW + versionW, y, COL_INFO);
    }
}
