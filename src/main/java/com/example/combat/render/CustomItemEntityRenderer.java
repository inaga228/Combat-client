package com.example.combat.render;

import com.example.combat.modules.renderer.ItemPhysics;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.ResourceLocation;

/**
 * Заменяет стандартный ItemEntityRenderer.
 * Когда ItemPhysics включён — не рисует ничего (наш рендер в RenderWorldLastEvent).
 * Когда выключен — вызывает super (стандартное поведение).
 */
public class CustomItemEntityRenderer extends EntityRenderer<ItemEntity> {

    public CustomItemEntityRenderer(EntityRendererManager manager) {
        super(manager);
        this.shadowRadius = 0.1f;
    }

    @Override
    public void render(ItemEntity entity, float yaw, float pt,
                       MatrixStack ms, IRenderTypeBuffer buf, int light) {
        // Если ItemPhysics включён — скипаем ванильный рендер
        if (ItemPhysics.INSTANCE != null && ItemPhysics.INSTANCE.isEnabled()) return;
        super.render(entity, yaw, pt, ms, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ItemEntity entity) {
        return new ResourceLocation("textures/misc/white.png");
    }
}
