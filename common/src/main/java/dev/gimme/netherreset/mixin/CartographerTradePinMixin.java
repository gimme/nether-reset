package dev.gimme.netherreset.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Adds Ancient City Map to Cartographer's Expert trade set, to provide a way for players to consistently find the
 * structure. Also rebalances the other trades to smooth out the progression.
 */
@Mixin(AbstractVillager.class)
public class CartographerTradePinMixin {

    @Unique
    private static final ResourceKey<VillagerTrade> OCEAN_ANCIENT_CITY_MAP_TRADE = ResourceKey.create(Registries.VILLAGER_TRADE,
        Identifier.fromNamespaceAndPath("netherreset", "cartographer/4/ocean_ancient_city_map"));
    @Unique
    private static final ResourceKey<VillagerTrade> TRIAL_ANCIENT_CITY_MAP_TRADE = ResourceKey.create(Registries.VILLAGER_TRADE,
        Identifier.fromNamespaceAndPath("netherreset", "cartographer/4/trial_ancient_city_map"));

    @Inject(method = "addOffersFromTradeSet", at = @At("TAIL"))
    private void pinCartographerTrades(ServerLevel level, MerchantOffers offers, ResourceKey<TradeSet> resourceKey, CallbackInfo ci) {
        // TODO: tie the trade rebalancing to a config flag (default: enabled)

        // Adjust trades for the new level.
        if (resourceKey.equals(TradeSets.CARTOGRAPHER_LEVEL_1)) {
            // Buff the Paper trade to be cheaper.
            nether_reset$replaceAll(offers,
                offer -> offer.getCostA().is(Items.PAPER),
                offer -> nether_reset$withCost(12, offer));

            // Buff the Map trade to be cheaper.
            nether_reset$replaceAll(offers,
                offer -> offer.getResult().is(Items.MAP),
                offer -> nether_reset$withCost(2, offer));
        } else if (resourceKey.equals(TradeSets.CARTOGRAPHER_LEVEL_2)) {
            // Pin an Amethyst Shard trade if vanilla didn't roll the simpler Glass Pane trade.
            if (nether_reset$doesNotHaveGlassPaneTrade(offers) && nether_reset$doesNotHaveAmethystTrade(offers)) {
                nether_reset$addAmethystTrade(offers);
            }
        } else if (resourceKey.equals(TradeSets.CARTOGRAPHER_LEVEL_3)) {
            // Pin an Amethyst Shard trade if vanilla didn't roll the simpler Compass trade.
            if (nether_reset$doesNotHaveCompassTrade(offers) && nether_reset$doesNotHaveAmethystTrade(offers)) {
                nether_reset$addAmethystTrade(offers);
            }
            // Add alternative trade for XP.
            nether_reset$addSpyglassTrade(offers);
        }

        if (resourceKey.equals(TradeSets.CARTOGRAPHER_LEVEL_4)) {
            // Pin the Ancient City Map trades to the Expert level.
            nether_reset$addOffer(level, offers, OCEAN_ANCIENT_CITY_MAP_TRADE);
            nether_reset$addOffer(level, offers, TRIAL_ANCIENT_CITY_MAP_TRADE);
        }
    }

    @Unique
    private static void nether_reset$replaceAll(MerchantOffers offers, Predicate<MerchantOffer> matcher, Function<MerchantOffer, MerchantOffer> replacer) {
        offers.replaceAll(offer -> matcher.test(offer) ? replacer.apply(offer) : offer);
    }

    @Unique
    private static MerchantOffer nether_reset$withCost(int count, MerchantOffer offer) {
        return new MerchantOffer(new ItemCost(offer.getCostA().getItem(), count), offer.getItemCostB(), offer.getResult(), offer.getUses(), offer.getMaxUses(), offer.getXp(), offer.getPriceMultiplier());
    }

    @Unique
    private static boolean nether_reset$doesNotHaveGlassPaneTrade(MerchantOffers offers) {
        return offers.stream().noneMatch(offer -> offer.getCostA().is(Items.GLASS_PANE));
    }

    @Unique
    private static boolean nether_reset$doesNotHaveCompassTrade(MerchantOffers offers) {
        return offers.stream().noneMatch(offer -> offer.getCostA().is(Items.COMPASS));
    }

    @Unique
    private static boolean nether_reset$doesNotHaveAmethystTrade(MerchantOffers offers) {
        return offers.stream().noneMatch(offer -> offer.getCostA().is(Items.AMETHYST_SHARD));
    }

    @Unique
    private static void nether_reset$addAmethystTrade(MerchantOffers offers) {
        offers.add(new MerchantOffer(new ItemCost(Items.AMETHYST_SHARD, 8), new ItemStack(Items.EMERALD), 12, 10, 0.05f));
    }

    @Unique
    private static void nether_reset$addSpyglassTrade(MerchantOffers offers) {
        offers.add(new MerchantOffer(new ItemCost(Items.COPPER_INGOT, 14), Optional.of(new ItemCost(Items.EMERALD, 2)), new ItemStack(Items.SPYGLASS), 12, 10, 0.2f));
    }

    @Unique
    private void nether_reset$addOffer(ServerLevel serverLevel, MerchantOffers offers, ResourceKey<VillagerTrade> tradeKey) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        VillagerTrade trade = self.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).getOptional(tradeKey).orElse(null);
        if (trade == null) return;

        LootContext context = new LootContext.Builder(
            new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, self.position())
                .withParameter(LootContextParams.THIS_ENTITY, self)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE)
        ).create(Optional.empty());

        MerchantOffer offer = trade.getOffer(context);
        if (offer != null) {
            offers.add(offer);
        }
    }
}
