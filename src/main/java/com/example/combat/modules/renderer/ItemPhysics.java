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
import net.minecraftforge.client.event.RenderItemEntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * ItemPhysics — Forge 1.16.5.
 *
 * Forge предоставляет RenderItemEntityEvent — он вызывается вместо
 * стандартного рендера ItemEntity и позволяет его отменить (event.setCanceled).
 * Мы отменяем ванильный рендер и рисуем предмет сами — плоско, на земле.
 */
public class ItemPhysics extends Module {

    public final Setting<Boolean> randomRotation = new Setting<>("RandomRot", true);

    private final Map<Integer, Float> itemAngles = new HashMap<>();
    private final Random rand = new Random();

    public ItemPhysics() {
        super("ItemPhysics", "Items lie flat on ground instead of spinning", Category.RENDERER);
    }

    @Override
    public void onDisable() { itemAngles.clear(); }

    @SubscribeEvent
    public void onRenderItem(RenderItemEntityEvent event) {
        if (!isEnabled()) return;

        ItemEntity item  = event.getItemEntity();
        ItemStack  stack = item.getItem();
        if (stack.isEmpty()) return;

        // Отменяем ванильный рендер (убирает spinning + bobbbing + дубль)
        event.setCanceled(true);

        MatrixStack       ms  = event.getMatrixStack();
        IRenderTypeBuffer buf = event.getBuffers();
        int light = event.getLight();

        float pt = event.getPartialTick();

        // Интерполированная позиция относительно камеры
        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();
        double rx = MathHelper.lerp(pt, item.xOld, item.getX()) - cam.x;
        double ry = MathHelper.lerp(pt, item.yOld, item.getY()) - cam.y;
        double rz = MathHelper.lerp(pt, item.zOld, item.getZ()) - cam.z;

        IBakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player);

        ms.pushPose();
        ms.translate(rx, ry, rz);

        // Кладём предмет прямо на поверхность BB (убираем ванильный offset +0.25)
        ms.translate(0, item.getBbHeight() * 0.5 - 0.04, 0);

        // Поворачиваем плашмя (XP = вокруг оси X, 90° → горизонтально)
        ms.mulPose(Vector3f.XP.rotationDegrees(90f));

        // Стабильный случайный угол по ID предмета
        if (randomRotation.getValue()) {
            float angle = itemAngles.computeIfAbsent(item.getId(), id -> {
                rand.setSeed(id * 31L * 89748956L);
                return rand.nextFloat() * 360f;
            });
            ms.translate(0.5f, 0.5f, 0f);
            ms.mulPose(Vector3f.ZP.rotationDegrees(angle));
            ms.translate(-0.5f, -0.5f, 0f);
        }

        // Стандартный размер предмета на земле (0.25 единицы — как ванила)
        ms.scale(0.25f, 0.25f, 0.25f);
        ms.translate(-0.5, -0.5, -0.5);

        mc.getItemRenderer().render(
            stack,
            ItemCameraTransforms.TransformType.GROUND,
            false,
            ms,
            buf,
            light,
            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
            model
        );

        ms.popPose();
    }

    @SubscribeEvent
    public void onEntityLeave(EntityLeaveWorldEvent event) {
        itemAngles.remove(event.getEntity().getId());
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        // Сбрасываем угол при повторном спауне того же ID
        if (event.getEntity() instanceof ItemEntity)
            itemAngles.remove(event.getEntity().getId());
    }
}
