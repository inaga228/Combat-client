package com.example.combat.modules.renderer;

import com.example.combat.modules.Module;
import com.example.combat.modules.Setting;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * HandView — порт с Meteor Client (Fabric → Forge 1.16.5).
 */
public class HandView extends Module {

    public enum SwingMode { NONE, OFFHAND, MAINHAND }

    public final Setting<SwingMode> swingMode  = new Setting<>("SwingMode",  SwingMode.NONE);
    public final Setting<Integer>   swingSpeed = new Setting<>("SwingSpeed", 6).range(0, 20);
    public final Setting<Boolean>   oldAnims   = new Setting<>("OldAnims",   false);

    // Главная рука
    public final Setting<Float> mainPosX   = new Setting<>("MainPosX",   0.0f).range(-3f, 3f);
    public final Setting<Float> mainPosY   = new Setting<>("MainPosY",   0.0f).range(-3f, 3f);
    public final Setting<Float> mainPosZ   = new Setting<>("MainPosZ",   0.0f).range(-3f, 3f);
    public final Setting<Float> mainRotX   = new Setting<>("MainRotX",   0.0f).range(-180f, 180f);
    public final Setting<Float> mainRotY   = new Setting<>("MainRotY",   0.0f).range(-180f, 180f);
    public final Setting<Float> mainRotZ   = new Setting<>("MainRotZ",   0.0f).range(-180f, 180f);
    public final Setting<Float> mainScaleX = new Setting<>("MainScaleX", 1.0f).range(0.1f, 3f);
    public final Setting<Float> mainScaleY = new Setting<>("MainScaleY", 1.0f).range(0.1f, 3f);
    public final Setting<Float> mainScaleZ = new Setting<>("MainScaleZ", 1.0f).range(0.1f, 3f);

    // Оффхенд
    public final Setting<Float> offPosX   = new Setting<>("OffPosX",   0.0f).range(-3f, 3f);
    public final Setting<Float> offPosY   = new Setting<>("OffPosY",   0.0f).range(-3f, 3f);
    public final Setting<Float> offPosZ   = new Setting<>("OffPosZ",   0.0f).range(-3f, 3f);
    public final Setting<Float> offRotX   = new Setting<>("OffRotX",   0.0f).range(-180f, 180f);
    public final Setting<Float> offRotY   = new Setting<>("OffRotY",   0.0f).range(-180f, 180f);
    public final Setting<Float> offRotZ   = new Setting<>("OffRotZ",   0.0f).range(-180f, 180f);
    public final Setting<Float> offScaleX = new Setting<>("OffScaleX", 1.0f).range(0.1f, 3f);
    public final Setting<Float> offScaleY = new Setting<>("OffScaleY", 1.0f).range(0.1f, 3f);
    public final Setting<Float> offScaleZ = new Setting<>("OffScaleZ", 1.0f).range(0.1f, 3f);

    public HandView() {
        super("HandView", "Alters position, rotation and scale of your hands", Category.RENDERER);
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (!isEnabled()) return;

        
        MatrixStack ms = event.getMatrixStack();
        Hand hand = event.getHand();

        if (hand == Hand.MAIN_HAND) {
            applyTransform(ms,
                mainPosX.getValue(),   mainPosY.getValue(),   mainPosZ.getValue(),
                mainRotX.getValue(),   mainRotY.getValue(),   mainRotZ.getValue(),
                mainScaleX.getValue(), mainScaleY.getValue(), mainScaleZ.getValue());
        } else {
            applyTransform(ms,
                offPosX.getValue(),   offPosY.getValue(),   offPosZ.getValue(),
                offRotX.getValue(),   offRotY.getValue(),   offRotZ.getValue(),
                offScaleX.getValue(), offScaleY.getValue(), offScaleZ.getValue());
        }

        if (swingMode.getValue() != SwingMode.NONE && mc.player != null) {
            if (swingMode.getValue() == SwingMode.OFFHAND) {
                mc.player.attackAnim = mc.player.getAttackStrengthScale(1f);
            }
        }
    }

    private void applyTransform(MatrixStack ms,
                                float px, float py, float pz,
                                float rx, float ry, float rz,
                                float sx, float sy, float sz) {
        if (rx != 0) ms.mulPose(Vector3f.XP.rotationDegrees(rx));
        if (ry != 0) ms.mulPose(Vector3f.YP.rotationDegrees(ry));
        if (rz != 0) ms.mulPose(Vector3f.ZP.rotationDegrees(rz));
        if (sx != 1f || sy != 1f || sz != 1f) ms.scale(sx, sy, sz);
        if (px != 0 || py != 0 || pz != 0)    ms.translate(px, py, pz);
    }

    public int getSwingSpeed()    { return isEnabled() ? swingSpeed.getValue() : 6; }
    public boolean oldAnimations(){ return isEnabled() && oldAnims.getValue(); }
}
