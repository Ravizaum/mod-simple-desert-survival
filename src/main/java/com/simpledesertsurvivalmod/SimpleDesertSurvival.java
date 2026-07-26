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

    // Same shears_dig-priority shadowing issue as dead_bush above, for the two dry grass blocks.
    // Vanilla's short_grass drops wheat seeds at a 0.125 chance when broken without shears/silk touch;
    // dry grass (both blocks) drops seeds at half that (0.0625) instead, otherwise mirroring NeoForge's
    // own shears/silk-touch handling so shears and Silk Touch keep working as normal.
    private static final String SHORT_DRY_GRASS_LOOT_TABLE_JSON = """
            {"type":"minecraft:block","pools":[{"entries":[{"type":"minecraft:alternatives","children":[{"type":"minecraft:item","conditions":[{"condition":"minecraft:any_of","terms":[{"condition":"neoforge:can_item_perform_ability","ability":"shears_dig"},{"condition":"minecraft:match_tool","predicate":{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:silk_touch","levels":{"min":1}}]}}}]}],"name":"minecraft:short_dry_grass"},{"type":"minecraft:item","conditions":[{"condition":"minecraft:random_chance","chance":0.0625}],"functions":[{"function":"minecraft:apply_bonus","enchantment":"minecraft:fortune","formula":"minecraft:uniform_bonus_count","parameters":{"bonusMultiplier":2}},{"function":"minecraft:explosion_decay"}],"name":"minecraft:wheat_seeds"}]}],"rolls":1.0}],"random_sequence":"minecraft:blocks/short_dry_grass"}
            """;

    private static final String TALL_DRY_GRASS_LOOT_TABLE_JSON = """
            {"type":"minecraft:block","pools":[{"entries":[{"type":"minecraft:alternatives","children":[{"type":"minecraft:item","conditions":[{"condition":"minecraft:any_of","terms":[{"condition":"neoforge:can_item_perform_ability","ability":"shears_dig"},{"condition":"minecraft:match_tool","predicate":{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:silk_touch","levels":{"min":1}}]}}}]}],"name":"minecraft:tall_dry_grass"},{"type":"minecraft:item","conditions":[{"condition":"minecraft:random_chance","chance":0.0625}],"functions":[{"function":"minecraft:apply_bonus","enchantment":"minecraft:fortune","formula":"minecraft:uniform_bonus_count","parameters":{"bonusMultiplier":2}},{"function":"minecraft:explosion_decay"}],"name":"minecraft:wheat_seeds"}]}],"rolls":1.0}],"random_sequence":"minecraft:blocks/tall_dry_grass"}
            """;

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        if (!event.getName().getNamespace().equals("minecraft")) {
            return;
        }

        String forcedJson = switch (event.getName().getPath()) {
            case "blocks/dead_bush" -> DEAD_BUSH_LOOT_TABLE_JSON;
            case "blocks/short_dry_grass" -> SHORT_DRY_GRASS_LOOT_TABLE_JSON;
            case "blocks/tall_dry_grass" -> TALL_DRY_GRASS_LOOT_TABLE_JSON;
            default -> null;
        };
        if (forcedJson == null) {
            return;
        }

        var ops = event.getRegistries().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var parsed = net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC.parse(ops, JsonParser.parseString(forcedJson));
        parsed.resultOrPartial(error -> LOGGER.error("Failed to parse forced loot table for {}: {}", event.getName(), error))
                .ifPresent(event::setTable);
    }
}
