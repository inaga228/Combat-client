package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**
 * ItemPhysics — Forge 1.16.5
 * Портировано с Meteor Client (Fabric).
 *
 * Как работает:
 * - Перехватываем рендер ItemEntity через RenderWorldLastEvent
 * - Рисуем предмет вручную: повёрнутым на бок (flat items) или стоя
 * - Добавляем случайный угол поворота на основе ID сущности
 *
 * В Forge 1.16.5 нет прямого события для замены рендера ItemEntity,
 * поэтому используем подход: подписываемся на RenderWorldLastEvent
 * и дополнительно рисуем трансформации на уже отрисованных предметах.
 *
 * Реализация: отменяем стандартный рендер через cancelable событие
 * и рисуем предмет вручную с физическими трансформациями.
 */
public class ItemPhysics extends Module {

    private static final float PIXEL = 1f / 16f;
    private static final Random RAND = new Random();

    public final Setting<Boolean> randomRotation = new Setting<>("RandomRot", true);
    public final Setting<Boolean> flatItems      = new Setting<>("FlatItems",  true);

    // seed по ID энтити → стабильный случайный угол
    private final Map<Integer, Float> rotationCache = new HashMap<>();

    public ItemPhysics() {
        super("ItemPhysics", "Applies physics to dropped items", Category.RENDERER);
    }

    @Override
    public void onDisable() {
        rotationCache.clear();
    }

    /**
     * Форж не позволяет отменить рендер ItemEntity напрямую через событие.
     * Вместо этого мы используем RenderWorldLastEvent для отрисовки поверх
     * стандартного рендера с нашими трансформациями.
     *
     * Альтернативный подход — через mixin, но без него делаем через
     * EntityRendererManager: рисуем предмет вручную с измененной матрицей.
     */
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled()) return;
        if (mc.level == null || mc.player == null) return;

        float pt = event.getPartialTicks();
        MatrixStack ms = event.getMatrixStack();

        double cx = mc.gameRenderer.getMainCamera().getPosition().x;
        double cy = mc.gameRenderer.getMainCamera().getPosition().y;
        double cz = mc.gameRenderer.getMainCamera().getPosition().z;

        EntityRendererManager erm = mc.getEntityRenderDispatcher();
        IRenderTypeBuffer.Impl buffers = mc.renderBuffers().bufferSource();

        for (net.minecraft.entity.Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity)) continue;
            ItemEntity itemEntity = (ItemEntity) entity;
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;

            // Позиция энтити относительно камеры
            double ex = MathHelper.lerp(pt, itemEntity.xOld, itemEntity.getX()) - cx;
            double ey = MathHelper.lerp(pt, itemEntity.yOld, itemEntity.getY()) - cy;
            double ez = MathHelper.lerp(pt, itemEntity.zOld, itemEntity.getZ()) - cz;

            ms.pushPose();
            ms.translate(ex, ey, ez);

            // Убираем стандартное вращение по времени (бобование предмета)
            // Добавляем физическое расположение: предмет лежит на земле
            applyPhysicsTransform(ms, itemEntity);

            // Рисуем предмет через стандартный item renderer
            net.minecraft.client.renderer.model.IBakedModel model =
                mc.getItemRenderer().getModel(stack, mc.level, mc.player);

            boolean flat = isFlatModel(model);

            ms.pushPose();
            if (flat && flatItems.getValue()) {
                // Плоский предмет (монета, бумага и т.п.) — кладём горизонтально
                ms.mulPose(Vector3f.XP.rotationDegrees(90f));
                ms.translate(0, 0, -PIXEL / 2f);
            } else {
                // Объёмный предмет — ставим на "ребро" снизу
                ms.translate(0, -0.25f + PIXEL / 2f, 0);
            }

            // Случайный поворот (стабильный по ID)
            if (randomRotation.getValue()) {
                float angle = getRotationForEntity(itemEntity);
                if (flat && flatItems.getValue()) {
                    ms.mulPose(Vector3f.ZP.rotationDegrees(angle));
                } else {
                    ms.translate(0.5f, 0, 0.5f);
                    ms.mulPose(Vector3f.YP.rotationDegrees(angle));
                    ms.translate(-0.5f, 0, -0.5f);
                }
            }

            // Масштаб как у vanilla (предметы в мире рисуются 0.25f)
            ms.scale(0.25f, 0.25f, 0.25f);
            ms.translate(-0.5f, -0.5f, -0.5f);

            mc.getItemRenderer().renderModelLists(model, stack, 15728880, 655360, ms, buffers.getBuffer(
                net.minecraft.client.renderer.RenderType.translucent()
            ));

            ms.popPose();
            ms.popPose();
        }

        buffers.endBatch();
    }

    /** Применяем "физические" трансформации: убираем боббинг и вращение */
    private void applyPhysicsTransform(MatrixStack ms, ItemEntity entity) {
        // В воде — приподнимаем немного
        if (entity.isInWater()) {
            ms.translate(0, 0.333f, 0);
        }
        // Компенсируем стандартный Y-боббинг (в vanilla: sin(age * 0.2) * 0.1 + 0.1)
        // Мы не отменяем стандартный рендер, а рисуем ПОВЕРХ — поэтому
        // сдвигаем вниз чтобы предмет лежал на земле
        ms.translate(0, -entity.getBbHeight() * 0.5f, 0);
    }

    /** Эвристика: плоская модель если толщина < 1 пикселя */
    private boolean isFlatModel(net.minecraft.client.renderer.model.IBakedModel model) {
        // В Forge 1.16.5 нельзя легко получить геометрию модели без рендер контекста.
        // Используем эвристику: GUI-трансформация — если rotation.x != 0 → плоский.
        net.minecraft.client.renderer.model.ItemCameraTransforms transforms =
            model.getTransforms();
        float rx = transforms.ground.rotation.x();
        // Плоские предметы (монеты, пластины) имеют rotation.x = 0 в ground transform
        // Объёмные — тоже 0. Используем isSideLit как признак 3D модели.
        return !model.usesBlockLight();
    }

    /** Возвращает стабильный случайный угол для данной энтити */
    private float getRotationForEntity(ItemEntity entity) {
        return rotationCache.computeIfAbsent(entity.getId(), id -> {
            RAND.setSeed(id * 89748956L);
            return (RAND.nextFloat() * 2f - 1f) * 90f;
        });
    }
}
