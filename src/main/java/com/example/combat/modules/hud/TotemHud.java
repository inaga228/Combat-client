package com.example.combat.modules.hud;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.example.combat.utils.RenderUtil;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.NonNullList;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TotemHud extends Module {

    public final Setting<Integer> posX = new Setting<>("PosX", 4).range(0, 1000);
    public final Setting<Integer> posY = new Setting<>("PosY", 60).range(0, 1000);

    // Цвет: зелёный если > 3, жёлтый если 1-3, красный если 0
    private static final int COL_SAFE  = 0xFF55FF55;
    private static final int COL_LOW   = 0xFFFFFF55;
    private static final int COL_EMPTY = 0xFFFF5555;
    private static final int BG        = 0x80000000;

    public TotemHud() {
        super("TotemHud", "Shows totem count in inventory", Category.HUD);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled()) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.player == null) return;

        int count = countTotems();
        String label = "Totems: ";
        String num   = String.valueOf(count);

        int x = posX.getValue();
        int y = posY.getValue();

        MatrixStack ms = event.getMatrixStack();
        int totalW = mc.font.width(label + num);

        RenderUtil.drawRect(ms, x - 2, y - 1, totalW + 4, 10, BG);

        int numColor = count > 3 ? COL_SAFE : count > 0 ? COL_LOW : COL_EMPTY;

        mc.font.drawShadow(ms, label, x, y, 0xFFFFFFFF);
        mc.font.drawShadow(ms, num,   x + mc.font.width(label), y, numColor);

        // Иконка тотема рядом (16x16 предмет)
        ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);
        mc.getItemRenderer().renderGuiItem(totem, x + totalW + 6, y - 4);
        // Сброс GL состояния после рендера предмета
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    private int countTotems() {
        NonNullList<ItemStack> inv = mc.player.inventory.items;
        int count = 0;
        for (ItemStack stack : inv) {
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                count += stack.getCount();
            }
        }
        // Оффхенд
        for (ItemStack stack : mc.player.inventory.offhand) {
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
