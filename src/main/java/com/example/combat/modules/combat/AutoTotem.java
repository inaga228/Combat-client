package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.NonNullList;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * AutoTotem — порт с Meteor Client (Fabric → Forge 1.16.5).
 * Автоматически кладёт тотем в оффхенд.
 */
public class AutoTotem extends Module {

    public enum Mode { SMART, STRICT }

    public final Setting<Mode>    mode   = new Setting<>("Mode",   Mode.SMART);
    public final Setting<Integer> health = new Setting<>("Health", 10).range(0, 36);
    public final Setting<Integer> delay  = new Setting<>("Delay",  0).range(0, 20);
    public final Setting<Boolean> elytra = new Setting<>("Elytra", true);
    public final Setting<Boolean> fall   = new Setting<>("Fall",   true);

    private int ticks = 0;

    public AutoTotem() {
        super("AutoTotem", "Automatically equips a totem in your offhand", Category.COMBAT);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!isEnabled() || mc.player == null || mc.playerController == null) return;

        // Уже держит тотем в оффхенде
        if (mc.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) return;

        // Найти тотем в инвентаре (slots 0-35)
        int slot = findTotemSlot();
        if (slot == -1) return;

        // Проверка задержки
        if (ticks < delay.getValue()) { ticks++; return; }
        ticks = 0;

        // Проверка условий
        boolean shouldHold = false;
        if (mode.getValue() == Mode.STRICT) {
            shouldHold = true;
        } else {
            float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (hp <= health.getValue()) shouldHold = true;
            if (elytra.getValue() && mc.player.isFallFlying()) shouldHold = true;
            if (fall.getValue() && mc.player.fallDistance > 3.0f) shouldHold = true;
        }
        if (!shouldHold) return;

        moveToOffhand(slot);
    }

    private int findTotemSlot() {
        NonNullList<ItemStack> items = mc.player.inventory.items;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItem() == Items.TOTEM_OF_UNDYING) return i;
        }
        return -1;
    }

    /**
     * Перемещает тотем из inventory slot (0-35) в оффхенд.
     * Оффхенд в PlayerContainer = slot 45.
     * Используем ClickType.PICKUP (двойной клик): pick up → place in offhand.
     */
    private void moveToOffhand(int invSlot) {
        int containerId = mc.player.containerMenu.containerId;

        // Конвертируем inventory slot в container slot:
        // hotbar 0-8 → container 36-44
        // main inv 9-35 → container 9-35
        int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;

        // Подбираем предмет на курсор
        mc.playerController.handleInventoryMouseClick(containerId, containerSlot,
                0, ClickType.PICKUP, mc.player);

        // Кладём в оффхенд (slot 45)
        mc.playerController.handleInventoryMouseClick(containerId, 45,
                0, ClickType.PICKUP, mc.player);

        // Если что-то осталось на курсоре — кладём обратно
        if (!mc.player.inventory.getCarried().isEmpty()) {
            mc.playerController.handleInventoryMouseClick(containerId, containerSlot,
                    0, ClickType.PICKUP, mc.player);
        }
    }
}
