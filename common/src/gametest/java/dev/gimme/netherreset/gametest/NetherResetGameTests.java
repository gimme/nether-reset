package dev.gimme.netherreset.gametest;

import com.mojang.authlib.GameProfile;
import dev.gimme.netherreset.Main;
import dev.gimme.netherreset.infrastructure.ConfigTestSupport;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
