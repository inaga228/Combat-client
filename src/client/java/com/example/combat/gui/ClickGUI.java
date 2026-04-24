package com.example.combat.gui;

import com.example.combat.CombatClientMod;
import com.example.combat.module.Module;
import com.example.combat.module.setting.BooleanSetting;
import com.example.combat.module.setting.ModeSetting;
import com.example.combat.module.setting.NumberSetting;
import com.example.combat.module.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {
    private static final int PANEL_W = 440;
    private static final int PANEL_H = 280;

    private float openAnim;

    public ClickGUI() {
        super(Text.literal("Combat ClickGUI"));
    }

    @Override
    protected void init() {
        openAnim = 0f;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        openAnim = Math.min(1f, openAnim + delta * 0.15f);
        float ease = 1f - (1f - openAnim) * (1f - openAnim);

        int px = (width - PANEL_W) / 2;
        int py = (int) ((height - PANEL_H) / 2f + (1f - ease) * 24f);

        ctx.fill(0, 0, width, height, color(0, 0, 0, (int) (165 * ease)));
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, color(10, 10, 22, 245));
        ctx.fill(px, py, px + PANEL_W, py + 24, color(18, 16, 40, 255));
        ctx.drawTextWithShadow(textRenderer, "Combat Client • Build modules", px + 8, py + 8, color(220, 215, 255, 255));

        int y = py + 34;
        int lineHeight = 12;
        for (RenderLine line : buildLines()) {
            boolean hover = mx >= px + 8 && mx <= px + PANEL_W - 8 && my >= y - 1 && my <= y + lineHeight;
            int bg = hover ? color(40, 35, 70, 120) : color(0, 0, 0, 0);
            ctx.fill(px + 6, y - 1, px + PANEL_W - 6, y + lineHeight, bg);
            ctx.drawTextWithShadow(textRenderer, line.label(), px + 10, y + 1, line.color());
            y += lineHeight + 2;
            if (y > py + PANEL_H - 16) {
                break;
            }
        }

        ctx.drawTextWithShadow(textRenderer,
            "LMB: toggle/inc • RMB: expand/dec • MMB: mode prev • ESC: close",
            px + 8,
            py + PANEL_H - 12,
            color(145, 140, 200, 220));

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int px = (width - PANEL_W) / 2;
        int py = (height - PANEL_H) / 2;
        int y = py + 34;
        int lineHeight = 12;

        for (RenderLine line : buildLines()) {
            boolean hover = mx >= px + 8 && mx <= px + PANEL_W - 8 && my >= y - 1 && my <= y + lineHeight;
            if (hover) {
                line.click(button);
                return true;
            }
            y += lineHeight + 2;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private List<RenderLine> buildLines() {
        List<RenderLine> lines = new ArrayList<>();
        for (Module module : CombatClientMod.moduleManager.getModules()) {
            lines.add(new RenderLine(
                "[" + (module.isEnabled() ? "ON" : "OFF") + "] " + module.getName(),
                module.isEnabled() ? color(155, 255, 170, 255) : color(255, 145, 145, 255),
                button -> {
                    if (button == 0) {
                        module.toggle();
                    }
                }
            ));

            for (Setting setting : module.getSettings()) {
                lines.add(new RenderLine(
                    "    • " + setting.getName() + ": " + setting.getDisplayValue(),
                    color(190, 185, 240, 255),
                    button -> mutateSetting(setting, button)
                ));
            }
        }
        return lines;
    }

    private void mutateSetting(Setting setting, int button) {
        if (setting instanceof BooleanSetting boolSetting && (button == 0 || button == 1)) {
            boolSetting.toggle();
            return;
        }

        if (setting instanceof NumberSetting numberSetting) {
            if (button == 0) {
                numberSetting.increase();
            } else if (button == 1) {
                numberSetting.decrease();
            }
            return;
        }

        if (setting instanceof ModeSetting modeSetting) {
            if (button == 2) {
                modeSetting.prev();
            } else if (button == 0 || button == 1) {
                modeSetting.next();
            }
        }
    }

    private static int color(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    private record RenderLine(String label, int color, ClickAction clickAction) {
        void click(int button) {
            clickAction.click(button);
        }
    }

    @FunctionalInterface
    private interface ClickAction {
        void click(int button);
    }
}
