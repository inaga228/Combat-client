package com.example.combat.modules.combat;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * KillAura — Forge 1.16.5
 * Логика портирована с Meteor Client (Fabric).
 */
public class KillAura extends Module {

    /** Статик ссылка для Criticals. */
    public static KillAura INSTANCE;

    // ══ Enums ═══════════════════════════════════════════════════════════
    public enum TargetMode  { PLAYERS, MOBS, ALL }
    public enum AimMode     { ALWAYS, ON_HIT, NONE }
    public enum ShieldMode  { IGNORE, BREAK, NONE }
    public enum MobAgeFilter{ BABY, ADULT, BOTH }
    public enum SortPriority { CLOSEST_ANGLE, CLOSEST, LOWEST_HEALTH }
    public enum WeaponMode  { WEAPONS, ALL }

    // ══ Настройки ════════════════════════════════════════════════════════
    // --- Общие ---
    public final Setting<WeaponMode>  weaponMode   = new Setting<>("WeaponMode",   WeaponMode.WEAPONS);
    public final Setting<AimMode>     aimMode      = new Setting<>("AimMode",      AimMode.ALWAYS);
    public final Setting<Boolean>     smoothRot    = new Setting<>("SmoothRot",    true);
    public final Setting<Float>       smoothSpd    = new Setting<>("SmoothSpd",    0.18f).range(0.05, 1.0);
    public final Setting<Boolean>     autoSwitch   = new Setting<>("AutoSwitch",   false);
    public final Setting<Boolean>     swapBack     = new Setting<>("SwapBack",     false);
    public final Setting<ShieldMode>  shieldMode   = new Setting<>("ShieldMode",   ShieldMode.NONE);
    public final Setting<Boolean>     onlyOnClick  = new Setting<>("OnlyOnClick",  false);

    // --- Таргетинг ---
    public final Setting<TargetMode>  targetMode   = new Setting<>("Targets",      TargetMode.PLAYERS);
    public final Setting<Float>       range        = new Setting<>("Range",        4.5f).range(2.0, 6.0);
    public final Setting<Float>       wallsRange   = new Setting<>("WallsRange",   3.5f).range(0.0, 6.0);
    public final Setting<Boolean>     ignoreNamed  = new Setting<>("IgnoreNamed",  false);
    public final Setting<Boolean>     ignorePassive= new Setting<>("IgnorePassive",true);
    public final Setting<Boolean>     ignoreTamed  = new Setting<>("IgnoreTamed",  false);
    public final Setting<Boolean>      onlyOnLook   = new Setting<>("OnlyOnLook",   false);
    public final Setting<Integer>      maxTargets   = new Setting<>("MaxTargets",   1).range(1, 5);
    public final Setting<SortPriority> sortPriority = new Setting<>("SortPriority", SortPriority.CLOSEST_ANGLE);
    public final Setting<MobAgeFilter>passiveAge   = new Setting<>("PassiveAge",   MobAgeFilter.ADULT);
    public final Setting<MobAgeFilter>hostileAge   = new Setting<>("HostileAge",   MobAgeFilter.BOTH);

    // --- Тайминг ---
    public final Setting<Boolean>     pauseOnUse   = new Setting<>("PauseOnUse",   false);
    public final Setting<Boolean>     customDelay  = new Setting<>("CustomDelay",  false);
    public final Setting<Integer>     hitDelayTicks= new Setting<>("HitDelay",     11).range(0, 60);
    public final Setting<Integer>     switchDelay  = new Setting<>("SwitchDelay",  0).range(0, 10);

    // ══ Состояние ════════════════════════════════════════════════════════
    private static final Random RAND = new Random();

    private int  hitTimer      = 0;
    private int  switchTimer   = 0;
    private int  prevSlot      = -1;
    private boolean swapped    = false;

    // Плавный прицел
    private float currentYaw   = 0;
    private float currentPitch = 0;
    private boolean firstTarget = true;

