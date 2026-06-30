package dev.gimme.netherreset.gametest;

import com.mojang.authlib.GameProfile;
import dev.gimme.netherreset.Main;
import dev.gimme.netherreset.infrastructure.ConfigTestSupport;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Loader-agnostic game test bodies. Each {@code static void(GameTestHelper)} method is one test;
 * a test passes by calling {@link GameTestHelper#succeed()} and fails by throwing.
 *
 * <p>To add a test: write the method here, then wire it into {@code FabricGameTests} and
 * {@code NeoForgeGameTests}.
 *
 * <p>These drive the live {@link Main#INSTANCE} player handler with a mock player, the same call the
 * loader's dimension-change hook makes in game, and read the result straight off the player's
 * inventory and active effects — so the per-dimension inventory swap and the grace effects run through
 * the real production path.
 */
public final class NetherResetGameTests {

    private NetherResetGameTests() {
    }

    /**
     * The first Nether crossing stashes the Overworld inventory, drops the player into the empty Nether
     * inventory, and grants the configured grace effect; crossing back restores the Overworld inventory.
     */
    public static void firstNetherEntrySwapsInventoryAndAppliesGrace(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));

        // 30s, level 2 — distinct from the shipped default (60s, level 1) so the assertions prove the
        // applied effect tracks the config rather than a baked-in default. Starter items are pinned empty
        // so the empty-inventory assertion proves the swap, not whatever default starter list ships.
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.GRACE_EFFECTS,
                List.of("minecraft:fire_resistance,30,2"));
             var _ = ConfigTestSupport.override(ConfigTestSupport.NETHER_STARTER_ITEMS, List.<String>of())) {
            Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);

            helper.assertTrue(player.getInventory().getItem(0).isEmpty(),
                    "the first Nether entry should start with an empty inventory");

            MobEffectInstance grace = player.getEffect(MobEffects.FIRE_RESISTANCE);
            helper.assertTrue(grace != null, "entering the Nether should grant the configured grace effect");
            helper.assertTrue(grace.getDuration() == 30 * 20 && grace.getAmplifier() == 1,
                    "the grace effect should follow the config (30s, level 2) but was "
                            + grace.getDuration() + " ticks at amplifier " + grace.getAmplifier());

            Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.NETHER, Level.OVERWORLD);
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(0), new ItemStack(Items.DIAMOND, 5)),
                    "returning to the Overworld should restore the stashed inventory");
        }
        helper.succeed();
    }

    /** Each dimension keeps its own inventory: items put down in one are still there after a round trip. */
    public static void dimensionInventoriesStayIsolatedAcrossCrossings(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));

        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);
        player.getInventory().setItem(0, new ItemStack(Items.NETHERITE_SCRAP, 2)); // gathered in the Nether

        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.NETHER, Level.OVERWORLD);
        helper.assertTrue(ItemStack.matches(player.getInventory().getItem(0), new ItemStack(Items.DIAMOND, 5)),
                "the Overworld inventory should be untouched by what happened in the Nether");

        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);
        helper.assertTrue(ItemStack.matches(player.getInventory().getItem(0), new ItemStack(Items.NETHERITE_SCRAP, 2)),
                "the Nether inventory should still hold what was gathered there");
        helper.succeed();
    }

    /**
     * The Nether gets its own Ender Chest: Overworld contents are hidden on the way in (blocking imports), the
     * Nether stash persists across trips, and nothing is auto-merged back — extraction is left to the ritual.
     */
    public static void netherEnderChestIsolatesFromOverworld(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        Container enderChest = player.getEnderChestInventory();
        enderChest.setItem(0, new ItemStack(Items.DIAMOND, 5)); // stored in the Overworld

        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);
        helper.assertTrue(enderChest.getItem(0).isEmpty(),
                "the Overworld Ender Chest contents must not be reachable in the Nether");

        enderChest.setItem(0, new ItemStack(Items.NETHERITE_SCRAP, 2)); // stashed in the Nether

        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.NETHER, Level.OVERWORLD);
        helper.assertTrue(ItemStack.matches(enderChest.getItem(0), new ItemStack(Items.DIAMOND, 5)),
                "the Overworld Ender Chest should come back untouched, with no Nether loot auto-merged in");

        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);
        helper.assertTrue(ItemStack.matches(enderChest.getItem(0), new ItemStack(Items.NETHERITE_SCRAP, 2)),
                "the Nether Ender Chest should still hold what was stashed there last trip");
        helper.succeed();
    }

    /**
     * The recovery ritual: right-clicking with a Recovery Compass spits the whole Nether stash out of the chest
     * as drops, leaves the Overworld contents alone, empties the stash, and does not consume the (reusable) compass.
     */
    public static void recoveryCompassSpitsOutNetherStash(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        Container enderChest = player.getEnderChestInventory();
        enderChest.setItem(0, new ItemStack(Items.DIAMOND, 5)); // Overworld storage that must survive

        stashInNether(player, new ItemStack(Items.NETHERITE_SCRAP, 2));
        helper.assertTrue(ItemStack.matches(enderChest.getItem(0), new ItemStack(Items.DIAMOND, 5)),
                "the Overworld Ender Chest should be back, untouched, after the Nether trip");

        ItemStack compass = new ItemStack(Items.RECOVERY_COMPASS);
        boolean handled = Main.INSTANCE.getPlayerHandler().onUseEnderChest(player, compass, player.blockPosition());
        helper.assertTrue(handled, "using the Recovery Compass on the chest should be taken as a recovery");
        helper.assertTrue(compass.getCount() == 1 && compass.is(Items.RECOVERY_COMPASS),
                "the Recovery Compass is reusable and must not be consumed");
        helper.assertTrue(ItemStack.matches(enderChest.getItem(0), new ItemStack(Items.DIAMOND, 5)),
                "recovery should leave the Overworld Ender Chest contents alone");

        // The spit-out drops only enter the level's entity lookup next tick, so check them (and the now-empty
        // stash) then: a second recovery finds nothing, and re-entering the Nether shows an empty chest.
        helper.runAtTickTime(1, () -> {
            helper.assertTrue(itemEntityNear(player, Items.NETHERITE_SCRAP),
                    "the recovered Nether items should spit out of the chest into the world");
            helper.assertTrue(
                    Main.INSTANCE.getPlayerHandler().onUseEnderChest(player, compass, player.blockPosition()),
                    "a key click is always taken as a recovery attempt, so it consumes the interaction even with "
                            + "an empty stash rather than opening the chest");
            Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);
            helper.assertTrue(enderChest.getItem(0).isEmpty(),
                    "the Nether Ender Chest should be empty again after its stash was recovered");
            helper.succeed();
        });
    }

    /** Recovering with an Echo Shard works too, but consumes exactly one shard from the stack. */
    public static void echoShardRecoveryConsumesTheShard(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        stashInNether(player, new ItemStack(Items.GHAST_TEAR, 4));

        ItemStack shards = new ItemStack(Items.ECHO_SHARD, 3);
        boolean handled = Main.INSTANCE.getPlayerHandler().onUseEnderChest(player, shards, player.blockPosition());
        helper.assertTrue(handled, "using an Echo Shard on the chest should recover the Nether stash");
        helper.assertTrue(shards.getCount() == 2, "recovery by Echo Shard should consume exactly one shard");

        helper.runAtTickTime(1, () -> { // the drops are only queryable next tick
            helper.assertTrue(itemEntityNear(player, Items.GHAST_TEAR),
                    "the recovered Nether items should spit out of the chest into the world");
            helper.succeed();
        });
    }

    /**
     * Recovery is symmetric. Used while isolated in the Nether, the key spills the set-aside <em>Overworld</em>
     * Ender Chest contents (the other side) rather than the Nether stash, and the Overworld chest comes back empty
     * because those items were extracted. (In normal play this can't import items: the key items only exist in the
     * Overworld and can't be carried into the Nether.)
     */
    public static void recoveryFromNetherSideSpitsOutOverworldStash(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        Container enderChest = player.getEnderChestInventory();
        enderChest.setItem(0, new ItemStack(Items.DIAMOND, 5)); // Overworld contents

        // Enter the Nether: the Overworld contents are set aside and the live chest becomes the Nether stash.
        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);
        helper.assertTrue(enderChest.getItem(0).isEmpty(),
                "the Overworld Ender Chest contents should be set aside on the Nether side");

        ItemStack compass = new ItemStack(Items.RECOVERY_COMPASS);
        boolean handled = Main.INSTANCE.getPlayerHandler().onUseEnderChest(player, compass, player.blockPosition());
        helper.assertTrue(handled, "a key click on the Nether side should recover the set-aside Overworld stash");
        helper.assertTrue(compass.getCount() == 1 && compass.is(Items.RECOVERY_COMPASS),
                "the Recovery Compass is reusable and must not be consumed");

        helper.runAtTickTime(1, () -> { // the spit-out drops are only queryable next tick
            helper.assertTrue(itemEntityNear(player, Items.DIAMOND),
                    "recovering on the Nether side should spit the set-aside Overworld items into the world");
            // Heading back to the Overworld now leaves an empty chest: those items were extracted into the Nether.
            Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.NETHER, Level.OVERWORLD);
            helper.assertTrue(enderChest.getItem(0).isEmpty(),
                    "the recovered Overworld items should be gone from the Overworld Ender Chest");
            helper.succeed();
        });
    }

    /**
     * A player who dies in the Nether and respawns in the Overworld never fires a dimension-change event, so the
     * Ender Chest swap-back happens on respawn instead: the Overworld chest returns and the Nether loot is kept
     * in the recoverable stash rather than lost.
     */
    public static void netherEnderRestoredOnRespawnOutOfNether(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        Container enderChest = player.getEnderChestInventory();
        enderChest.setItem(0, new ItemStack(Items.DIAMOND, 5));

        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);
        enderChest.setItem(0, new ItemStack(Items.NETHERITE_SCRAP, 2)); // gathered before dying

        // The mock player stays in the (Overworld) test level, so this stands in for respawning out of the Nether.
        Main.INSTANCE.getPlayerHandler().onPlayerRespawn(player, false);
        helper.assertTrue(ItemStack.matches(enderChest.getItem(0), new ItemStack(Items.DIAMOND, 5)),
                "respawning out of the Nether should restore the Overworld Ender Chest");

        ItemStack compass = new ItemStack(Items.RECOVERY_COMPASS);
        helper.assertTrue(
                Main.INSTANCE.getPlayerHandler().onUseEnderChest(player, compass, player.blockPosition()),
                "the Nether stash gathered before death should still be recoverable");
        helper.runAtTickTime(1, () -> { // the drops are only queryable next tick
            helper.assertTrue(itemEntityNear(player, Items.NETHERITE_SCRAP),
                    "recovering after a Nether death should spit the loot out");
            helper.succeed();
        });
    }

    /**
     * With {@code isolateNetherEnderChest} off, the Ender Chest is vanilla: Overworld contents stay reachable in
     * the Nether, and the recovery ritual is inert.
     */
    public static void enderChestStaysSharedWhenIsolationDisabled(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        Container enderChest = player.getEnderChestInventory();
        enderChest.setItem(0, new ItemStack(Items.DIAMOND, 5));

        try (var _ = ConfigTestSupport.override(ConfigTestSupport.ISOLATE_NETHER_ENDER_CHEST, false)) {
            Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);
            helper.assertTrue(ItemStack.matches(enderChest.getItem(0), new ItemStack(Items.DIAMOND, 5)),
                    "with isolation disabled, the Overworld Ender Chest should stay reachable in the Nether");
            helper.assertFalse(
                    Main.INSTANCE.getPlayerHandler().onUseEnderChest(
                            player, new ItemStack(Items.RECOVERY_COMPASS), player.blockPosition()),
                    "with the feature disabled, the recovery ritual should do nothing");
        }
        helper.succeed();
    }

    /**
     * Dying in the Nether redirects the respawn back into the Nether, at the spot the player last entered it, rather
     * than to the Overworld spawn vanilla computed. The mock player never actually dies, so this drives the redirect
     * decision directly: enter the Nether (recording the entry point), then resolve an Overworld respawn as if the
     * player had died in the Nether.
     */
    public static void netherDeathRespawnsAtEntryPortal(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        // The recorded entry is reused as a Nether position, and the portal-still-standing check reads Nether blocks
        // there. The test region sits below y=0 (valid in the Overworld) but the Nether floor is at y=0, so lift the
        // player to a height that's in-bounds in both before the crossing records it.
        player.snapTo(player.getX(), 64, player.getZ());
        Vec3 entryPos = player.position();

        // Crossing into the Nether records the entry point through the real production hook.
        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);

        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the test server should have a Nether dimension");

        // The redirect only fires while the entry portal still stands, so stand one up at the recorded spot.
        nether.setBlock(BlockPos.containing(entryPos), Blocks.NETHER_PORTAL.defaultBlockState(), Block.UPDATE_CLIENTS);

        TeleportTransition overworldRespawn = transitionInto(helper.getLevel(), new Vec3(0.5, 64, 0.5));
        TeleportTransition resolved =
                Main.INSTANCE.getPlayerHandler().resolveRespawn(player, Level.NETHER, overworldRespawn);

        helper.assertTrue(resolved.newLevel() == nether,
                "dying in the Nether should redirect the respawn into the Nether");
        helper.assertTrue(resolved.position().distanceToSqr(entryPos) < 1.0e-6,
                "the respawn should land at the recorded Nether entry point");
        helper.succeed();
    }

    /**
     * The redirect only holds while the entry portal still stands. With nothing but open Nether at the recorded spot
     * (the portal was broken since the crossing), redirecting there would strand the player, so the respawn falls
     * back to the vanilla Overworld one instead.
     */
    public static void netherDeathWithBrokenPortalKeepsVanillaRespawn(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        player.snapTo(player.getX(), 64, player.getZ()); // a height that's in-bounds in the Nether too (see above)
        Vec3 entryPos = player.position();
        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);

        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the test server should have a Nether dimension");

        // Make sure the entry spot is clear — no portal block to find, standing in for one that's been destroyed.
        nether.setBlock(BlockPos.containing(entryPos), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);

        TeleportTransition overworldRespawn = transitionInto(helper.getLevel(), new Vec3(0.5, 64, 0.5));
        TeleportTransition resolved =
                Main.INSTANCE.getPlayerHandler().resolveRespawn(player, Level.NETHER, overworldRespawn);
        helper.assertTrue(resolved == overworldRespawn,
                "with the entry portal gone, the respawn should fall back to vanilla");
        helper.succeed();
    }

    /** With {@code respawnInNether} off, a Nether death keeps the vanilla (Overworld) respawn untouched. */
    public static void netherDeathRespawnUnchangedWhenDisabled(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);

        TeleportTransition overworldRespawn = transitionInto(helper.getLevel(), new Vec3(0.5, 64, 0.5));
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.RESPAWN_IN_NETHER, false)) {
            TeleportTransition resolved =
                    Main.INSTANCE.getPlayerHandler().resolveRespawn(player, Level.NETHER, overworldRespawn);
            helper.assertTrue(resolved == overworldRespawn,
                    "with the feature off, the vanilla respawn transition should be returned unchanged");
        }
        helper.succeed();
    }

    /** Dying anywhere other than the Nether is left to vanilla, even with a Nether entry on record. */
    public static void deathOutsideNetherKeepsVanillaRespawn(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);

        TeleportTransition overworldRespawn = transitionInto(helper.getLevel(), new Vec3(0.5, 64, 0.5));
        TeleportTransition resolved =
                Main.INSTANCE.getPlayerHandler().resolveRespawn(player, Level.OVERWORLD, overworldRespawn);
        helper.assertTrue(resolved == overworldRespawn,
                "a death outside the Nether should keep the vanilla respawn");
        helper.succeed();
    }

    /** Without a recorded Nether entry (e.g. the player has never been to the Nether), the redirect bows out. */
    public static void netherDeathWithoutEntryKeepsVanillaRespawn(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);

        TeleportTransition overworldRespawn = transitionInto(helper.getLevel(), new Vec3(0.5, 64, 0.5));
        TeleportTransition resolved =
                Main.INSTANCE.getPlayerHandler().resolveRespawn(player, Level.NETHER, overworldRespawn);
        helper.assertTrue(resolved == overworldRespawn,
                "with no recorded entry, the respawn should fall back to vanilla");
        helper.succeed();
    }

    /**
     * A charged Nether respawn anchor still wins: when vanilla already resolves the respawn to the Nether, the
     * redirect leaves it alone instead of dragging the player back to their entry portal.
     */
    public static void netherRespawnAnchorWinsOverEntryPortal(GameTestHelper helper) {
        ServerPlayer player = placeMockPlayer(helper, GameType.SURVIVAL);
        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);

        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the test server should have a Nether dimension");

        TeleportTransition anchorRespawn = transitionInto(nether, new Vec3(8.5, 70, 8.5));
        TeleportTransition resolved =
                Main.INSTANCE.getPlayerHandler().resolveRespawn(player, Level.NETHER, anchorRespawn);
        helper.assertTrue(resolved == anchorRespawn,
                "an existing Nether respawn (e.g. a respawn anchor) should be left untouched");
        helper.succeed();
    }

    /** A plain respawn transition into the given level, standing at {@code pos} and facing north. */
    private static TeleportTransition transitionInto(ServerLevel level, Vec3 pos) {
        return new TeleportTransition(level, pos, Vec3.ZERO, 0f, 0f, TeleportTransition.DO_NOTHING);
    }

    /** Runs a Nether round trip that leaves {@code loot} in the player's recoverable Nether stash. */
    private static void stashInNether(ServerPlayer player, ItemStack loot) {
        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.OVERWORLD, Level.NETHER);
        player.getEnderChestInventory().setItem(0, loot);
        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, Level.NETHER, Level.OVERWORLD);
    }

    /** Whether an item entity holding the given item has been spit out near the player. */
    private static boolean itemEntityNear(ServerPlayer player, Item item) {
        return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8.0))
                .stream().anyMatch(entity -> entity.getItem().is(item));
    }

    /**
     * When a non-player entity is allowed through a Nether portal, {@code clearEntityItemsOnTeleport} wipes
     * everything it carries — both worn/held equipment (a zombie's sword and helmet) and a container
     * inventory (a chest minecart's contents) — so it can't smuggle loot past the per-dimension reset.
     */
    public static void allowedEntityCrossingStripsCarriedItems(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.PREVENT_OTHER_ENTITIES_FROM_TELEPORTING, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.CLEAR_ENTITY_ITEMS_ON_TELEPORT, true)) {

            LivingEntity zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));

            Entity minecart = helper.spawn(EntityTypes.CHEST_MINECART, new BlockPos(3, 2, 1));
            ((Container) minecart).setItem(0, new ItemStack(Items.DIAMOND, 3));

            boolean zombieBlocked =
                    Main.INSTANCE.getEntityHandler().shouldBlockNetherTeleport(zombie, Level.OVERWORLD, Level.NETHER);
            boolean minecartBlocked =
                    Main.INSTANCE.getEntityHandler().shouldBlockNetherTeleport(minecart, Level.OVERWORLD, Level.NETHER);

            helper.assertFalse(zombieBlocked, "with mob teleporting allowed, the zombie should be let through");
            helper.assertFalse(minecartBlocked, "with mob teleporting allowed, the chest minecart should be let through");
            helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(),
                    "the zombie's held weapon should have been wiped on the crossing");
            helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
                    "the zombie's worn armor should have been wiped on the crossing");
            helper.assertTrue(((Container) minecart).getItem(0).isEmpty(),
                    "the chest minecart's contents should have been wiped on the crossing");
        }
        helper.succeed();
    }

    /**
     * With {@code clearEntityItemsOnTeleport} off, an allowed entity keeps what it carries — the wipe is
     * strictly opt-out, so disabling it lets pack animals and the like haul cargo through as before.
     */
    public static void allowedEntityKeepsItemsWhenClearingDisabled(GameTestHelper helper) {
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.PREVENT_OTHER_ENTITIES_FROM_TELEPORTING, false);
             var _ = ConfigTestSupport.override(ConfigTestSupport.CLEAR_ENTITY_ITEMS_ON_TELEPORT, false)) {

            LivingEntity zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));

            boolean blocked =
                    Main.INSTANCE.getEntityHandler().shouldBlockNetherTeleport(zombie, Level.OVERWORLD, Level.NETHER);

            helper.assertFalse(blocked, "with mob teleporting allowed, the zombie should be let through");
            helper.assertTrue(ItemStack.matches(zombie.getItemBySlot(EquipmentSlot.MAINHAND), new ItemStack(Items.DIAMOND_SWORD)),
                    "with item clearing disabled, the zombie should still be holding its weapon");
        }
        helper.succeed();
    }

    /**
     * Creates a mock {@link ServerPlayer} of the given gamemode and places it in the test level with a
     * dummy connection — the non-deprecated replacement for {@code makeMockServerPlayerInLevel()}, which
     * was locked to creative. {@link GameTestHelper#makeMockServerPlayer(GameType)} only builds the player
     * object, so we register it here: the player handler stashes/restores the inventory and applies the
     * grace effect (which sends a packet) straight on the player object, so the player needs a connection.
     */
    private static ServerPlayer placeMockPlayer(GameTestHelper helper, GameType gameType) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(gameType);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        GameProfile profile = player.getGameProfile();
        helper.getLevel().getServer().getPlayerList()
                .placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
        // Anchor the player inside the loaded test region. The recovery tests pop items at player.blockPosition();
        // left at the world origin, those land in an unloaded chunk on loaders that run tests far from origin
        // (NeoForge) and can't be found by getEntitiesOfClass.
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.snapTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
        return player;
    }
}
