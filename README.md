# Simple Desert Survival — Feature Overview

A NeoForge mod for Minecraft 26.2 that makes desert-only survival actually viable, whether
you're just playing near a desert or generating a world entirely made of it.

## World Generation

- **"Desert Survival" world type.** A new world preset (selectable from the World Type button
  when creating a world) whose overworld is entirely desert on land, but - unlike a vanilla
  single-biome world, which uses the same fixed biome underwater too - still generates proper
  oceans: a shallow `warm_ocean` band (coral, kelp, tropical fish, correct water color) followed
  by `deep_ocean` further from shore (guardians, ocean ravines, monuments). See
  `DesertOceanBiomeSource` for how this works: it's a custom biome source that reads only the
  continentalness climate parameter (not vanilla's full six-parameter system) to decide between
  desert, beach, shallow ocean, and deep ocean.
- **Rare acacia trees in any desert biome** - not just the new world type. Scatters occasional
  acacia trees (reusing vanilla's own tree feature) across desert biomes, gated by a placement
  check for exposed sand so it only ever tries to grow where it can actually survive.

## Survival

- **Dead bushes drop acacia saplings** (37.5% chance, 0 or 1 per break) instead of nothing,
  giving you a way to grow your own acacia trees for wood.
- **Dirt can be crafted** from ingredients a desert actually has: bone meal, gravel, and sand,
  shaped like a small pit being filled in:
  ```
   B
  BGB
   S
  ```
  (bone meal on top of and beside a gravel block, gravel on top of sand) → 1 dirt.

## Technical Notes (for future maintenance)

- NeoForge ships its own datapack override for `minecraft:blocks/dead_bush`'s loot table at a
  pack priority higher than any mod's own data files, so the sapling drop is force-applied in
  code (`onLootTableLoad` in `SimpleDesertSurvival.java`) rather than a plain datapack JSON file,
  same issue and same fix as documented in the Real Stone Age mod.
- The dirt recipe uses a brand-new recipe ID (not overriding an existing vanilla one), so it
  doesn't need that same treatment - a plain datapack JSON file is enough.
- `#minecraft:normal` (the tag that puts a preset in the World Type button's cycle) is additive
  across datapacks, unlike recipes/loot tables, so adding the Desert Survival preset to it is a
  plain tag file, no code-forcing required.

## License

MIT — see [LICENSE](LICENSE). Free to use, fork, and include in modpacks.

Project scaffolding originally generated from the [NeoForge MDK](https://github.com/NeoForged/MDK).
