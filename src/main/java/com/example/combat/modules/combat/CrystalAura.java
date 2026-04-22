package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CrystalAura — порт с Meteor Client (Fabric → Forge 1.16.5).
 * Автоматически ставит и взрывает кристаллы конца для нанесения урона игрокам.
 */
public class CrystalAura extends Module {

    // ── Режимы ────────────────────────────────────────────────────────
    public enum SwitchMode  { NORMAL, SILENT, NONE }
    public enum RotateMode  { PACKET, CLIENT, NONE }
    public enum SwingMode   { CLIENT, PACKET, NONE }

    // ── Настройки — General ───────────────────────────────────────────
    public final Setting<Float>      targetRange   = new Setting<>("TargetRange",  10.0f).range(0, 16);
    public final Setting<Float>      minDamage     = new Setting<>("MinDamage",    6.0f).range(0, 36);
    public final Setting<Float>      maxSelfDmg    = new Setting<>("MaxSelfDmg",   6.0f).range(0, 36);
    public final Setting<Boolean>    antiSuicide   = new Setting<>("AntiSuicide",  true);
    public final Setting<RotateMode> rotateMode    = new Setting<>("Rotate",       RotateMode.PACKET);
    public final Setting<SwingMode>  swingMode     = new Setting<>("Swing",        SwingMode.CLIENT);

    // ── Настройки — Switch ────────────────────────────────────────────
    public final Setting<SwitchMode> switchMode    = new Setting<>("Switch",       SwitchMode.NORMAL);
    public final Setting<Integer>    switchDelay   = new Setting<>("SwitchDelay",  0).range(0, 10);

    // ── Настройки — Place ─────────────────────────────────────────────
    public final Setting<Boolean>    doPlace       = new Setting<>("Place",        true);
    public final Setting<Integer>    placeDelay    = new Setting<>("PlaceDelay",   0).range(0, 20);
    public final Setting<Float>      placeRange    = new Setting<>("PlaceRange",   4.5f).range(0, 6);
    public final Setting<Float>      placeWallsRange = new Setting<>("PlaceWalls", 4.5f).range(0, 6);

    // ── Настройки — Break ─────────────────────────────────────────────
    public final Setting<Boolean>    doBreak       = new Setting<>("Break",        true);
    public final Setting<Integer>    breakDelay    = new Setting<>("BreakDelay",   0).range(0, 20);
    public final Setting<Float>      breakRange    = new Setting<>("BreakRange",   4.5f).range(0, 6);
    public final Setting<Float>      breakWallsRange = new Setting<>("BreakWalls", 4.5f).range(0, 6);
    public final Setting<Integer>    breakAttempts = new Setting<>("BreakAttempts",2).range(1, 5);

    // ── Состояние ─────────────────────────────────────────────────────
    private int placeTimer  = 0;
    private int breakTimer  = 0;
    private int switchTimer = 0;

    private int  savedSlot   = -1;
    private boolean switched = false;

    // Список ID кристаллов которые мы поставили (для отслеживания)
    private final List<Integer> placedIds = new ArrayList<>();
    // Счётчик попыток взрыва по ID
    private final java.util.Map<Integer, Integer> breakAttemptMap = new java.util.HashMap<>();

