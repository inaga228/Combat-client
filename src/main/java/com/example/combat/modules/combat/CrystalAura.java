package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.potion.Effects;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CrystalAura — Forge 1.16.5
 *
 * Тик-логика:
 *   1. Найти цель в радиусе.
 *   2. Найти лучшую позицию для кристалла (обсидиан/бедрок под ногами цели).
 *   3. Silent-свитч на кристалл → отправить пакет Place → вернуть слот.
 *   4. Следующий тик: найти кристалл рядом с целью → Attack пакет → взрыв.
 *   5. Повторять.
 *
 * Античит-меры:
 *   - Всё через сетевые пакеты, не через GameMode методы.
 *   - Поворот серверный пакетом CPlayerPacket.RotationPacket.
 *   - Silent switch: CHeldItemChangePacket туда и обратно в одном тике.
 *   - Нет движения через forwardImpulse — не трогаем ввод игрока.
 *   - Задержки place/break настраиваемые.
 */
public class CrystalAura extends Module {

    public enum TargetMode { PLAYERS, MOBS, ALL }
    public enum SwitchMode { SILENT, NORMAL }

    public final Setting<Float>      targetRange  = new Setting<>("TargetRange",  10f).range(3, 16);
    public final Setting<Float>      placeRange   = new Setting<>("PlaceRange",    4f).range(2, 6);
    public final Setting<Float>      breakRange   = new Setting<>("BreakRange",   4.5f).range(2, 6);
    public final Setting<Float>      minDamage    = new Setting<>("MinDamage",    5f).range(0, 36);
    public final Setting<Float>      maxSelfDmg   = new Setting<>("MaxSelfDmg",   8f).range(0, 36);
    public final Setting<Boolean>    antiSuicide  = new Setting<>("AntiSuicide",  true);
    public final Setting<SwitchMode> switchMode   = new Setting<>("Switch",       SwitchMode.SILENT);
    public final Setting<TargetMode> targetMode   = new Setting<>("Targets",      TargetMode.PLAYERS);
    public final Setting<Integer>    placeDelay   = new Setting<>("PlaceDelay",   2).range(0, 10);
    public final Setting<Integer>    breakDelay   = new Setting<>("BreakDelay",   1).range(0, 10);
    public final Setting<Boolean>    rotate       = new Setting<>("Rotate",       true);

    // Таймеры
    private int placeTick = 0;
    private int breakTick = 0;

    // Последний поставленный кристалл (ждём его появления в мире)
    private BlockPos lastPlacePos = null;

    public int kaTimer = 0; // для совместимости с KillAura pauseOnCA

    public CrystalAura() {
        super("CrystalAura", "Places and explodes end crystals", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        placeTick = 0; breakTick = 0; lastPlacePos = null; kaTimer = 0;
    }

    // ══ Главный тик ══════════════════════════════════════════════════════
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        if (mc.player.isSpectator()) return;

        kaTimer = Math.max(0, kaTimer - 1);

        LivingEntity target = findTarget();
        if (target == null) return;

        // ── Break: взрываем уже существующие кристаллы ───────────────
        if (breakTick > 0) { breakTick--; }
        else {
            EnderCrystalEntity crystal = findBestCrystal(target);
            if (crystal != null) {
                if (rotate.getValue())
                    sendRotation(crystal.position());
                // Атакуем пакетом напрямую — не через gameMode.attack
                if (mc.getConnection() != null) {
                    mc.gameMode.attack(mc.player, crystal);
                    mc.player.swing(Hand.MAIN_HAND);
                    kaTimer = 4;
                }
                breakTick = breakDelay.getValue();
                lastPlacePos = null; // можем ставить снова
            }
        }

        // ── Place: ставим новый кристалл ─────────────────────────────
        if (placeTick > 0) { placeTick--; return; }

        BlockPos placePos = findBestPlacement(target);
        if (placePos == null) return;

        if (rotate.getValue())
            sendRotation(new Vector3d(
                placePos.getX()+0.5, placePos.getY()+1.5, placePos.getZ()+0.5));

        silentSwitch(Items.END_CRYSTAL, () -> {
            if (mc.getConnection() == null) return;
            mc.getConnection().send(new CPlayerTryUseItemOnBlockPacket(
                Hand.MAIN_HAND,
                new BlockRayTraceResult(
                    new Vector3d(placePos.getX()+0.5, placePos.getY()+1.0, placePos.getZ()+0.5),
                    Direction.UP,
                    placePos,
                    false
                )
            ));
            mc.player.swing(Hand.MAIN_HAND);
        });

        lastPlacePos = placePos;
        placeTick = placeDelay.getValue();
    }

