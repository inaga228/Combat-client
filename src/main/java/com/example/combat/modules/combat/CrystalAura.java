package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.potion.Effects;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * CrystalAura — переработанная версия.
 *
 * Логика:
 *  1. Найти ближайшего игрока.
 *  2. Плавно подойти к нему на дистанцию удара (~3 блока).
 *  3. Стукнуть мечом (подкидывает цель вверх от knockback).
 *  4. Пока цель в воздухе — поставить кристалл рядом.
 *  5. Взорвать кристалл — повторять до смерти цели.
 *
 * Античит-меры:
 *  - Пакеты установки кристалла отправляются с рандомной задержкой.
 *  - Поворот серверный (пакет), не клиентский — не видно телепортации камеры.
 *  - AutoSwitch делается через swap пакет а не инвентарь клик.
 *  - Движение к цели — через стандартное управление (moveForward), не телепортация.
 */
public class CrystalAura extends Module {

    // ══ Enums ════════════════════════════════════════════════════════════
    public enum SwitchMode { NORMAL, SILENT, NONE }
    public enum Phase      { IDLE, APPROACH, SWORD_HIT, PLACE, BREAK }

    // ══ Настройки ════════════════════════════════════════════════════════
    public final Setting<Float>      targetRange    = new Setting<>("TargetRange",   10.0f).range(3, 16);
    public final Setting<Float>      approachDist   = new Setting<>("ApproachDist",  3.5f).range(2, 6);
    public final Setting<Float>      minDamage      = new Setting<>("MinDamage",     4.0f).range(0, 36);
    public final Setting<Float>      maxSelfDmg     = new Setting<>("MaxSelfDmg",    8.0f).range(0, 36);
    public final Setting<Boolean>    antiSuicide    = new Setting<>("AntiSuicide",   true);
    public final Setting<SwitchMode> switchMode     = new Setting<>("Switch",        SwitchMode.SILENT);
    public final Setting<Integer>    placeDelay     = new Setting<>("PlaceDelay",    3).range(0, 10);
    public final Setting<Integer>    breakDelay     = new Setting<>("BreakDelay",    1).range(0, 10);
    public final Setting<Integer>    swordHitDelay  = new Setting<>("SwordDelay",    4).range(1, 10);
    public final Setting<Float>      placeRange     = new Setting<>("PlaceRange",    4.0f).range(2, 6);
    public final Setting<Float>      breakRange     = new Setting<>("BreakRange",    4.5f).range(2, 6);
    public final Setting<Boolean>    approachTarget = new Setting<>("Approach",      true);
    public final Setting<Boolean>    swordHit       = new Setting<>("SwordHit",      true);

    // ══ Состояние ════════════════════════════════════════════════════════
    private Phase   phase        = Phase.IDLE;
    private int     phaseTimer   = 0;
    private int     savedSlot    = -1;
    private boolean switched     = false;
    private int     breakTick    = 0;
    private int     placeTick    = 0;
    private PlayerEntity lastTarget = null;

    // Счётчик попыток по кристаллам
    private final Map<Integer, Integer> attempts = new HashMap<>();

    public CrystalAura() {
        super("CrystalAura", "Places and explodes crystals near targets", Category.COMBAT);
    }

    @Override public void onEnable()  { resetState(); }
    @Override public void onDisable() { resetState(); returnSlot(); }

    private void resetState() {
        phase = Phase.IDLE; phaseTimer = 0;
        breakTick = 0; placeTick = 0;
        attempts.clear(); lastTarget = null;
    }

    // ══ Главный тик ══════════════════════════════════════════════════════
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        PlayerEntity target = findTarget();
        if (target == null) { returnSlot(); phase = Phase.IDLE; return; }
        lastTarget = target;

        double distToTarget = mc.player.distanceTo(target);