    public CrystalAura() {
        super("CrystalAura", "Places and explodes end crystals to damage players", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        placeTimer  = 0;
        breakTimer  = 0;
        switchTimer = 0;
        savedSlot   = -1;
        switched    = false;
        placedIds.clear();
        breakAttemptMap.clear();
    }

    @Override
    public void onDisable() {
        returnSlot();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        // ── Найти ближайшего игрока-цель ──────────────────────────────
        PlayerEntity target = findTarget();
        if (target == null) { returnSlot(); return; }

        // ── AutoSwitch на кристаллы ────────────────────────────────────
        if (switchMode.getValue() != SwitchMode.NONE) {
            if (switchTimer > 0) { switchTimer--; }
            else if (doSwitch()) { switchTimer = switchDelay.getValue(); }
        }

        // Проверяем что в руке кристалл
        boolean hasCrystal = mc.player.inventory.getSelected().getItem() == Items.END_CRYSTAL;

        // ── Break ──────────────────────────────────────────────────────
        if (doBreak.getValue()) {
            if (breakTimer <= 0) {
                EnderCrystalEntity best = findBestCrystal(target);
                if (best != null) {
                    attackCrystal(best);
                    breakTimer = breakDelay.getValue();
                }
            } else breakTimer--;
        }

        // ── Place ──────────────────────────────────────────────────────
        if (doPlace.getValue() && hasCrystal) {
            if (placeTimer <= 0) {
                BlockPos best = findBestPlacement(target);
                if (best != null) {
                    placeCrystal(best);
                    placeTimer = placeDelay.getValue();
                }
            } else placeTimer--;
        }
    }

    // ══ Поиск цели ═══════════════════════════════════════════════════════
    private PlayerEntity findTarget() {
        float r = targetRange.getValue();
        AxisAlignedBB box = mc.player.getBoundingBox().inflate(r, r, r);
        PlayerEntity best = null;
        double bestDist   = Double.MAX_VALUE;

        for (Entity e : mc.level.getEntitiesOfClass(PlayerEntity.class, box, p ->
                p != mc.player && p.isAlive() && !p.isCreative())) {
            double d = mc.player.distanceTo(e);
            if (d < bestDist) { bestDist = d; best = (PlayerEntity) e; }
        }
        return best;
    }

    // ══ Поиск лучшего кристалла для взрыва ═══════════════════════════════
    private EnderCrystalEntity findBestCrystal(PlayerEntity target) {
        float r = breakRange.getValue();
        AxisAlignedBB box = mc.player.getBoundingBox().inflate(r, r, r);

        EnderCrystalEntity best = null;
        float bestDmg = -1;

        for (EnderCrystalEntity crystal :
                mc.level.getEntitiesOfClass(EnderCrystalEntity.class, box, e -> e.isAlive())) {

            double dist = mc.player.distanceTo(crystal);
            boolean canSee = canSee(crystal.position());
            if (!canSee && dist > breakWallsRange.getValue()) continue;
            if (dist > r) continue;

            // Попытки взрыва
            int attempts = breakAttemptMap.getOrDefault(crystal.getId(), 0);
            if (attempts >= breakAttempts.getValue()) continue;

            float dmgTarget = calcExplosionDmg(target, crystal.position());
            float dmgSelf   = calcExplosionDmg(mc.player, crystal.position());

            if (dmgTarget < minDamage.getValue()) continue;
            if (dmgSelf > maxSelfDmg.getValue()) continue;
            if (antiSuicide.getValue() && dmgSelf >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) continue;

            if (dmgTarget > bestDmg) { bestDmg = dmgTarget; best = crystal; }
        }
        return best;
    }

    // ══ Поиск лучшей позиции для установки ═══════════════════════════════
    private BlockPos findBestPlacement(PlayerEntity target) {
        float r = placeRange.getValue();
        BlockPos playerPos = mc.player.blockPosition();

        BlockPos best = null;
        float bestDmg = -1;

        // Перебираем блоки вокруг игрока в радиусе установки
        for (int x = -(int)r; x <= (int)r; x++) {
            for (int y = -(int)r; y <= (int)r; y++) {
                for (int z = -(int)r; z <= (int)r; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);

                    if (!isValidPlacement(pos)) continue;

                    double dist = mc.player.position().distanceTo(
                            new Vector3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
                    if (dist > r) continue;

                    boolean canSee = canSee(new Vector3d(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5));
                    if (!canSee && dist > placeWallsRange.getValue()) continue;

                    // Центр взрыва — 1 блок выше поверхности
                    Vector3d crystalPos = new Vector3d(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5);

                    float dmgTarget = calcExplosionDmg(target, crystalPos);
                    float dmgSelf   = calcExplosionDmg(mc.player, crystalPos);

                    if (dmgTarget < minDamage.getValue()) continue;
                    if (dmgSelf > maxSelfDmg.getValue()) continue;
                    if (antiSuicide.getValue() && dmgSelf >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) continue;

                    if (dmgTarget > bestDmg) { bestDmg = dmgTarget; best = pos; }
                }
            }
        }
        return best;
    }

    // ══ Проверка валидной позиции установки ══════════════════════════════
    private boolean isValidPlacement(BlockPos pos) {
        // Основание должно быть обсидиан или бедрок
        net.minecraft.block.Block base = mc.level.getBlockState(pos).getBlock();
        if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK) return false;

        // Блок над основанием и два выше должны быть воздух
        if (!mc.level.getBlockState(pos.above()).isAir()) return false;
        if (!mc.level.getBlockState(pos.above(2)).isAir()) return false;

        // Не должно быть кристалла уже на этой позиции
        AxisAlignedBB checkBox = new AxisAlignedBB(
                pos.getX(), pos.getY() + 1, pos.getZ(),
                pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1);
        return mc.level.getEntitiesOfClass(EnderCrystalEntity.class, checkBox, e -> true).isEmpty();
    }

    // ══ Установка кристалла ═══════════════════════════════════════════════
    private void placeCrystal(BlockPos pos) {
        if (mc.getConnection() == null) return;

        // Поворот к позиции
        rotateTo(new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));

