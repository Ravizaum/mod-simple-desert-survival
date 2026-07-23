package com.simpledesertsurvivalmod;

import org.slf4j.Logger;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SimpleDesertSurvival.MODID)
public class SimpleDesertSurvival {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "simpledesertsurvival";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold custom BiomeSource types (referenced from worldgen/world_preset JSON)
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister.create(Registries.BIOME_SOURCE, MODID);

    // A biome source for the "Desert Survival" world preset - see DesertOceanBiomeSource for why this
    // exists instead of just using the vanilla "fixed" single-biome source.
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<DesertOceanBiomeSource>> DESERT_OCEAN_BIOME_SOURCE =
            BIOME_SOURCES.register("desert_ocean", () -> DesertOceanBiomeSource.CODEC);

    public SimpleDesertSurvival(IEventBus modEventBus) {
        // Register the Deferred Register to the mod event bus so the custom biome source type gets registered
        BIOME_SOURCES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);
    }

    // NeoForge ships its own datapack override for dead_bush's loot table (same "shears_dig"-priority
    // shadowing issue documented in Real Stone Age's onModifyRecipeJsons/onLootTableLoad), at a pack
    // priority higher than any mod's own data files - so a plain data/minecraft/loot_table JSON file
    // here would get silently overwritten. Forced back to our intended content here instead.
    private static final String DEAD_BUSH_LOOT_TABLE_JSON = """
            {"type":"minecraft:block","pools":[{"entries":[{"type":"minecraft:item","conditions":[{"condition":"minecraft:random_chance","chance":0.375}],"functions":[{"function":"minecraft:explosion_decay"}],"name":"minecraft:acacia_sapling"}],"rolls":1.0}],"random_sequence":"minecraft:blocks/dead_bush"}
            """;

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        if (!event.getName().getNamespace().equals("minecraft") || !event.getName().getPath().equals("blocks/dead_bush")) {
            return;
        }

        var ops = event.getRegistries().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var parsed = net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC.parse(ops, JsonParser.parseString(DEAD_BUSH_LOOT_TABLE_JSON));
        parsed.resultOrPartial(error -> LOGGER.error("Failed to parse forced dead_bush loot table: {}", error))
                .ifPresent(event::setTable);
    }
}
