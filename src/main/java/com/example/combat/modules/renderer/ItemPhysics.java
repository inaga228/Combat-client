package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * ItemPhysics — Forge 1.16.5
 *
 * В Forge нет события для отмены рендера ItemEntity отдельно.
 * ItemEntity — не LivingEntity, поэтому RenderLivingEvent не подходит.
 *
 * Решение: используем net.minecraftforge.client.event.RenderItemInFrameEvent
 * и для выпавших предметов — подписываемся на специфичный рендер.
 *
 * Для ItemEntity в Forge 1.16.5 единственный способ без mixin —
 * перехватить через RenderWorldLastEvent и нарисовать поверх
 * с правильной трансформацией (предмет лежит на земле).
 *
 * Ключевое отличие от старой версии:
 * - Берём реальную позицию предмета правильно
 * - Не смещаем дважды
 * - Убираем боббинг через translate
 */
public class ItemPhysics extends Module {

    private static final float PIXEL = 1f / 16f;

    public final Setting<Boolean> randomRotation = new Setting<>("RandomRot",  true);
    public final Setting<Boolean> flatMode       = new Setting<>("FlatItems",  true);

    private final Map<Integer, Float> angles = new HashMap<>();
    private final Random rand = new Random();

    public ItemPhysics() {
        super("ItemPhysics", "Items on ground lay flat instead of spinning", Category.RENDERER);
    }

    @Override
    public void onDisable() {
        angles.clear();
    }

    /**
     * Форж 1.16.5 не имеет события для замены рендера ItemEntity без mixin.
     * Реализуем через onUpdate() — рисуем предметы вручную через ItemRenderer
     * поверх мира в каждый тик рендера.
     *
     * Используем RenderWorldLastEvent через ClientEventHandler → onUpdate.
     * Но onUpdate — это тик, а не рендер-фрейм.
     *
     * Правильный подход: регистрируем этот класс на EVENT_BUS и слушаем
     * net.minecraftforge.client.event.RenderWorldLastEvent.
     * ItemEntity НЕ является LivingEntity, поэтому обрабатываем отдельно.
     */
    @SubscribeEvent
    public void onRenderWorld(net.minecraftforge.client.event.RenderWorldLastEvent event) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        float pt = event.getPartialTicks();
        net.minecraft.util.math.vector.Vector3d cam =
            mc.gameRenderer.getMainCamera().getPosition();

        MatrixStack ms = event.getMatrixStack();
        IRenderTypeBuffer.Impl buffers = mc.renderBuffers().bufferSource();

        for (net.minecraft.entity.Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ItemEntity)) continue;
            ItemEntity item = (ItemEntity) e;
            ItemStack stack = item.getItem();
            if (stack.isEmpty()) continue;

            // Интерполированная позиция
            double x = net.minecraft.util.math.MathHelper.lerp(pt, item.xOld, item.getX()) - cam.x;
            double y = net.minecraft.util.math.MathHelper.lerp(pt, item.yOld, item.getY()) - cam.y;
            double z = net.minecraft.util.math.MathHelper.lerp(pt, item.zOld, item.getZ()) - cam.z;

            IBakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player);

            ms.pushPose();
            ms.translate(x, y, z);

            // Убираем стандартный боббинг (vanilla: +0.1 + sin * 0.1)
            // Кладём предмет на землю — сдвиг вниз к нижней границе BB
            float bbH = item.getBbHeight();
            ms.translate(0, -bbH * 0.5 + PIXEL, 0);

            // Вода — приподнимаем
            if (item.isInWater()) ms.translate(0, 0.2, 0);

            boolean flat = flatMode.getValue() && !model.usesBlockLight();

            if (flat) {
                // Плоский предмет (монеты, пластины) — горизонтально
                ms.mulPose(Vector3f.XP.rotationDegrees(90f));
            }

            // Случайный угол поворота — стабильный по ID
            if (randomRotation.getValue()) {
                float angle = angles.computeIfAbsent(item.getId(), id -> {
                    rand.setSeed(id * 89748956L);
                    return (rand.nextFloat() * 2f - 1f) * 90f;
                });

                if (flat) {
                    ms.translate(0.5f, 0.5f, 0);
                    ms.mulPose(Vector3f.ZP.rotationDegrees(angle));
                    ms.translate(-0.5f, -0.5f, 0);
                } else {
                    ms.translate(0.5f, 0, 0.5f);
                    ms.mulPose(Vector3f.YP.rotationDegrees(angle));
                    ms.translate(-0.5f, 0, -0.5f);
                }
            }

            // Стандартный масштаб предмета в мире
            ms.scale(0.25f, 0.25f, 0.25f);
            ms.translate(-0.5, -0.5, -0.5);

            mc.getItemRenderer().render(
                stack,
                ItemCameraTransforms.TransformType.GROUND,
                false,
                ms,
                buffers,
                15728880,  // fullbright
                655360,    // default overlay
                model
            );

            ms.popPose();
        }

        buffers.endBatch();
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        // Чистим кэш мёртвых энтитей чтобы не копить память
        if (event.getEntity() instanceof ItemEntity) {
            angles.remove(event.getEntity().getId());
        }
    }
}
