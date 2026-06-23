package dev.gimme.netherreset.gametest;

import com.mojang.authlib.GameProfile;
import dev.gimme.netherreset.Main;
import dev.gimme.netherreset.infrastructure.ConfigTestSupport;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

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
        return player;
    }
}
