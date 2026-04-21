package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;

/**
 * BetterTab — порт с Meteor Client (Fabric → Forge 1.16.5).
 */
public class BetterTab extends Module {

    public final Setting<Integer> tabSize   = new Setting<>("TabSize",   100).range(1, 400);
    public final Setting<Integer> tabHeight = new Setting<>("ColHeight",  20).range(1, 100);
    public final Setting<Boolean> selfHl    = new Setting<>("HighlightSelf", true);
    public final Setting<Boolean> pingNum   = new Setting<>("PingNumber",    true);
    public final Setting<Boolean> showGm    = new Setting<>("ShowGamemode",  false);

    public final Setting<Integer> selfR = new Setting<>("SelfR", 250).range(0, 255);
    public final Setting<Integer> selfG = new Setting<>("SelfG", 130).range(0, 255);
    public final Setting<Integer> selfB = new Setting<>("SelfB",  30).range(0, 255);

    public BetterTab() {
        super("BetterTab", "Improves the player tab list", Category.RENDERER);
    }

    public ITextComponent getPlayerName(NetworkPlayerInfo info) {
        if (mc.player == null) return fallback(info);

        // official mappings: getProfile() вместо getGameProfile()
        ITextComponent base = info.getTabListDisplayName();
        if (base == null) base = new StringTextComponent(info.getProfile().getName());

        String nameStr = base.getString().replaceAll("§[0-9a-fk-or]", "");

        IFormattableTextComponent result;

        if (selfHl.getValue()
                && info.getProfile().getId().equals(mc.player.getGameProfile().getId())) {
            int rgb = (selfR.getValue() << 16) | (selfG.getValue() << 8) | selfB.getValue();
            // Forge 1.16.5 official mappings: net.minecraft.util.text.Color.fromRgb()
            result = new StringTextComponent(nameStr)
                    .withStyle(Style.EMPTY.withColor(Color.fromRgb(rgb)));
        } else {
            result = new StringTextComponent(nameStr);
        }

        if (showGm.getValue() && info.getGameMode() != null) {
            String gm;
            GameType gt = info.getGameMode();
            if      (gt == GameType.SPECTATOR)  gm = "Sp";
            else if (gt == GameType.CREATIVE)   gm = "C";
            else if (gt == GameType.ADVENTURE)  gm = "A";
            else                                gm = "S";

            result = result.append(
                new StringTextComponent(" [" + gm + "]")
                    .withStyle(Style.EMPTY.withColor(TextFormatting.DARK_GRAY))
            );
        }

        return result;
    }

    private ITextComponent fallback(NetworkPlayerInfo info) {
        ITextComponent n = info.getTabListDisplayName();
        return n != null ? n : new StringTextComponent(info.getProfile().getName());
    }
}