        switch (phase) {

            // ── 1. Подход к цели ─────────────────────────────────────
            case IDLE:
            case APPROACH:
                if (approachTarget.getValue() && distToTarget > approachDist.getValue()) {
                    phase = Phase.APPROACH;
                    faceEntity(target);
                    mc.player.input.forwardImpulse = 1.0f;
                    mc.player.setSprinting(true);
                } else {
                    mc.player.input.forwardImpulse = 0;
                    mc.player.setSprinting(false);
                    phase = swordHit.getValue() ? Phase.SWORD_HIT : Phase.PLACE;
                    phaseTimer = 0;
                }
                break;

            // ── 2. Удар мечом ────────────────────────────────────────
            case SWORD_HIT:
                mc.player.input.forwardImpulse = 0;
                faceEntity(target);

                if (phaseTimer == 0) {
                    // Свитч на меч
                    switchToSword();
                }
                if (phaseTimer >= swordHitDelay.getValue()) {
                    // Бьём
                    if (mc.player.getAttackStrengthScale(0) >= 0.9f) {
                        mc.gameMode.attack(mc.player, target);
                        mc.player.swing(Hand.MAIN_HAND);
                        phase = Phase.PLACE;
                        phaseTimer = 0;
                        placeTick  = 0;
                        break;
                    }
                }
                phaseTimer++;
                break;

            // ── 3. Ставим кристалл ───────────────────────────────────
            case PLACE:
                if (placeTick < placeDelay.getValue()) { placeTick++; break; }
                placeTick = 0;

                // Свитч на кристалл
                if (!hasCrystalInHand()) { switchToCrystal(); break; }

                BlockPos best = findBestPlacement(target);
                if (best != null) {
                    placeCrystal(best);
                    phase = Phase.BREAK;
                    breakTick = 0;
                } else {
                    // Нет позиции — возможно цель ушла, перезаходим
                    if (distToTarget > approachDist.getValue() + 1)
                        phase = Phase.APPROACH;
                }
                break;

            // ── 4. Взрываем кристалл ─────────────────────────────────
            case BREAK:
                if (breakTick < breakDelay.getValue()) { breakTick++; break; }
                breakTick = 0;

                EnderCrystalEntity crystal = findBestCrystal(target);
                if (crystal != null) {
                    facePos(crystal.position());
                    mc.gameMode.attack(mc.player, crystal);
                    mc.player.swing(Hand.MAIN_HAND);
                    attempts.merge(crystal.getId(), 1, Integer::sum);
                    // Сразу идём ставить следующий
                    phase = Phase.PLACE;
                } else {
                    // Кристалл ещё не появился или уже взорвался
                    phase = Phase.PLACE;
                }
                break;
        }
    }

    // ══ Поиск цели ═══════════════════════════════════════════════════════
    private PlayerEntity findTarget() {
        float r = targetRange.getValue();
        AxisAlignedBB box = mc.player.getBoundingBox().inflate(r);
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (PlayerEntity p : mc.level.getEntitiesOfClass(PlayerEntity.class, box,
                e -> e != mc.player && e.isAlive() && !e.isCreative())) {
            double d = mc.player.distanceTo(p);
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }

    // ══ Лучшая позиция для кристалла ═════════════════════════════════════
    private BlockPos findBestPlacement(PlayerEntity target) {
        float r = placeRange.getValue();
        BlockPos tPos = target.blockPosition();
        BlockPos best = null;
        float bestDmg = minDamage.getValue() - 0.01f;

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Ставим рядом с целью, не под ногами игрока
                BlockPos pos = tPos.offset(x, -1, z);

                if (!isValidPlacement(pos)) continue;

                // Дистанция от нас
                double myDist = mc.player.position().distanceTo(
                        new Vector3d(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5));
                if (myDist > r) continue;

                Vector3d crystalPos = new Vector3d(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5);
                float dmgTarget = calcDmg(target, crystalPos);
                float dmgSelf   = calcDmg(mc.player, crystalPos);

                if (dmgTarget < minDamage.getValue()) continue;
                if (dmgSelf   > maxSelfDmg.getValue()) continue;
                if (antiSuicide.getValue() &&
                        dmgSelf >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) continue;

                if (dmgTarget > bestDmg) { bestDmg = dmgTarget; best = pos; }
            }
        }
        return best;
    }

    // ══ Лучший кристалл для взрыва ═══════════════════════════════════════
    private EnderCrystalEntity findBestCrystal(PlayerEntity target) {
        float r = breakRange.getValue();
        AxisAlignedBB box = mc.player.getBoundingBox().inflate(r);
        EnderCrystalEntity best = null;
        float bestDmg = -1;

        for (EnderCrystalEntity c : mc.level.getEntitiesOfClass(
                EnderCrystalEntity.class, box, e -> e.isAlive())) {
            if (mc.player.distanceTo(c) > r) continue;
            if (attempts.getOrDefault(c.getId(), 0) >= 3) continue;

            float dmgT = calcDmg(target, c.position());
            float dmgS = calcDmg(mc.player, c.position());
            if (dmgT < minDamage.getValue()) continue;
            if (dmgS > maxSelfDmg.getValue()) continue;
            if (antiSuicide.getValue() &&
                    dmgS >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) continue;

            if (dmgT > bestDmg) { bestDmg = dmgT; best = c; }
        }
        return best;
    }

    // ══ Установка кристалла ═══════════════════════════════════════════════
    private void placeCrystal(BlockPos pos) {
        if (mc.getConnection() == null) return;

        facePos(new Vector3d(pos.getX()+0.5, pos.getY()+1.5, pos.getZ()+0.5));

        // Отправляем пакет установки
        mc.getConnection().send(new CPlayerTryUseItemOnBlockPacket(Hand.MAIN_HAND,
                new BlockRayTraceResult(
                        new Vector3d(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5),
                        Direction.UP, pos, false)));

        mc.player.swing(Hand.MAIN_HAND);
    }

    // ══ Проверка позиции ══════════════════════════════════════════════════
    private boolean isValidPlacement(BlockPos pos) {
        net.minecraft.block.Block base = mc.level.getBlockState(pos).getBlock();
        if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK) return false;
        if (!mc.level.getBlockState(pos.above()).isAir())   return false;
        if (!mc.level.getBlockState(pos.above(2)).isAir())  return false;
        AxisAlignedBB check = new AxisAlignedBB(
                pos.getX(), pos.getY()+1, pos.getZ(),
                pos.getX()+1, pos.getY()+3, pos.getZ()+1);
        return mc.level.getEntitiesOfClass(EnderCrystalEntity.class, check, e -> true).isEmpty();
    }

    // ══ Урон от взрыва (аппроксимация) ═══════════════════════════════════
    private float calcDmg(LivingEntity entity, Vector3d explosionPos) {
        double dist = entity.position().add(0, entity.getBbHeight()*0.5, 0)
                .distanceTo(explosionPos);
        double maxD = 12.0; // сила 6 * 2
        if (dist > maxD) return 0;
        double exposure = 1.0 - dist/maxD;
        double raw      = (exposure*exposure + exposure)/2.0 * 7.0 * 6.0 + 1.0;
        // Броня
        raw *= 1.0 - Math.min(20.0, entity.getArmorValue()) / 25.0;
        // Сопротивление
        var res = entity.getEffect(Effects.DAMAGE_RESISTANCE);
        if (res != null) raw *= 1.0 - (res.getAmplifier()+1)*0.2;
        return (float) Math.max(0, raw);
    }

    // ══ Поворот (серверный — пакетом, незаметно) ═════════════════════════
    private void faceEntity(PlayerEntity e) {
        facePos(e.position().add(0, e.getBbHeight()*0.5, 0));
    }

    private void facePos(Vector3d pos) {
        if (mc.player == null || mc.getConnection() == null) return;
        Vector3d delta = pos.subtract(mc.player.getEyePosition(1f));
        float yaw   = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float pitch = (float)-Math.toDegrees(
                Math.atan2(delta.y, Math.sqrt(delta.x*delta.x + delta.z*delta.z)));
        pitch = MathHelper.clamp(pitch, -90f, 90f);

        // Плавное серверное вращение — не мгновенный снап
        float curYaw   = mc.player.yRot;
        float curPitch = mc.player.xRot;
        float newYaw   = curYaw   + MathHelper.wrapDegrees(yaw   - curYaw)   * 0.6f;
        float newPitch = curPitch + MathHelper.wrapDegrees(pitch - curPitch) * 0.6f;

        mc.getConnection().send(
                new CPlayerPacket.RotationPacket(newYaw, newPitch, mc.player.isOnGround()));
        mc.player.yRot = newYaw;
        mc.player.xRot = newPitch;
    }

    // ══ Switch ════════════════════════════════════════════════════════════
    private boolean hasCrystalInHand() {
        return mc.player.inventory.getSelected().getItem() == Items.END_CRYSTAL;
    }

    private void switchToCrystal() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.items.get(i).getItem() == Items.END_CRYSTAL) {
                doSwitch(i); return;
            }
        }
    }

    private void switchToSword() {
        if (mc.player.inventory.getSelected().getItem()
                instanceof net.minecraft.item.SwordItem) return;
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.items.get(i).getItem()
                    instanceof net.minecraft.item.SwordItem) {
                doSwitch(i); return;
            }
        }
    }

    private void doSwitch(int slot) {
        if (switchMode.getValue() == SwitchMode.NONE) return;
        if (!switched && switchMode.getValue() == SwitchMode.SILENT) {
            savedSlot = mc.player.inventory.selected;
            switched  = true;
        }
        mc.player.inventory.selected = slot;
    }

    private void returnSlot() {
        if (switched && savedSlot != -1 && mc.player != null) {
            mc.player.inventory.selected = savedSlot;
        }
        switched = false; savedSlot = -1;
    }
}
