package com.example.combat.modules.hud;

import com.example.combat.CombatClient;
import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.example.combat.modules.combat.KillAura;
import com.example.combat.utils.RenderUtil;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TargetHud extends Module {

    public final Setting<Integer> posX = new Setting<>("PosX", 4).range(0, 1000);
    public final Setting<Integer> posY = new Setting<>("PosY", 100).range(0, 1000);

    // Размер панели
    private static final int W = 130;
    private static final int H = 30;

    private static final int BG_DARK   = 0xCC0A0A0A;
    private static final int BG_LIGHT  = 0xCC141414;
    private static final int COL_NAME  = 0xFFFFFFFF;
    private static final int COL_HP_HI = 0xFF55FF55;
    private static final int COL_HP_MD = 0xFFFFFF55;
    private static final int COL_HP_LO = 0xFFFF5555;
    private static final int ACCENT    = 0xFF00FFAA;

    public TargetHud() {
        super("TargetHud", "Shows info about KillAura target", Category.HUD);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled()) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.player == null || CombatClient.moduleManager == null) return;

        // Получаем текущий таргет из KillAura
        KillAura ka = (KillAura) CombatClient.moduleManager.getByName("KillAura");
        if (ka == null || !ka.isEnabled()) return;

        LivingEntity target = ka.getCurrentTarget();
        if (target == null) return;

        int x = posX.getValue();
        int y = posY.getValue();
        MatrixStack ms = event.getMatrixStack();

        // Фон панели
        RenderUtil.drawRect(ms, x,     y,     W,     H,     BG_DARK);
        RenderUtil.drawRect(ms, x,     y,     W,     1,     ACCENT);   // верхняя полоска
        RenderUtil.drawRect(ms, x,     y + H, W,     1,     0x44FFFFFF); // нижняя

        // Имя цели
        String name = target instanceof PlayerEntity
            ? target.getGameProfile().getName()
            : target.getName().getString();
        if (name.length() > 14) name = name.substring(0, 13) + "…";
        mc.font.drawShadow(ms, name, x + 4, y + 4, COL_NAME);

        // HP
        float hp    = MathHelper.clamp(target.getHealth(), 0, target.getMaxHealth());
        float maxHp = target.getMaxHealth();
        float pct   = hp / maxHp;

        String hpStr = String.format("%.1f / %.0f", hp, maxHp);
        int hpColor  = pct > 0.6f ? COL_HP_HI : pct > 0.3f ? COL_HP_MD : COL_HP_LO;
        mc.font.drawShadow(ms, hpStr, x + 4, y + 15, hpColor);

        // HP бар
        int barX = x + 4;
        int barY = y + H - 5;
        int barW = W - 8;
        RenderUtil.drawRect(ms, barX,     barY, barW,          3, 0x55000000);
        RenderUtil.drawRect(ms, barX,     barY, (int)(barW * pct), 3, hpColor);

        // Дистанция
        float dist = mc.player.distanceTo(target);
        String distStr = String.format("%.1fm", dist);
        mc.font.drawShadow(ms, distStr,
            x + W - mc.font.width(distStr) - 4, y + 4, 0xFFAAAAAA);
    }
}
