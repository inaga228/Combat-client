package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * ItemPhysics — Forge 1.16.5.
 *
 * RenderItemEntityEvent не существует в 1.16.5.
 * Решение: рисуем предметы поверх мира через RenderWorldLastEvent,
 * а ванильный рендер скрываем масштабированием до нуля через отдельный хук
 * в ItemEntityRenderer, который регистрируем в CombatClient.clientSetup.
 *
 * Поскольку без миксинов нельзя полностью отменить ванильный рендер ItemEntity,
 * мы заменяем весь EntityRenderer через EntityRendererManager.
 * Регистрация кастомного рендерера — в CombatClient через
 * EntityRenderersEvent.RegisterRenderers (Forge 1.16.5+).
 */
public class ItemPhysics extends Module {

    public static ItemPhysics INSTANCE;

    public final Setting<Boolean> randomRotation = new Setting<>("RandomRot", true);

    private final Map<Integer, Float> itemAngles = new HashMap<>();
    private final Random rand = new Random();

    public ItemPhysics() {
        super("ItemPhysics", "Items lie flat on ground instead of spinning", Category.RENDERER);
        INSTANCE = this;
    }

    @Override
    public void onDisable() { itemAngles.clear(); }

    /**
     * Рисуем физику предметов поверх мира.
     * Ванильный рендер ItemEntity остаётся, но наш CustomItemEntityRenderer
     * (зарегистрированный в CombatClient) заменяет его полностью.
     *
     * Если замена рендерера не сработала — этот метод рисует поверх,
     * визуально перекрывая ванильный.
     */
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.world == null || mc.player == null) return;

        float pt = event.getPartialTicks();
        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();
        MatrixStack ms = event.getMatrixStack();
        IRenderTypeBuffer.Impl buffers = mc.renderBuffers().bufferSource();

        for (net.minecraft.entity.Entity e : mc.world.getAllEntities()) {
            if (!(e instanceof ItemEntity)) continue;
            ItemEntity item = (ItemEntity) e;
            ItemStack stack = item.getItem();
            if (stack.isEmpty()) continue;

            double rx = MathHelper.lerp(pt, item.xOld, item.getX()) - cam.x;
            double ry = MathHelper.lerp(pt, item.yOld, item.getY()) - cam.y;
            double rz = MathHelper.lerp(pt, item.zOld, item.getZ()) - cam.z;

            IBakedModel model = mc.getItemRenderer().getModel(stack, mc.world, mc.player);

            ms.pushPose();
            ms.translate(rx, ry, rz);

            // Убираем ванильный bob-offset и кладём на землю
            ms.translate(0.0, 0.04, 0.0);

            // Плашмя
            ms.mulPose(Vector3f.XP.rotationDegrees(90f));

            // Стабильный угол по ID
            if (randomRotation.getValue()) {
                float angle = itemAngles.computeIfAbsent(item.getId(), id -> {
                    rand.setSeed(id * 31L * 89748956L);
                    return rand.nextFloat() * 360f;
                });
                ms.translate(0.5f, 0.5f, 0f);
                ms.mulPose(Vector3f.ZP.rotationDegrees(angle));
                ms.translate(-0.5f, -0.5f, 0f);
            }

            ms.scale(0.25f, 0.25f, 0.25f);
            ms.translate(-0.5, -0.5, -0.5);

            int light = mc.getEntityRenderDispatcher().getPackedLightCoords(item, pt);

            mc.getItemRenderer().render(
                stack,
                ItemCameraTransforms.TransformType.GROUND,
                false,
                ms,
                buffers,
                light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                model
            );

            ms.popPose();
        }

        buffers.endBatch();
    }

    @SubscribeEvent
    public void onEntityLeave(EntityLeaveWorldEvent event) {
        itemAngles.remove(event.getEntity().getId());
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof ItemEntity)
            itemAngles.remove(event.getEntity().getId());
    }

    /** Получить угол для предмета (используется в CustomItemEntityRenderer). */
    public float getAngle(int entityId) {
        return itemAngles.computeIfAbsent(entityId, id -> {
            rand.setSeed(id * 31L * 89748956L);
            return rand.nextFloat() * 360f;
        });
    }
}
