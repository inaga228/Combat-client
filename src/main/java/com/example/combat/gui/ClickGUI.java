package com.example.combat.gui;

import com.example.combat.CombatClient;
import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ClickGUI — compact panel-based module selector.
 *
 * Left-click  → toggle module on/off
 * Right-click → open settings panel for that module
 * Drag panel  → move it around
 * Press ESC   → close GUI
 */
public class ClickGUI extends Screen {

    // ---- Panel per category ----
    private static class Panel {
        Module.Category cat;
        int x, y;
        boolean collapsed = false;
        boolean dragging  = false;
        int dragOffX, dragOffY;

        static final int W = 100;
        static final int HEADER_H = 12;
        static final int ROW_H = 11;

        Panel(Module.Category cat, int x, int y) {
            this.cat = cat; this.x = x; this.y = y;
        }
    }

    // ---- Settings panel ----
    private Module settingsTarget = null;
    private int settingsX = 0, settingsY = 0;
    private boolean settingsDragging = false;
    private int settingsDragOffX, settingsDragOffY;
    private boolean waitingForBind = false;

    private final List<Panel> panels = new ArrayList<>();

    public ClickGUI() {
        super(new StringTextComponent("CombatClient"));
        int x = 4;
        for (Module.Category cat : Module.Category.values()) {
            panels.add(new Panel(cat, x, 4));
            x += Panel.W + 4;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ---- Render ----

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partial) {
        // Dim background
        fill(ms, 0, 0, width, height, 0x88000000);

        for (Panel p : panels) renderPanel(ms, p, mouseX, mouseY);

        if (settingsTarget != null) renderSettings(ms, mouseX, mouseY);

        super.render(ms, mouseX, mouseY, partial);
    }

    private void renderPanel(MatrixStack ms, Panel p, int mx, int my) {
        List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);
        int totalH = Panel.HEADER_H + (p.collapsed ? 0 : mods.size() * Panel.ROW_H);

        // Background
        fill(ms, p.x, p.y, p.x + Panel.W, p.y + totalH, 0xCC1A1A2E);
        // Header
        fill(ms, p.x, p.y, p.x + Panel.W, p.y + Panel.HEADER_H, 0xFF16213E);
        font.drawString(ms, p.cat.name(), p.x + 3, p.y + 2, 0x00FFAA);

        if (!p.collapsed) {
            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                int ry = p.y + Panel.HEADER_H + i * Panel.ROW_H;
                boolean hover = mx >= p.x && mx < p.x + Panel.W && my >= ry && my < ry + Panel.ROW_H;

                int bg = m.isEnabled() ? 0xCC0F3460 : (hover ? 0x44FFFFFF : 0x00000000);
                fill(ms, p.x, ry, p.x + Panel.W, ry + Panel.ROW_H, bg);

                int nameColor = m.isEnabled() ? 0xFFFFFF : 0xAAAAAA;
                font.drawString(ms, m.getName(), p.x + 3, ry + 2, nameColor);

                // Keybind label
                String kb = m.getKeyBind() != GLFW.GLFW_KEY_UNKNOWN ? "[" + m.getKeyName() + "]" : "";
                if (!kb.isEmpty()) {
                    int kbW = font.getStringWidth(kb);
                    font.drawString(ms, kb, p.x + Panel.W - kbW - 2, ry + 2, 0x666666);
                }
            }
        }
    }

    private void renderSettings(MatrixStack ms, int mx, int my) {
        List<Setting<?>> settings = getSettings(settingsTarget);
        int sw = 120;
        int sh = 14 + settings.size() * 14 + 14; // header + rows + bind row

        fill(ms, settingsX, settingsY, settingsX + sw, settingsY + sh, 0xEE0D1B2A);
        fill(ms, settingsX, settingsY, settingsX + sw, settingsY + 13, 0xFF1B2838);
        font.drawString(ms, "§b" + settingsTarget.getName(), settingsX + 3, settingsY + 3, 0xFFFFFF);

        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            int ry = settingsY + 14 + i * 14;
            renderSettingRow(ms, s, settingsX, ry, sw, mx, my);
        }

        // Bind row
        int by = settingsY + 14 + settings.size() * 14;
        fill(ms, settingsX, by, settingsX + sw, by + 13, 0x33FFFFFF);
        String bindLabel = waitingForBind ? "§ePress key..." : "Bind: §f" + settingsTarget.getKeyName();
        font.drawString(ms, bindLabel, settingsX + 3, by + 3, 0xCCCCCC);
    }

    @SuppressWarnings("unchecked")
    private void renderSettingRow(MatrixStack ms, Setting<?> s, int x, int y, int w, int mx, int my) {
        font.drawString(ms, s.getName(), x + 3, y + 3, 0xCCCCCC);

        switch (s.getType()) {
            case TOGGLE -> {
                boolean v = (Boolean) s.getValue();
                String lbl = v ? "§aON" : "§cOFF";
                font.drawString(ms, lbl, x + w - font.getStringWidth(lbl.replaceAll("§.", "")) - 4, y + 3, 0xFFFFFF);
            }
            case ENUM -> {
                String lbl = ((Enum<?>) s.getValue()).name();
                font.drawString(ms, lbl, x + w - font.getStringWidth(lbl) - 4, y + 3, 0xFFD700);
            }
            case SLIDER_INT, SLIDER_FLOAT -> {
                double val = s.getValue() instanceof Float ? (Float) s.getValue() : (Integer) s.getValue();
                double min = s.getMin(), max = s.getMax();
                int barW = w - 6;
                int filled = (int) ((val - min) / (max - min) * barW);
                fill(ms, x + 3, y + 9, x + 3 + barW, y + 12, 0x441A1A1A);
                fill(ms, x + 3, y + 9, x + 3 + filled, y + 12, 0xFF00FFAA);
                String valStr = s.getValue() instanceof Float
                    ? String.format("%.1f", val) : String.valueOf((int) val);
                font.drawString(ms, valStr, x + w - font.getStringWidth(valStr) - 4, y + 3, 0xFFFFFF);
            }
        }
    }

    // ---- Mouse ----

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Settings dragging
        if (settingsTarget != null && my >= settingsY && my < settingsY + 13
            && mx >= settingsX && mx < settingsX + 120) {
            settingsDragging = true;
            settingsDragOffX = (int)(mx - settingsX);
            settingsDragOffY = (int)(my - settingsY);
            return true;
        }

        // Settings interaction
        if (settingsTarget != null && button == 0) {
            if (handleSettingsClick((int) mx, (int) my)) return true;
        }

        // Close settings if click outside
        if (settingsTarget != null && button == 0) {
            settingsTarget = null;
            waitingForBind = false;
        }

        for (Panel p : panels) {
            // Panel header click → collapse / drag start
            if (my >= p.y && my < p.y + Panel.HEADER_H && mx >= p.x && mx < p.x + Panel.W) {
                if (button == 0) {
                    p.dragging = true;
                    p.dragOffX = (int)(mx - p.x);
                    p.dragOffY = (int)(my - p.y);
                } else if (button == 1) {
                    p.collapsed = !p.collapsed;
                }
                return true;
            }

            if (!p.collapsed) {
                List<Module> mods = CombatClient.moduleManager.getByCategory(p.cat);
                for (int i = 0; i < mods.size(); i++) {
                    int ry = p.y + Panel.HEADER_H + i * Panel.ROW_H;
                    if (my >= ry && my < ry + Panel.ROW_H && mx >= p.x && mx < p.x + Panel.W) {
                        Module m = mods.get(i);
                        if (button == 0) {
                            m.toggle();
                        } else if (button == 1) {
                            // Open settings
                            settingsTarget = m;
                            settingsX = (int) mx + 2;
                            settingsY = (int) my - 6;
                            waitingForBind = false;
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @SuppressWarnings("unchecked")
    private boolean handleSettingsClick(int mx, int my) {
        List<Setting<?>> settings = getSettings(settingsTarget);
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            int ry = settingsY + 14 + i * 14;
            if (my < ry || my >= ry + 14) continue;
            if (mx < settingsX || mx >= settingsX + 120) continue;

            switch (s.getType()) {
                case TOGGLE -> {
                    Setting<Boolean> sb = (Setting<Boolean>) s;
                    sb.setValue(!sb.getValue());
                }
                case ENUM -> s.cycleEnum();
                case SLIDER_INT, SLIDER_FLOAT -> {
                    int barX = settingsX + 3;
                    int barW = 120 - 6;
                    double ratio = Math.max(0, Math.min(1.0, (mx - barX) / (double) barW));
                    double val = s.getMin() + ratio * (s.getMax() - s.getMin());
                    if (s.getValue() instanceof Float) ((Setting<Float>) s).setValue((float) val);
                    else ((Setting<Integer>) s).setValue((int) Math.round(val));
                }
            }
            return true;
        }
        // Bind row
        int by = settingsY + 14 + settings.size() * 14;
        if (my >= by && my < by + 13 && mx >= settingsX && mx < settingsX + 120) {
            waitingForBind = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (settingsDragging) {
            settingsX = (int)(mx - settingsDragOffX);
            settingsY = (int)(my - settingsDragOffY);
            return true;
        }
        for (Panel p : panels) {
            if (p.dragging) {
                p.x = (int)(mx - p.dragOffX);
                p.y = (int)(my - p.dragOffY);
                return true;
            }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        settingsDragging = false;
        for (Panel p : panels) p.dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    // ---- Keyboard ----

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForBind && settingsTarget != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                settingsTarget.setKeyBind(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                settingsTarget.setKeyBind(keyCode);
            }
            waitingForBind = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (settingsTarget != null) { settingsTarget = null; return true; }
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---- Helpers ----

    private List<Setting<?>> getSettings(Module m) {
        List<Setting<?>> list = new ArrayList<>();
        for (java.lang.reflect.Field f : m.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            try {
                Object val = f.get(m);
                if (val instanceof Setting<?>) list.add((Setting<?>) val);
            } catch (IllegalAccessException ignored) {}
        }
        return list;
    }
}