        BlockRayTraceResult hit = new BlockRayTraceResult(
                new Vector3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5),
                Direction.UP, pos, false);

        mc.getConnection().send(new CPlayerTryUseItemOnBlockPacket(Hand.MAIN_HAND, hit));

        // Swing
        if (swingMode.getValue() == SwingMode.CLIENT) {
            mc.player.swing(Hand.MAIN_HAND);
        } else if (swingMode.getValue() == SwingMode.PACKET) {
            mc.getConnection().send(
                new net.minecraft.network.play.client.CAnimateHandPacket(Hand.MAIN_HAND));
        }
    }

    // ══ Взрыв кристалла ═══════════════════════════════════════════════════
    private void attackCrystal(EnderCrystalEntity crystal) {
        if (mc.getConnection() == null || mc.gameMode == null) return;

        rotateTo(crystal.position());

        mc.gameMode.attack(mc.player, crystal);

        if (swingMode.getValue() == SwingMode.CLIENT) {
            mc.player.swing(Hand.MAIN_HAND);
        } else if (swingMode.getValue() == SwingMode.PACKET) {
            mc.getConnection().send(
                new net.minecraft.network.play.client.CAnimateHandPacket(Hand.MAIN_HAND));
        }

        breakAttemptMap.merge(crystal.getId(), 1, Integer::sum);
    }

    // ══ Поворот к точке ═══════════════════════════════════════════════════
    private void rotateTo(Vector3d target) {
        if (rotateMode.getValue() == RotateMode.NONE || mc.player == null) return;

        Vector3d delta = target.subtract(mc.player.getEyePosition(1f));
        float yaw   = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float pitch = (float) -Math.toDegrees(
                Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        pitch = net.minecraft.util.math.MathHelper.clamp(pitch, -90f, 90f);

        if (rotateMode.getValue() == RotateMode.PACKET && mc.getConnection() != null) {
            mc.getConnection().send(new CPlayerPacket.RotationPacket(yaw, pitch, mc.player.isOnGround()));
        } else if (rotateMode.getValue() == RotateMode.CLIENT) {
            mc.player.yRot = yaw;
            mc.player.xRot = pitch;
        }
    }

    // ══ Расчёт урона от взрыва кристалла ═════════════════════════════════
    /**
     * Упрощённый расчёт урона от взрыва конца (сила 6.0).
     * Учитывает дистанцию, броню и зелья сопротивления.
     * Полный расчёт требует mixins — это достаточно точная аппроксимация.
     */
    private float calcExplosionDmg(LivingEntity entity, Vector3d crystalPos) {
        float power = 6.0f;
        double dist = entity.position().add(0, entity.getBbHeight() * 0.5, 0)
                .distanceTo(crystalPos);
        double maxDist = power * 2.0;

        if (dist > maxDist) return 0f;

        // Базовый урон по формуле взрыва
        double exposure = 1.0 - (dist / maxDist);
        double damage   = (exposure * exposure + exposure) / 2.0 * 7.0 * power + 1.0;

        // Применяем защиту брони (упрощённо)
        float armor = entity.getArmorValue();
        damage = damage * (1.0 - Math.min(20.0, armor) / 25.0);

        // Зелье сопротивления
        net.minecraft.potion.EffectInstance resist =
                entity.getEffect(net.minecraft.potion.Effects.DAMAGE_RESISTANCE);
        if (resist != null) {
            damage *= 1.0 - ((resist.getAmplifier() + 1) * 0.2);
        }

        return (float) Math.max(0, damage);
    }

    // ══ Видимость точки ═══════════════════════════════════════════════════
    private boolean canSee(Vector3d pos) {
        if (mc.level == null || mc.player == null) return false;
        RayTraceContext ctx = new RayTraceContext(
                mc.player.getEyePosition(1f), pos,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, mc.player);
        return mc.level.clip(ctx).getType() != RayTraceResult.Type.BLOCK;
    }

    // ══ AutoSwitch ════════════════════════════════════════════════════════
    private boolean doSwitch() {
        if (mc.player == null) return false;
        if (mc.player.inventory.getSelected().getItem() == Items.END_CRYSTAL) return false;

        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.items.get(i).getItem() == Items.END_CRYSTAL) {
                if (switchMode.getValue() == SwitchMode.SILENT) {
                    if (!switched) { savedSlot = mc.player.inventory.selected; switched = true; }
                    mc.player.inventory.selected = i;
                } else {
                    mc.player.inventory.selected = i;
                }
                return true;
            }
        }
        return false;
    }

    private void returnSlot() {
        if (switched && savedSlot != -1 && mc.player != null) {
            mc.player.inventory.selected = savedSlot;
            switched = false;
            savedSlot = -1;
        }
    }
}
