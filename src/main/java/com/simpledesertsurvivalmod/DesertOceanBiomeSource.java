package com.simpledesertsurvivalmod;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

// A biome source for a "desert only" world that still generates proper oceans - with coral, kelp,
// fish, and the right water color - instead of the vanilla single-biome fixed source, which puts
// the *same* fixed biome under the water too, so none of that ever gets a chance to place.
//
// Rather than replicating vanilla's full six-parameter climate system (temperature, humidity,
// continentalness, erosion, depth, weirdness) used by MultiNoiseBiomeSource, this only looks at
// continentalness, using the same land/coast/ocean/deep-ocean thresholds vanilla's own
// OverworldBiomeBuilder uses. Unlike vanilla - which falls back to plain warm_ocean for its "deep"
// band too, since deep_warm_ocean doesn't exist - this deliberately uses plain deep_ocean out past
// the shallow band, trading temperature consistency for deep-ocean content (monuments, guardians,
// ravines) that a real desert coastline would never otherwise get.
public class DesertOceanBiomeSource extends BiomeSource {
    private static final float DEEP_OCEAN_CONTINENTALNESS = -0.455F;
    private static final float OCEAN_CONTINENTALNESS = -0.19F;
    private static final float COAST_CONTINENTALNESS = -0.11F;

    public static final MapCodec<DesertOceanBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.fieldOf("desert").forGetter(source -> source.desert),
            Biome.CODEC.fieldOf("beach").forGetter(source -> source.beach),
            Biome.CODEC.fieldOf("ocean").forGetter(source -> source.ocean),
            Biome.CODEC.fieldOf("deep_ocean").forGetter(source -> source.deepOcean)
    ).apply(instance, DesertOceanBiomeSource::new));

    private final Holder<Biome> desert;
    private final Holder<Biome> beach;
    private final Holder<Biome> ocean;
    private final Holder<Biome> deepOcean;

    public DesertOceanBiomeSource(Holder<Biome> desert, Holder<Biome> beach, Holder<Biome> ocean, Holder<Biome> deepOcean) {
        this.desert = desert;
        this.beach = beach;
        this.ocean = ocean;
        this.deepOcean = deepOcean;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(this.desert, this.beach, this.ocean, this.deepOcean);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        float continentalness = Climate.unquantizeCoord(sampler.sample(quartX, quartY, quartZ).continentalness());
        if (continentalness < DEEP_OCEAN_CONTINENTALNESS) {
            return this.deepOcean;
        }
        if (continentalness < OCEAN_CONTINENTALNESS) {
            return this.ocean;
        }
        return continentalness < COAST_CONTINENTALNESS ? this.beach : this.desert;
    }
}
