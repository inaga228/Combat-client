package com.example.combat.config;

import com.example.combat.CombatClientMod;
import com.example.combat.modules.building.FastPlaceModule;
import com.example.combat.modules.building.ScaffoldModule;
import com.example.combat.modules.building.TowerModule;
import com.example.combat.modules.building.ClutchSaveModule;
import com.example.combat.modules.combat.AimAssistModule;
import com.example.combat.modules.combat.AutoCritModule;
import com.example.combat.modules.combat.BedwarsModule;
import com.example.combat.modules.combat.TriggerBotModule;
import com.example.combat.modules.client.OptimizationModule;
import com.example.combat.modules.client.PlayerEspModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModuleConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("combat-client.json");

    public static void save() {
        try {
            JsonObject root = new JsonObject();

            JsonObject scaffold = new JsonObject();
            scaffold.addProperty("enabled", ScaffoldModule.enabled);
            scaffold.addProperty("delay", ScaffoldModule.delay);
            scaffold.addProperty("safeWalk", ScaffoldModule.safeWalk);
            scaffold.addProperty("onlyOnGround", ScaffoldModule.onlyOnGround);
            scaffold.addProperty("mode", ScaffoldModule.mode);
            scaffold.addProperty("autoJump", ScaffoldModule.autoJump);
            scaffold.addProperty("eagleSneak", ScaffoldModule.eagleSneak);
            scaffold.addProperty("clutchRescue", ScaffoldModule.clutchRescue);
            scaffold.addProperty("clutchTurnSpeed", ScaffoldModule.clutchTurnSpeed);
            scaffold.addProperty("clutchFallSpeed", ScaffoldModule.clutchFallSpeed);
            root.add("scaffold", scaffold);

            JsonObject tower = new JsonObject();
            tower.addProperty("enabled", TowerModule.enabled);
            tower.addProperty("speed", TowerModule.speed);
            tower.addProperty("legitMovement", TowerModule.legitMovement);
            tower.addProperty("mode", TowerModule.mode);
            root.add("tower", tower);

            JsonObject fastPlace = new JsonObject();
            fastPlace.addProperty("enabled", FastPlaceModule.enabled);
            fastPlace.addProperty("cooldown", FastPlaceModule.cooldown);
            fastPlace.addProperty("onlyBlocks", FastPlaceModule.onlyBlocks);
            root.add("fastPlace", fastPlace);

            JsonObject clutch = new JsonObject();
            clutch.addProperty("enabled", ClutchSaveModule.enabled);
            clutch.addProperty("turnSpeed", ClutchSaveModule.turnSpeed);
            clutch.addProperty("fallSpeed", ClutchSaveModule.fallSpeed);
            root.add("clutchSave", clutch);

            JsonObject optimization = new JsonObject();
            optimization.addProperty("enabled", OptimizationModule.enabled);
            optimization.addProperty("disableParticles", OptimizationModule.disableParticles);
            optimization.addProperty("disableSky", OptimizationModule.disableSky);
            optimization.addProperty("disableClouds", OptimizationModule.disableClouds);
            optimization.addProperty("disableFog", OptimizationModule.disableFog);
            optimization.addProperty("disableWeather", OptimizationModule.disableWeather);
            optimization.addProperty("renderBoost", OptimizationModule.renderBoost);
            optimization.addProperty("disableBlockEntities", OptimizationModule.disableBlockEntities);
            optimization.addProperty("disableEntities", OptimizationModule.disableEntities);
            optimization.addProperty("ultraLowPreset", OptimizationModule.ultraLowPreset);
            root.add("optimization", optimization);

            JsonObject triggerBot = new JsonObject();
            triggerBot.addProperty("enabled", TriggerBotModule.enabled);
            triggerBot.addProperty("cps", TriggerBotModule.cps);
            triggerBot.addProperty("onlyWeapon", TriggerBotModule.onlyWeapon);
            triggerBot.addProperty("playersOnly", TriggerBotModule.playersOnly);
            triggerBot.addProperty("range", TriggerBotModule.range);
            root.add("triggerBot", triggerBot);

            JsonObject aimAssist = new JsonObject();
            aimAssist.addProperty("enabled", AimAssistModule.enabled);
            aimAssist.addProperty("range", AimAssistModule.range);
            aimAssist.addProperty("strength", AimAssistModule.strength);
            aimAssist.addProperty("fov", AimAssistModule.fov);
            aimAssist.addProperty("requireClick", AimAssistModule.requireClick);
            aimAssist.addProperty("playersOnly", AimAssistModule.playersOnly);
            root.add("aimAssist", aimAssist);

            JsonObject bedwars = new JsonObject();
            bedwars.addProperty("enabled", BedwarsModule.enabled);
            bedwars.addProperty("autoSprint", BedwarsModule.autoSprint);
            bedwars.addProperty("edgeStop", BedwarsModule.edgeStop);
            bedwars.addProperty("autoBlockSwap", BedwarsModule.autoBlockSwap);
            bedwars.addProperty("bridgeJumpAssist", BedwarsModule.bridgeJumpAssist);
            root.add("bedwars", bedwars);

            JsonObject playerEsp = new JsonObject();
            playerEsp.addProperty("enabled", PlayerEspModule.enabled);
            playerEsp.addProperty("includeInvisible", PlayerEspModule.includeInvisible);
            playerEsp.addProperty("maxDistance", PlayerEspModule.maxDistance);
            root.add("playerEsp", playerEsp);

            JsonObject autoCrit = new JsonObject();
            autoCrit.addProperty("enabled", AutoCritModule.enabled);
            autoCrit.addProperty("playersOnly", AutoCritModule.playersOnly);
            autoCrit.addProperty("onlyWeapon", AutoCritModule.onlyWeapon);
            autoCrit.addProperty("syncWithTriggerBot", AutoCritModule.syncWithTriggerBot);
            autoCrit.addProperty("jumpCooldownTicks", AutoCritModule.jumpCooldownTicks);
            root.add("autoCrit", autoCrit);

            JsonObject binds = new JsonObject();
            for (int i = 0; i < 10; i++) {
                binds.addProperty(String.valueOf(i), CombatClientMod.getModuleBind(i));
            }
            root.add("binds", binds);

            Files.createDirectories(FILE.getParent());
            Files.write(FILE, GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = GSON.fromJson(new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;

            if (root.has("scaffold")) {
                JsonObject s = root.getAsJsonObject("scaffold");
                if (s.has("enabled")) ScaffoldModule.enabled = s.get("enabled").getAsBoolean();
                if (s.has("delay")) ScaffoldModule.delay = s.get("delay").getAsInt();
                if (s.has("safeWalk")) ScaffoldModule.safeWalk = s.get("safeWalk").getAsBoolean();
                if (s.has("onlyOnGround")) ScaffoldModule.onlyOnGround = s.get("onlyOnGround").getAsBoolean();
                if (s.has("mode")) ScaffoldModule.mode = s.get("mode").getAsInt();
                if (s.has("autoJump")) ScaffoldModule.autoJump = s.get("autoJump").getAsBoolean();
                if (s.has("eagleSneak")) ScaffoldModule.eagleSneak = s.get("eagleSneak").getAsBoolean();
                if (s.has("clutchRescue")) ScaffoldModule.clutchRescue = s.get("clutchRescue").getAsBoolean();
                if (s.has("clutchTurnSpeed")) ScaffoldModule.clutchTurnSpeed = s.get("clutchTurnSpeed").getAsInt();
                if (s.has("clutchFallSpeed")) ScaffoldModule.clutchFallSpeed = s.get("clutchFallSpeed").getAsDouble();
            }

            if (root.has("tower")) {
                JsonObject t = root.getAsJsonObject("tower");
                if (t.has("enabled")) TowerModule.enabled = t.get("enabled").getAsBoolean();
                if (t.has("speed")) TowerModule.speed = t.get("speed").getAsInt();
                if (t.has("legitMovement")) TowerModule.legitMovement = t.get("legitMovement").getAsBoolean();
                if (t.has("mode")) TowerModule.mode = t.get("mode").getAsInt();
            }

            if (root.has("fastPlace")) {
                JsonObject f = root.getAsJsonObject("fastPlace");
                if (f.has("enabled")) FastPlaceModule.enabled = f.get("enabled").getAsBoolean();
                if (f.has("cooldown")) FastPlaceModule.cooldown = f.get("cooldown").getAsInt();
                if (f.has("onlyBlocks")) FastPlaceModule.onlyBlocks = f.get("onlyBlocks").getAsBoolean();
            }

            if (root.has("clutchSave")) {
                JsonObject c = root.getAsJsonObject("clutchSave");
                if (c.has("enabled")) ClutchSaveModule.enabled = c.get("enabled").getAsBoolean();
                if (c.has("turnSpeed")) ClutchSaveModule.turnSpeed = c.get("turnSpeed").getAsInt();
                if (c.has("fallSpeed")) ClutchSaveModule.fallSpeed = c.get("fallSpeed").getAsDouble();
            }

            if (root.has("optimization")) {
                JsonObject o = root.getAsJsonObject("optimization");
                if (o.has("enabled")) OptimizationModule.enabled = o.get("enabled").getAsBoolean();
                if (o.has("disableParticles")) OptimizationModule.disableParticles = o.get("disableParticles").getAsBoolean();
                if (o.has("disableSky")) OptimizationModule.disableSky = o.get("disableSky").getAsBoolean();
                if (o.has("disableClouds")) OptimizationModule.disableClouds = o.get("disableClouds").getAsBoolean();
                if (o.has("disableFog")) OptimizationModule.disableFog = o.get("disableFog").getAsBoolean();
                if (o.has("disableWeather")) OptimizationModule.disableWeather = o.get("disableWeather").getAsBoolean();
                if (o.has("renderBoost")) OptimizationModule.renderBoost = o.get("renderBoost").getAsBoolean();
                if (o.has("disableBlockEntities")) OptimizationModule.disableBlockEntities = o.get("disableBlockEntities").getAsBoolean();
                if (o.has("disableEntities")) OptimizationModule.disableEntities = o.get("disableEntities").getAsBoolean();
                if (o.has("ultraLowPreset")) OptimizationModule.ultraLowPreset = o.get("ultraLowPreset").getAsBoolean();
            }

            if (root.has("triggerBot")) {
                JsonObject t = root.getAsJsonObject("triggerBot");
                if (t.has("enabled")) TriggerBotModule.enabled = t.get("enabled").getAsBoolean();
                if (t.has("cps")) TriggerBotModule.cps = t.get("cps").getAsInt();
                if (t.has("onlyWeapon")) TriggerBotModule.onlyWeapon = t.get("onlyWeapon").getAsBoolean();
                if (t.has("playersOnly")) TriggerBotModule.playersOnly = t.get("playersOnly").getAsBoolean();
                if (t.has("range")) TriggerBotModule.range = t.get("range").getAsDouble();
            }

            if (root.has("aimAssist")) {
                JsonObject a = root.getAsJsonObject("aimAssist");
                if (a.has("enabled")) AimAssistModule.enabled = a.get("enabled").getAsBoolean();
                if (a.has("range")) AimAssistModule.range = a.get("range").getAsDouble();
                if (a.has("strength")) AimAssistModule.strength = a.get("strength").getAsInt();
                if (a.has("fov")) AimAssistModule.fov = a.get("fov").getAsInt();
                if (a.has("requireClick")) AimAssistModule.requireClick = a.get("requireClick").getAsBoolean();
                if (a.has("playersOnly")) AimAssistModule.playersOnly = a.get("playersOnly").getAsBoolean();
            }

            if (root.has("bedwars")) {
                JsonObject b = root.getAsJsonObject("bedwars");
                if (b.has("enabled")) BedwarsModule.enabled = b.get("enabled").getAsBoolean();
                if (b.has("autoSprint")) BedwarsModule.autoSprint = b.get("autoSprint").getAsBoolean();
                if (b.has("edgeStop")) BedwarsModule.edgeStop = b.get("edgeStop").getAsBoolean();
                if (b.has("autoBlockSwap")) BedwarsModule.autoBlockSwap = b.get("autoBlockSwap").getAsBoolean();
                if (b.has("bridgeJumpAssist")) BedwarsModule.bridgeJumpAssist = b.get("bridgeJumpAssist").getAsBoolean();
            }

            if (root.has("playerEsp")) {
                JsonObject p = root.getAsJsonObject("playerEsp");
                if (p.has("enabled")) PlayerEspModule.enabled = p.get("enabled").getAsBoolean();
                if (p.has("includeInvisible")) PlayerEspModule.includeInvisible = p.get("includeInvisible").getAsBoolean();
                if (p.has("maxDistance")) PlayerEspModule.maxDistance = p.get("maxDistance").getAsDouble();
            }

            if (root.has("autoCrit")) {
                JsonObject c = root.getAsJsonObject("autoCrit");
                if (c.has("enabled")) AutoCritModule.enabled = c.get("enabled").getAsBoolean();
                if (c.has("playersOnly")) AutoCritModule.playersOnly = c.get("playersOnly").getAsBoolean();
                if (c.has("onlyWeapon")) AutoCritModule.onlyWeapon = c.get("onlyWeapon").getAsBoolean();
                if (c.has("syncWithTriggerBot")) AutoCritModule.syncWithTriggerBot = c.get("syncWithTriggerBot").getAsBoolean();
                if (c.has("jumpCooldownTicks")) AutoCritModule.jumpCooldownTicks = c.get("jumpCooldownTicks").getAsInt();
            }

            if (root.has("binds")) {
                JsonObject b = root.getAsJsonObject("binds");
                for (int i = 0; i < 10; i++) {
                    if (b.has(String.valueOf(i))) {
                        CombatClientMod.setModuleBind(i, b.get(String.valueOf(i)).getAsInt());
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
