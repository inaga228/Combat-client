package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Criticals — порт с Meteor Client (Fabric → Forge 1.16.5).
 * Отправляет пакеты движения чтобы удар считался критическим.
 */
public class Criticals extends Module {

    public static Criticals INSTANCE;

    public enum Mode { PACKET, JUMP, MINI_JUMP, NONE }

    public final Setting<Mode>    mode    = new Setting<>("Mode", Mode.PACKET);
    public final Setting<Boolean> onlyKA  = new Setting<>("OnlyKA", false);

    // Состояние для JUMP / MINI_JUMP
    private boolean   pending    = false;
    private boolean   waitPeak   = false;
    private double    lastY      = 0;
    private int       sendTimer  = 0;

    public Criticals() {
        super("Criticals", "Performs critical hits on attack", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    public void onEnable()  { pending = false; waitPeak = false; }
    @Override
    public void onDisable() { pending = false; waitPeak = false; }

    /**
     * Вызывается из KillAura прямо перед attackEntity().
     * Возвращает true если нужно отложить реальный удар (JUMP / MINI_JUMP).
     */
    public boolean beforeAttack() {
        if (!isEnabled() || mode.getValue() == Mode.NONE) return false;
        if (!canCrit()) return false;

        switch (mode.getValue()) {
            case PACKET:
                sendPosition(0.0625);
                sendPosition(0.0);
                return false; // атака сразу после пакетов
            case MINI_JUMP:
                if (!pending) {
                    pending   = true;
                    sendTimer = 4;
                    sendPosition(0.25);
                    sendPosition(0.0);
                }
                return pending; // пока pending — откладываем удар
            case JUMP:
                if (!pending) {
                    pending  = true;
                    waitPeak = true;
                    lastY    = mc.player.getY();
                    if (mc.player.isOnGround()) mc.player.jumpFromGround();
                }
                return pending;
            default:
                return false;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!isEnabled() || mc.player == null || !pending) return;

        if (mode.getValue() == Mode.JUMP && waitPeak) {
            double y = mc.player.getY();
            if (y <= lastY) { waitPeak = false; sendTimer = 0; }
            lastY = y;
            return;
        }

        if (sendTimer > 0) {
            sendTimer--;
        } else {
            // Теперь выполняем реальный удар через KillAura
            KillAura ka = KillAura.INSTANCE;
            if (ka != null && ka.isEnabled()) {
                // KillAura.forceAttack() будет вызвана на следующем тике
                // Просто снимаем флаг — KillAura сама ударит
                pending = false;
            } else {
                pending = false;
            }
        }
    }

    /** Можно ли сделать крит прямо сейчас. */
    private boolean canCrit() {
        if (mc.player == null) return false;
        // Нельзя критовать в воде, лаве, на лестнице, при полёте
        if (!mc.player.isOnGround()) return false;
        if (mc.player.isInWater() || mc.player.isInLava()) return false;
        if (mc.player.onClimbable()) return false;
        if (mc.player.isSprinting() && mode.getValue() == Mode.JUMP) return false;
        return true;
    }

    /**
     * Отправляет пакет позиции со сдвигом по Y — то же что делает Meteor.
     */
    private void sendPosition(double yOffset) {
        if (mc.player == null || mc.getConnection() == null) return;
        double x = mc.player.getX();
        double y = mc.player.getY() + yOffset;
        double z = mc.player.getZ();
        mc.getConnection().send(new net.minecraft.network.play.client.CPlayerPacket.PositionPacket(x, y, z, false));
    }

}
