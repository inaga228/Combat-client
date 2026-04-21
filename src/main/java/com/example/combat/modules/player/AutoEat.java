package com.example.combat.modules.player;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AutoEat extends Module {
    public final Setting<Integer> threshold = new Setting<>("Hunger", 16).range(1, 20);

    public AutoEat() {
        super("AutoEat", "Automatically eats food when hunger is low", Category.PLAYER);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;
        if (mc.player.getFoodStats().getFoodLevel() > threshold.getValue()) return;

        // Find food in hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item.isFood()) {
                mc.player.inventory.currentItem = i;
                mc.playerController.processRightClick(mc.player, mc.world, Hand.MAIN_HAND);
                break;
            }
        }
    }
}
