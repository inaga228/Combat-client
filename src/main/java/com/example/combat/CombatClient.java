package com.example.combat;

import com.example.combat.event.ClientEventHandler;
import com.example.combat.event.TabListHandler;
import com.example.combat.modules.ModuleManager;
import com.example.combat.modules.combat.AutoTotem;
import com.example.combat.modules.combat.Criticals;
import com.example.combat.modules.combat.CrystalAura;
import com.example.combat.modules.hud.Notifications;
import com.example.combat.modules.player.FastPlace;
import com.example.combat.modules.renderer.HandView;
import net.minecraftforge.common.MinecraftForge;
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

        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new Notifications());
        MinecraftForge.EVENT_BUS.register(new TabListHandler());
        MinecraftForge.EVENT_BUS.register((FastPlace)   moduleManager.getByName("FastPlace"));
        MinecraftForge.EVENT_BUS.register((Criticals)   moduleManager.getByName("Criticals"));
        MinecraftForge.EVENT_BUS.register((AutoTotem)   moduleManager.getByName("AutoTotem"));
        MinecraftForge.EVENT_BUS.register((CrystalAura) moduleManager.getByName("CrystalAura"));
        MinecraftForge.EVENT_BUS.register((HandView)    moduleManager.getByName("HandView"));

        LOGGER.info("[CombatClient] Loaded {} modules", moduleManager.getModules().size());
    }
}
