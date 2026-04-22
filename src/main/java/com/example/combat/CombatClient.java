package com.example.combat;

import com.example.combat.event.ClientEventHandler;
import com.example.combat.event.TabListHandler;
import com.example.combat.modules.ModuleManager;
import com.example.combat.render.CustomItemEntityRenderer;
import net.minecraft.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("combatclient")
public class CombatClient {

    public static final String MOD_ID = "combatclient";
    public static final String NAME   = "Combat Client";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public static ModuleManager moduleManager;

    public CombatClient() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        moduleManager = new ModuleManager();

        // Регистрация кастомного рендерера предметов — Forge 1.16.5 API
        RenderingRegistry.registerEntityRenderingHandler(
            EntityType.ITEM,
            CustomItemEntityRenderer::new
        );

        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new TabListHandler());

        // Регистрируем все модули с @SubscribeEvent на FORGE EVENT BUS
        for (com.example.combat.modules.Module m : moduleManager.getModules()) {
            MinecraftForge.EVENT_BUS.register(m);
        }

        LOGGER.info("[CombatClient] Loaded {} modules", moduleManager.getModules().size());
    }
}