    // ════════════════════════════════════════════════════════════════════
    public KillAura() {
        super("KillAura", "Automatically attacks nearby entities", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        prevSlot   = -1;
        swapped    = false;
        firstTarget = true;
    }

    @Override
    public void onDisable() {
        // Вернуть слот если AutoSwitch + SwapBack
        if (swapBack.getValue() && swapped && prevSlot != -1) {
            mc.player.inventory.selected = prevSlot;
            swapped = false;
        }
        swapped     = false;
        firstTarget = true;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isAlive()) return;
        if (pauseOnUse.getValue() && (mc.player.isUsingItem())) return;
        if (onlyOnClick.getValue() && mc.gameSettings.keyBindAttack.isDown() == false) return;

        // ── OnlyOnLook ──────────────────────────────────────────────
        if (onlyOnLook.getValue() && !(mc.pointedEntity instanceof LivingEntity)) {
            firstTarget = true;
            return;
        }

        // ── Собрать список целей ────────────────────────────────────
        List<LivingEntity> targets = getTargets();
        if (targets.isEmpty()) {
            firstTarget = true;
            return;
        }

        LivingEntity primary = targets.get(0);

        // ── AutoSwitch ──────────────────────────────────────────────
        if (autoSwitch.getValue()) {
            if (!swapped) {
                prevSlot = mc.player.inventory.selected;
                swapped  = true;
            }
            int best = shouldBreakShield(targets)
                    ? getBestAxeSlot()
                    : getBestWeaponSlot();
            if (best != -1) {
                if (mc.player.inventory.selected != best) {
                    mc.player.inventory.selected = best;
                    switchTimer = switchDelay.getValue();
                }
            }
        }

        // Проверка подходящего оружия в руке
        if (!isAcceptableWeapon(mc.player.inventory.getSelected(), targets)) return;

        // ── Вращение ────────────────────────────────────────────────
        if (aimMode.getValue() == AimMode.ALWAYS) {
            rotateTo(primary);
        }

        // ── Задержка переключения слота ─────────────────────────────
        if (switchTimer > 0) {
            switchTimer--;
            return;
        }

        // ── Проверка задержки удара ──────────────────────────────────
        if (!canHit()) return;

        // ── Бить все цели из списка ──────────────────────────────────
        for (LivingEntity target : targets) {
            if (aimMode.getValue() == AimMode.ON_HIT) rotateTo(target);
            // Criticals — отправляем пакеты крита перед ударом
            Criticals crit = Criticals.INSTANCE;
            if (crit == null || !crit.beforeAttack()) {
                mc.playerController.attackEntity(mc.player, target);
                mc.player.swingArm(Hand.MAIN_HAND);
            }
        }
        hitTimer = 0;
    }

    // ══ Логика задержки удара ════════════════════════════════════════════
    private boolean canHit() {
        if (customDelay.getValue()) {
            if (hitTimer < hitDelayTicks.getValue()) {
                hitTimer++;
                return false;
            }
            return true;
        }
        // Vanilla cooldown — ждём полного заряда (как в Meteor)
        return mc.player.getCooledAttackStrength(0.5f) >= 1.0f;
    }

