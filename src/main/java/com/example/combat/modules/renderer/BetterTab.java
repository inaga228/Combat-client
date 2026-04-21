package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.text.*;
import net.minecraft.world.GameType;

/**
 * BetterTab — порт с Meteor Client (Fabric → Forge 1.16.5).
 * Улучшает таблицу игроков: размер, подсветка себя, пинг числом, геймод.
 */
public class BetterTab extends Module {

    public final Setting<Integer> tabSize   = new Setting<>("TabSize",   100).range(1, 400);
    public final Setting<Integer> tabHeight = new Setting<>("ColHeight",  20).range(1, 100);
    public final Setting<Boolean> selfHl    = new Setting<>("HighlightSelf", true);
    public final Setting<Boolean> pingNum   = new Setting<>("PingNumber",    true);
    public final Setting<Boolean> showGm    = new Setting<>("ShowGamemode",  false);

    // Цвет подсветки себя (R, G, B — 0-255)
    public final Setting<Integer> selfR = new Setting<>("SelfR", 250).range(0, 255);
    public final Setting<Integer> selfG = new Setting<>("SelfG", 130).range(0, 255);
    public final Setting<Integer> selfB = new Setting<>("SelfB",  30).range(0, 255);

    public BetterTab() {
        super("BetterTab", "Improves the player tab list", Category.RENDERER);
    }

    /**
     * Генерирует отображаемое имя для игрока в таб-листе.
     * Вызывается из TabListHandler перед рендером каждой строки.
     */
    public ITextComponent getPlayerName(NetworkPlayerInfo info) {
        if (mc.player == null) return fallback(info);

        ITextComponent base = info.getTabListDisplayName();
        if (base == null) base = new StringTextComponent(info.getGameProfile().getName());

        // Убираем ванильное форматирование
        String nameStr = base.getString().replaceAll("§[0-9a-fk-or]", "");

        IFormattableTextComponent result;

        // Подсветка себя
        if (selfHl.getValue()
                && info.getGameProfile().getId().equals(mc.player.getGameProfile().getId())) {
            int packed = (0xFF << 24)
                       | (selfR.getValue() << 16)
                       | (selfG.getValue() << 8)
                       | selfB.getValue();
            // В 1.16.5 TextColor.fromRgb принимает int без альфа
            int rgb = (selfR.getValue() << 16) | (selfG.getValue() << 8) | selfB.getValue();
            result = new StringTextComponent(nameStr)
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
        } else {
            result = new StringTextComponent(nameStr);
        }

        // Геймод рядом с ником
        if (showGm.getValue() && info.getGameMode() != null) {
            String gm;
            GameType gt = info.getGameMode();
            if (gt == GameType.SPECTATOR)  gm = "Sp";
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
        return n != null ? n : new StringTextComponent(info.getGameProfile().getName());
    }
}
