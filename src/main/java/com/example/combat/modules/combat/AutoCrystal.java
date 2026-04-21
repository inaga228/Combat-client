package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class AutoCrystal extends Module {
    public final Setting<Float>   placeRange = new Setting<>("PlaceRange", 5.0f).range(1, 8);
    public final Setting<Float>   explodeRange = new Setting<>("ExplodeRange", 5.0f).range(1, 8);
    public final Setting<Boolean> autoSwitch = new Setting<>("AutoSwitch", true);
    public final Setting<Integer> delay = new Setting<>("Delay ms", 50).range(0, 500);

    private long lastAction = 0;

    public AutoCrystal() {
        super("AutoCrystal", "Places and explodes end crystals automatically", Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;
        if (System.currentTimeMillis() - lastAction < delay.getValue()) return;

        // Explode existing crystals near enemies
        for (Entity e : mc.world.getAllEntities()) {
            if (!(e instanceof EnderCrystalEntity)) continue;
            if (mc.player.getDistanceSq(e) > explodeRange.getValue() * explodeRange.getValue()) continue;
            mc.playerController.attackEntity(mc.player, e);
            lastAction = System.currentTimeMillis();
            return;
        }
    }
}