    // ══ Вращение к цели ═════════════════════════════════════════════════
    private void rotateTo(LivingEntity e) {
        // Целим в центр тела
        Vector3d pos   = e.getPositionVec().add(0, e.getHeight() * 0.5, 0);
        Vector3d delta = pos.subtract(mc.player.getEyePosition(1f));
        float tYaw   = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f;
        float tPitch = (float) -Math.toDegrees(
                Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        tPitch = MathHelper.clamp(tPitch, -90f, 90f);

        if (firstTarget) {
            currentYaw   = mc.player.rotationYaw;
            currentPitch = mc.player.rotationPitch;
            firstTarget  = false;
        }

        if (smoothRot.getValue()) {
            float spd = smoothSpd.getValue();
            float dYaw   = MathHelper.wrapDegrees(tYaw   - currentYaw);
            float dPitch = MathHelper.wrapDegrees(tPitch - currentPitch);
            float stepY  = dYaw   * spd + (dYaw   > 0 ? 0.3f : -0.3f) * (1f - spd);
            float stepP  = dPitch * spd + (dPitch > 0 ? 0.3f : -0.3f) * (1f - spd);
            // Человеческий тремор
            stepY += (RAND.nextFloat() - 0.5f) * 0.05f;
            stepP += (RAND.nextFloat() - 0.5f) * 0.03f;
            currentYaw   += stepY;
            currentPitch += stepP;
            currentPitch  = MathHelper.clamp(currentPitch, -90f, 90f);
        } else {
            currentYaw   = tYaw;
            currentPitch = tPitch;
        }

        // GCD коррекция — анти-детект движения мыши
        float div  = 0.15f;
        float gcdY = mc.player.rotationYaw + (float)(Math.round((currentYaw   - mc.player.rotationYaw) / div) * div);
        float gcdP = mc.player.rotationPitch + (float)(Math.round((currentPitch - mc.player.rotationPitch) / div) * div);

        mc.player.rotationYaw     = gcdY;
        mc.player.rotationPitch     = gcdP;
        mc.player.renderYawOffset = gcdY;
        mc.player.rotationYawHead = gcdY;
    }

    // ══ Получение списка целей ═══════════════════════════════════════════
    private List<LivingEntity> getTargets() {
        float r = range.getValue();
        AxisAlignedBB box = mc.player.getBoundingBox().inflate(r, r, r);
        Vector3d eye = mc.player.getEyePosition(1f);

        List<LivingEntity> list = mc.world.getEntitiesWithinAABB(LivingEntity.class, box, e -> entityCheck(e, eye));

        // Сортировка по SortPriority
        switch (sortPriority.getValue()) {
            case CLOSEST:
                list.sort(Comparator.comparingDouble(e -> {
                    AxisAlignedBB hb = e.getBoundingBox();
                    double cx = MathHelper.clamp(mc.player.getX(), hb.minX, hb.maxX);
                    double cy = MathHelper.clamp(mc.player.getY(), hb.minY, hb.maxY);
                    double cz = MathHelper.clamp(mc.player.getZ(), hb.minZ, hb.maxZ);
                    return mc.player.getPositionVec().distanceTo(new Vector3d(cx, cy, cz));
                }));
                break;
            case LOWEST_HEALTH:
                list.sort(Comparator.comparingDouble(e -> e.getHealth()));
                break;
            default:
                list.sort(Comparator.comparingDouble(e -> getAngleTo(e)));
                break;
        }
        // MaxTargets limit
        int max = maxTargets.getValue();
        if (list.size() > max) list = list.subList(0, max);
        return list;
    }

    private double getAngleTo(LivingEntity e) {
        Vector3d pos   = e.getPositionVec().add(0, e.getHeight() * 0.5, 0);
        Vector3d delta = pos.subtract(mc.player.getEyePosition(1f)).normalize();
        Vector3d look  = mc.player.getLookVec();
        return Math.acos(MathHelper.clamp(look.dot(delta), -1.0, 1.0));
    }

    // ══ Проверка цели ════════════════════════════════════════════════════
    private boolean entityCheck(LivingEntity e, Vector3d eye) {
        if (e == mc.player) return false;
        if (!e.isAlive() || e.getHealth() <= 0) return false;

        float r = range.getValue();

        // Проверка дистанции через хитбокс (как в Meteor)
        AxisAlignedBB hitbox = e.getBoundingBox();
        double clampedX = MathHelper.clamp(mc.player.getX(), hitbox.minX, hitbox.maxX);
        double clampedY = MathHelper.clamp(mc.player.getY(), hitbox.minY, hitbox.maxY);
        double clampedZ = MathHelper.clamp(mc.player.getZ(), hitbox.minZ, hitbox.maxZ);
        double dist = mc.player.getPositionVec().distanceTo(new Vector3d(clampedX, clampedY, clampedZ));
        if (dist > r) return false;

        // Фильтр по типу цели
        TargetMode mode = targetMode.getValue();
        boolean isPlayer = e instanceof PlayerEntity;
        boolean isMob    = e instanceof IMob;
        if (mode == TargetMode.PLAYERS && !isPlayer) return false;
        if (mode == TargetMode.MOBS    && !isMob)    return false;

        // Игроки
        if (isPlayer) {
            PlayerEntity player = (PlayerEntity) e;
            if (player.isCreative()) return false;
            if (shieldMode.getValue() == ShieldMode.IGNORE && player.isBlocking()) return false;
        }

        if (ignoreNamed.getValue() && e.hasCustomName()) return false;
        if (ignoreTamed.getValue() && e instanceof TameableEntity) {
            TameableEntity tame = (TameableEntity) e;
            if (tame.getOwner() != null && tame.getOwner().equals(mc.player)) return false;
        }

        // IgnorePassive — не бить нейтралов если они не агрятся
        if (ignorePassive.getValue()) {
            if (e instanceof EndermanEntity && !((EndermanEntity) e).isScreaming()) return false;
            // Forge 1.16.5: пиглины — net.minecraft.entity.monster.piglin
            if (e instanceof net.minecraft.entity.monster.piglin.AbstractPiglinEntity
                    && !((net.minecraft.entity.monster.piglin.AbstractPiglinEntity) e).isAggressive()) return false;
            if (e instanceof ZombifiedPiglinEntity && !((ZombifiedPiglinEntity) e).isAngry()) return false;
            if (e instanceof WolfEntity && !((WolfEntity) e).isAngry()) return false;
        }

        // Фильтр возраста мобов
        if (e instanceof ZombieEntity
                || e instanceof net.minecraft.entity.monster.piglin.AbstractPiglinEntity
                || e instanceof HoglinEntity || e instanceof ZoglinEntity) {
            MobAgeFilter af = hostileAge.getValue();
            if (af == MobAgeFilter.BABY   && !e.isBaby()) return false;
            if (af == MobAgeFilter.ADULT  &&  e.isBaby()) return false;
        } else if (e instanceof AnimalEntity || e instanceof net.minecraft.entity.AgeableEntity) {
            MobAgeFilter af = passiveAge.getValue();
            if (af == MobAgeFilter.BABY   && !e.isBaby()) return false;
            if (af == MobAgeFilter.ADULT  &&  e.isBaby()) return false;
        }

        // Сквозь стены — wallsRange
        boolean canSee = canSeeEntity(e, eye);
        if (!canSee && dist > wallsRange.getValue()) return false;

        return true;
    }

    // ══ Видимость ═══════════════════════════════════════════════════════
    private boolean canSeeEntity(LivingEntity e, Vector3d eye) {
        float h = e.getHeight();
        Vector3d[] pts = {
            e.getPositionVec().add(0, h * 0.9, 0),
            e.getPositionVec().add(0, h * 0.5, 0),
            e.getPositionVec().add(0, h * 0.1, 0)
        };
        for (Vector3d pt : pts) {
            RayTraceContext ctx = new RayTraceContext(eye, pt,
                    RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.NONE, mc.player);
            if (mc.world.clip(ctx).getType() != RayTraceResult.Type.BLOCK)
                return true;
        }
        return false;
    }

    // ══ Проверка щита у кого-либо из целей ═══════════════════════════════
    private boolean shouldBreakShield(List<LivingEntity> targets) {
        if (shieldMode.getValue() != ShieldMode.BREAK) return false;
        for (LivingEntity t : targets) {
            if (t instanceof PlayerEntity && ((PlayerEntity) t).isBlocking()) return true;
        }
        return false;
    }

    // ══ Поиск слотов оружия ═════════════════════════════════════════════
    private boolean isAcceptableWeapon(ItemStack stack, List<LivingEntity> targets) {
        if (shouldBreakShield(targets)) return stack.getItem() instanceof AxeItem;
        if (weaponMode.getValue() == WeaponMode.ALL) return true;
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem
                || item instanceof PickaxeItem || item instanceof ShovelItem
                || item instanceof HoeItem    || item instanceof TridentItem;
    }

    private int getBestWeaponSlot() {
        int best = -1;
        double bestDmg = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.inventory.items.get(i);
            Item item = s.getItem();
            double dmg = 0;
            if (item instanceof SwordItem)  dmg = ((SwordItem) item).getDamage() + 2;
            else if (item instanceof AxeItem)     dmg = s.getAttributeModifiers(net.minecraft.inventory.EquipmentSlotType.MAINHAND)
                    .get(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                    .stream().mapToDouble(m -> m.getAmount()).sum();
            if (dmg > bestDmg) { bestDmg = dmg; best = i; }
        }
        return best;
    }

    public LivingEntity getCurrentTarget() {
        if (mc.player == null || mc.world == null) return null;
        java.util.List<LivingEntity> t = getTargets();
        return t.isEmpty() ? null : t.get(0);
    }

    private int getBestAxeSlot() {
        int best = -1;
        double bestDmg = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.inventory.items.get(i);
            if (s.getItem() instanceof AxeItem) {
                double dmg = s.getAttributeModifiers(net.minecraft.inventory.EquipmentSlotType.MAINHAND)
                        .get(net.minecraft.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                        .stream().mapToDouble(m -> m.getAmount()).sum();
                if (dmg > bestDmg) { bestDmg = dmg; best = i; }
            }
        }
        return best;
    }
}
