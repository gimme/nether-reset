package dev.gimme.netherreset.gametest;

import com.mojang.authlib.GameProfile;
import dev.gimme.netherreset.Main;
import dev.gimme.netherreset.application.VillagerHandler;
import dev.gimme.netherreset.infrastructure.ConfigTestSupport;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
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

    // --- Villager bargaining --------------------------------------------------------------------------------------

    /**
     * Bargains rack up <em>unique professions</em>: a different profession adds one (mutually), but re-meeting the same
     * villager, meeting a second villager of an already-bargained profession, or meeting your own profession adds none.
     */
    public static void bargainsAccrueAcrossProfessionsButNotDuplicatesOrSelf(GameTestHelper helper) {
        VillagerHandler villagers = Main.INSTANCE.getVillagerHandler();
        Villager farmer = employ(helper, VillagerProfession.FARMER);
        Villager librarian = employ(helper, VillagerProfession.LIBRARIAN);

        villagers.onSocialize(farmer, librarian);
        helper.assertTrue(villagers.bargainCount(farmer) == 1,
                "a farmer should bargain with a librarian, was " + villagers.bargainCount(farmer));
        helper.assertTrue(villagers.bargainCount(librarian) == 1, "the bargain should be mutual");

        villagers.onSocialize(farmer, librarian);
        helper.assertTrue(villagers.bargainCount(farmer) == 1, "re-meeting the same villager adds nothing");

        villagers.onSocialize(farmer, employ(helper, VillagerProfession.LIBRARIAN));
        helper.assertTrue(villagers.bargainCount(farmer) == 1, "a second librarian is not a new profession");

        villagers.onSocialize(farmer, employ(helper, VillagerProfession.CARTOGRAPHER));
        helper.assertTrue(villagers.bargainCount(farmer) == 2, "a cartographer is a new profession");

        Villager farmer2 = employ(helper, VillagerProfession.FARMER);
        villagers.onSocialize(farmer, farmer2);
        helper.assertTrue(villagers.bargainCount(farmer) == 2, "two farmers have nothing to bargain");
        helper.assertTrue(villagers.bargainCount(farmer2) == 0, "the other farmer gains nothing either");
        helper.succeed();
    }

    /**
     * Plain (professionless) villagers are jack-of-all-trades: each distinct one is a fresh interaction. But they
     * carry their own record, so the same profession can't count a given plain villager twice (whether the same
     * employed villager re-meets it or a different one of that profession does).
     */
    public static void plainVillagersAreWildcardPartners(GameTestHelper helper) {
        VillagerHandler villagers = Main.INSTANCE.getVillagerHandler();
        Villager farmer = employ(helper, VillagerProfession.FARMER);
        Villager plain1 = employ(helper, null);
        Villager plain2 = employ(helper, null);

        villagers.onSocialize(farmer, plain1);
        villagers.onSocialize(farmer, plain2);
        helper.assertTrue(villagers.bargainCount(farmer) == 2,
                "each distinct plain villager counts as unique, was " + villagers.bargainCount(farmer));
        helper.assertTrue(villagers.bargainCount(plain1) == 1, "a plain villager records the profession it met");

        villagers.onSocialize(farmer, plain1); // same farmer, same plain villager
        helper.assertTrue(villagers.bargainCount(farmer) == 2, "the same plain villager can't be counted twice");

        Villager farmer2 = employ(helper, VillagerProfession.FARMER);
        villagers.onSocialize(farmer2, plain1);
        helper.assertTrue(villagers.bargainCount(farmer2) == 0, "a second farmer can't reuse an already-met plain villager");

        Villager librarian = employ(helper, VillagerProfession.LIBRARIAN);
        villagers.onSocialize(librarian, plain1);
        helper.assertTrue(villagers.bargainCount(librarian) == 1, "a different profession can still meet that plain villager");
        helper.assertTrue(villagers.bargainCount(plain1) == 2, "the plain villager now records both professions");

        villagers.onSocialize(plain1, plain2);
        helper.assertTrue(villagers.bargainCount(plain2) == 1, "two plain villagers have nothing to trade");
        helper.succeed();
    }

    /** A villager will not bargain past the configured cap. */
    public static void bargainCapIsRespected(GameTestHelper helper) {
        VillagerHandler villagers = Main.INSTANCE.getVillagerHandler();
        try (var _ = ConfigTestSupport.override(ConfigTestSupport.VILLAGER_BARGAIN_CAP, 2)) {
            Villager farmer = employ(helper, VillagerProfession.FARMER);
            villagers.onSocialize(farmer, employ(helper, VillagerProfession.LIBRARIAN));
            villagers.onSocialize(farmer, employ(helper, VillagerProfession.CARTOGRAPHER));
            helper.assertTrue(villagers.bargainCount(farmer) == 2, "should reach the cap of 2");

            villagers.onSocialize(farmer, employ(helper, VillagerProfession.MASON));
            helper.assertTrue(villagers.bargainCount(farmer) == 2, "should not bargain past the cap");
        }
        helper.succeed();
    }

    /**
     * Trades start empty; with no bargains a restock yields nothing; with bargains a restock tops each trade up toward
     * (but never past) the bargain count.
     */
    public static void tradesStartEmptyAndRestockScalesWithBargains(GameTestHelper helper) {
        VillagerHandler villagers = Main.INSTANCE.getVillagerHandler();

        Villager broke = employ(helper, VillagerProfession.FARMER);
        helper.assertTrue(totalAvailable(broke.getOffers()) == 0, "a freshly hired villager should have no stock");
        broke.restock();
        helper.assertTrue(totalAvailable(broke.getOffers()) == 0, "with no bargains, restock should add no stock");

        Villager farmer = employ(helper, VillagerProfession.FARMER);
        villagers.onSocialize(farmer, employ(helper, VillagerProfession.LIBRARIAN));
        villagers.onSocialize(farmer, employ(helper, VillagerProfession.CARTOGRAPHER));
        villagers.onSocialize(farmer, employ(helper, VillagerProfession.MASON));
        int bargains = villagers.bargainCount(farmer);
        helper.assertTrue(bargains == 3, "expected 3 bargains, got " + bargains);

        for (int i = 0; i < 40; i++) farmer.restock(); // top-up trends toward the ceiling
        int total = 0;
        for (MerchantOffer offer : farmer.getOffers()) {
            int available = offer.getMaxUses() - offer.getUses();
            helper.assertTrue(available <= bargains,
                    "no trade may exceed the bargain count (" + available + " > " + bargains + ")");
            total += available;
        }
        helper.assertTrue(total > 0, "a well-bargained villager should restock some stock");
        helper.succeed();
    }

    /**
     * The counterforce is behavioural, not time-based: a villager with no bargains has nothing to restock from, so once
     * its earned stock is sold out it cannot refill. (An isolated villager is a finite resource, not an exploit.)
     */
    public static void isolatedVillagerCannotRefillWithoutBargains(GameTestHelper helper) {
        VillagerHandler villagers = Main.INSTANCE.getVillagerHandler();
        Villager farmer = employ(helper, VillagerProfession.FARMER);
        villagers.onSocialize(farmer, employ(helper, VillagerProfession.LIBRARIAN));
        villagers.onSocialize(farmer, employ(helper, VillagerProfession.CARTOGRAPHER));
        villagers.onSocialize(farmer, employ(helper, VillagerProfession.MASON));
        for (int i = 0; i < 40; i++) farmer.restock();
        helper.assertTrue(totalAvailable(farmer.getOffers()) > 0, "precondition: farmer earned some stock");

        // Cut it off: age the record a day so today's bargains read empty (nobody to socialize with).
        long today = farmer.level().getGameTime() / 24000L;
        villagers.setBargainData(farmer, villagers.getBargainData(farmer).onDay(today - 1));
        helper.assertTrue(villagers.bargainCount(farmer) == 0, "an isolated villager has no fresh bargains");
        helper.assertTrue(villagers.shouldSkipWork(farmer), "with no bargains there is no reason to work");
        helper.assertTrue(!villagers.wantsToRestock(farmer), "and nothing to restock from");

        // Sell it out, then confirm a restock can't bring stock back without bargains.
        for (MerchantOffer offer : farmer.getOffers()) offer.setToOutOfStock();
        farmer.restock();
        helper.assertTrue(totalAvailable(farmer.getOffers()) == 0, "a bargain-less restock must not refill stock");
        helper.succeed();
    }

    /** Changing profession wipes the day's bargains, and the new profession's trades start empty. */
    public static void professionChangeResetsBargainsAndStock(GameTestHelper helper) {
        VillagerHandler villagers = Main.INSTANCE.getVillagerHandler();
        Villager villager = employ(helper, VillagerProfession.FARMER);
        villagers.onSocialize(villager, employ(helper, VillagerProfession.LIBRARIAN));
        for (int i = 0; i < 40; i++) villager.restock();
        helper.assertTrue(villagers.bargainCount(villager) > 0, "precondition: villager has bargains");

        villager.setVillagerData(villager.getVillagerData()
                .withProfession(helper.getLevel().registryAccess(), VillagerProfession.CARTOGRAPHER));
        helper.assertTrue(villagers.bargainCount(villager) == 0, "changing profession should reset bargains");
        helper.assertTrue(totalAvailable(villager.getOffers()) == 0, "a new profession's trades start empty");
        helper.succeed();
    }

    /**
     * Because a bargain is a real use (not an artificial decay), trade use — whether by players or by bargaining
     * villagers, both via {@code increaseUses} — folds straight into vanilla's demand. We no longer neutralize it.
     */
    public static void tradeUseFeedsVanillaDemand(GameTestHelper helper) {
        VillagerHandler villagers = Main.INSTANCE.getVillagerHandler();
        Villager farmer = employ(helper, VillagerProfession.FARMER);
        villagers.onSocialize(farmer, employ(helper, VillagerProfession.LIBRARIAN));
        villagers.onSocialize(farmer, employ(helper, VillagerProfession.CARTOGRAPHER));
        villagers.onSocialize(farmer, employ(helper, VillagerProfession.MASON));
        for (int i = 0; i < 40; i++) farmer.restock(); // build up stock to use

        MerchantOffers offers = farmer.getOffers();
        helper.assertTrue(totalAvailable(offers) > 0, "precondition: farmer has stock to use");

        int demandBefore = 0;
        for (MerchantOffer offer : offers) demandBefore += offer.getDemand();

        // Use every trade to the limit (as a busy day of sales and bargains would), then restock to fold it in.
        for (MerchantOffer offer : offers) {
            while (offer.getMaxUses() - offer.getUses() > 0) offer.increaseUses();
        }
        farmer.restock();

        int demandAfter = 0;
        for (MerchantOffer offer : offers) demandAfter += offer.getDemand();
        helper.assertTrue(demandAfter > demandBefore,
                "trade use should raise vanilla demand (" + demandBefore + " -> " + demandAfter + ")");
        helper.succeed();
    }

    /** Spawns a villager and (if a profession is given) employs it, forcing its now-empty trades to generate. */
    private static Villager employ(GameTestHelper helper, ResourceKey<VillagerProfession> profession) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, 1, 2, 1);
        if (profession != null) {
            villager.setVillagerData(villager.getVillagerData()
                    .withProfession(helper.getLevel().registryAccess(), profession));
            villager.setVillagerDataFinalized(true);
            villager.getOffers(); // force trade generation (zeroed by the bargain mixin)
        }
        return villager;
    }

    private static int totalAvailable(MerchantOffers offers) {
        int total = 0;
        for (MerchantOffer offer : offers) {
            total += offer.getMaxUses() - offer.getUses();
        }
        return total;
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