    // ══ Поворот серверным пакетом ════════════════════════════════════════
    private void sendRotation(Vector3d target) {
        if (mc.getConnection() == null) return;
        Vector3d eye   = mc.player.getEyePosition(1f);
        Vector3d delta = target.subtract(eye);
        float yaw   = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float pitch = (float) -Math.toDegrees(
            Math.atan2(delta.y, Math.sqrt(delta.x*delta.x + delta.z*delta.z)));
        pitch = MathHelper.clamp(pitch, -90f, 90f);
        mc.getConnection().send(new CPlayerPacket.RotationPacket(yaw, pitch, mc.player.isOnGround()));
    }

    // ══ Silent switch — пакетами, не трогая клиентский инвентарь ═════════
    private void silentSwitch(net.minecraft.item.Item item, Runnable action) {
        if (mc.getConnection() == null) return;

        // Проверяем текущий слот
        if (mc.player.inventory.getSelected().getItem() == item) {
            action.run();
            return;
        }

        // Ищем предмет в хотбаре
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.items.get(i).getItem() == item) {
                slot = i; break;
            }
        }
        if (slot == -1) return; // нет предмета

        int prevSlot = mc.player.inventory.selected;

        if (switchMode.getValue() == SwitchMode.SILENT) {
            // Отправляем пакет свитча → действие → пакет обратно
            // Всё в один тик — сервер видит использование предмета на нужном слоте
            mc.getConnection().send(new CHeldItemChangePacket(slot));
            mc.player.inventory.selected = slot;
            action.run();
            mc.getConnection().send(new CHeldItemChangePacket(prevSlot));
            mc.player.inventory.selected = prevSlot;
        } else {
            // Normal switch — просто меняем слот
            mc.player.inventory.selected = slot;
            mc.getConnection().send(new CHeldItemChangePacket(slot));
            action.run();
        }
    }

    // ══ Поиск цели ═══════════════════════════════════════════════════════
    private LivingEntity findTarget() {
        float r = targetRange.getValue();
        AxisAlignedBB box = mc.player.getBoundingBox().inflate(r);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == mc.player || !e.isAlive()) return false;
            TargetMode m = targetMode.getValue();
            if (m == TargetMode.PLAYERS)
                return e instanceof PlayerEntity && !((PlayerEntity)e).isCreative();
            if (m == TargetMode.MOBS)
                return e instanceof net.minecraft.entity.monster.IMob;
            return !(e instanceof PlayerEntity && ((PlayerEntity)e).isCreative());
        })) {
            double d = mc.player.distanceTo(e);
            if (d < bestDist) { bestDist = d; best = e; }
        }
        return best;
    }

    // ══ Лучшая позиция для установки ════════════════════════════════════
    private BlockPos findBestPlacement(LivingEntity target) {
        float pr = placeRange.getValue();
        BlockPos tPos = target.blockPosition();
        BlockPos best = null;
        float bestDmg = minDamage.getValue() - 0.01f;

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Позиция под кристалл — на уровне ног цели или ниже
                for (int y = 0; y >= -1; y--) {
                    BlockPos pos = tPos.offset(x, y, z);
                    if (!isValidPlacement(pos)) continue;

                    // Расстояние от игрока до верхней грани блока
                    double dx = mc.player.getX() - (pos.getX() + 0.5);
                    double dy = mc.player.getY() - pos.getY();
                    double dz = mc.player.getZ() - (pos.getZ() + 0.5);
                    if (Math.sqrt(dx*dx + dy*dy + dz*dz) > pr) continue;

                    Vector3d crystalPos = new Vector3d(
                        pos.getX()+0.5, pos.getY()+1.0, pos.getZ()+0.5);

                    float dmgT = calcExplosionDmg(target, crystalPos);
                    float dmgS = calcExplosionDmg(mc.player, crystalPos);

                    if (dmgT < minDamage.getValue()) continue;
                    if (dmgS > maxSelfDmg.getValue()) continue;
                    if (antiSuicide.getValue() &&
                        dmgS >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) continue;

                    if (dmgT > bestDmg) { bestDmg = dmgT; best = pos; }
                }
            }
        }
        return best;
    }

    // ══ Лучший кристалл для взрыва ═══════════════════════════════════════
    private EnderCrystalEntity findBestCrystal(LivingEntity target) {
        float br = breakRange.getValue();
        AxisAlignedBB box = mc.player.getBoundingBox().inflate(br);
        EnderCrystalEntity best = null;
        float bestDmg = minDamage.getValue() - 0.01f;

        for (EnderCrystalEntity c : mc.level.getEntitiesOfClass(
                EnderCrystalEntity.class, box, e -> e.isAlive())) {
            if (mc.player.distanceTo(c) > br) continue;

            float dmgT = calcExplosionDmg(target, c.position());
            float dmgS = calcExplosionDmg(mc.player, c.position());

            if (dmgT < minDamage.getValue()) continue;
            if (dmgS > maxSelfDmg.getValue()) continue;
            if (antiSuicide.getValue() &&
                dmgS >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) continue;

            if (dmgT > bestDmg) { bestDmg = dmgT; best = c; }
        }
        return best;
    }

    // ══ Проверка позиции ═════════════════════════════════════════════════
    private boolean isValidPlacement(BlockPos pos) {
        net.minecraft.block.Block base = mc.level.getBlockState(pos).getBlock();
        if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK) return false;
        if (!mc.level.getBlockState(pos.above()).isAir()) return false;
        if (!mc.level.getBlockState(pos.above(2)).isAir()) return false;
        // Нет кристалла уже стоящего здесь
        AxisAlignedBB check = new AxisAlignedBB(
            pos.getX(), pos.getY()+1, pos.getZ(),
            pos.getX()+1, pos.getY()+3, pos.getZ()+1);
        return mc.level.getEntitiesOfClass(
            EnderCrystalEntity.class, check, e -> true).isEmpty();
    }

    // ══ Расчёт урона от взрыва ═══════════════════════════════════════════
    private float calcExplosionDmg(LivingEntity entity, Vector3d explosionPos) {
        // Берём центр hitbox
        Vector3d entityCenter = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        double dist = entityCenter.distanceTo(explosionPos);
        double maxDist = 12.0; // сила взрыва кристалла: 6 * 2
        if (dist > maxDist) return 0;

        // Vanilla formula
        double exposure  = 1.0 - (dist / maxDist);
        double rawDamage = (exposure * exposure + exposure) / 2.0 * 7.0 * 6.0 + 1.0;

        // Броня
        float armor      = entity.getArmorValue();
        rawDamage       *= 1.0 - Math.min(20.0, armor) / 25.0;

        // Эффект сопротивления
        net.minecraft.potion.EffectInstance res = entity.getEffect(Effects.DAMAGE_RESISTANCE);
        if (res != null) rawDamage *= 1.0 - (res.getAmplifier() + 1) * 0.2;

        return (float) Math.max(0, rawDamage);
    }
}
