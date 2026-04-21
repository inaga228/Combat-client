package com.example.combat;

import com.example.combat.modules.ModuleManager;
import com.example.combat.event.ClientEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("combatclient")
public class CombatClient {

    public static final String MOD_ID = "combatclient";
    public static final String NAME = "Combat Client";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public static ModuleManager moduleManager;

    public CombatClient() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        moduleManager = new ModuleManager();
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        LOGGER.info("[CombatClient] Loaded {} modules", moduleManager.getModules().size());
    }
}
