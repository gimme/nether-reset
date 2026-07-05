# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [v26.2-0.1.3] - 2026-07-05

### Added

- Added `respawnInNether` config (default on): a player who dies in the Nether respawns back in the Nether, at the spot where they last entered it, instead of being sent to their Overworld spawn — keeping the Nether a committed, one-way challenge. The respawn is redirected the same way a respawn anchor works (the player is placed straight into the Nether, not teleported afterward), and a charged Nether respawn anchor still takes priority. Only redirects while the entry portal is still standing: if it has since been broken, respawning there would strand the player, so it falls back to the vanilla Overworld respawn (as it also does when there's no recorded entry point). Set it to false to always respawn in the Overworld as before.
- Added `cartographerLevelingTradeEnabled` config (default on): an Apprentice (level 2) Cartographer that didn't roll the vanilla Glass Pane → Emerald trade instead gets an equivalent 8 Amethyst Shard → Emerald trade, so it can always be leveled up affordably instead of having to commit to an explorer-map run; set it to false to remove that fallback trade.
- Added `ancientCityMapTradeEnabled` config (default on): Cartographer villagers offer an Ancient City Map pinned to their Expert (level 4) tier, a reliable way to locate Ancient Cities; set it to false to remove those trades.
- Added `clearEntityItemsOnTeleport` config (default on): entities allowed through a Nether portal have their carried items wiped (held/worn equipment plus container inventories like chested horses and chest minecarts), so they can't be used to smuggle items past the inventory reset.
- Added `isolateNetherEnderChest` config (default on): the Nether gets its own Ender Chest so it can only be used to extract items from the Nether, not carry items into it. Your Overworld Ender Chest is out of reach in the Nether; to extract your Nether stash, right-click an Ender Chest in the Overworld with a Recovery Compass (reusable) or Echo Shard (consumed) and the chest opens and the items spill out. The Ancient City Ender Chest is locked: opening it without the key keeps it shut and shows a hint explaining the ritual; every other Ender Chest stays a normal chest.

### Changed

- Updated to Minecraft 26.2

## [v1.21.11-0.1.2] - 2026-02-21

### Added

- Added option to disable loot modifications

### Changed

- Updated loot modifications

## [v1.21.11-0.1.1] - 2026-02-14

### Added

- Added config for starter items when entering the Nether for the first time
- Added grace period with buffs when entering the Nether for the first time
- Added a chance for Fortress chests to have Water Bottles
- Added a chance for Bastion chests to have Melon Slices and Gunpowder
- Added Ender Chest to the second variant of Ancient Cities

### Fixed

- Fixed some edge cases

## [v1.21.11-0.1.0] - 2026-02-08

### Added

- Initial release
