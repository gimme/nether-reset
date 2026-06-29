package dev.gimme.netherreset.mixin;

import dev.gimme.netherreset.Main;
import dev.gimme.netherreset.domain.config.ServerConfig;
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

/**
 * Adds extra Cartographer trades:
 * <ul>
 *     <li>Ancient City Map trades pinned to the Expert (level 4) trade set, to give players a way to consistently
 *     find the structure.</li>
 *     <li>A fallback Amethyst Shard -&gt; Emerald trade at the Apprentice (level 2) tier, added only when the
 *     Cartographer didn't roll the vanilla Glass Pane -&gt; Emerald trade, so it can always be leveled up cheaply
 *     instead of having to commit to an explorer-map run.</li>
 * </ul>
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
        ServerConfig config = Main.INSTANCE.getServerConfig();
        if (resourceKey.equals(TradeSets.CARTOGRAPHER_LEVEL_4)) {
            if (!config.isAncientCityMapTradeEnabled()) return;
            // Pin the Ancient City Map trades to the Expert level.
            nether_reset$addOffer(level, offers, OCEAN_ANCIENT_CITY_MAP_TRADE);
            nether_reset$addOffer(level, offers, TRIAL_ANCIENT_CITY_MAP_TRADE);
        } else if (resourceKey.equals(TradeSets.CARTOGRAPHER_LEVEL_2)) {
            if (!config.isCartographerLevelingTradeEnabled()) return;
            // The Apprentice pool can surface the cheap Glass Pane -> Emerald leveling trade or one of several
            // (biome-gated) explorer/village map trades, so a Cartographer doesn't always roll the leveling trade.
            // When it didn't, add an equivalent Amethyst Shard -> Emerald trade so it can still be leveled up affordably.
            if (!nether_reset$hasGlassPaneEmeraldTrade(offers)) {
                offers.add(new MerchantOffer(new ItemCost(Items.AMETHYST_SHARD, 8), new ItemStack(Items.EMERALD), 12, 10, 0.05f));
            }
        }
    }

    @Unique
    private static boolean nether_reset$hasGlassPaneEmeraldTrade(MerchantOffers offers) {
        return offers.stream()
            .anyMatch(offer -> offer.getResult().is(Items.EMERALD) && offer.getBaseCostA().is(Items.GLASS_PANE));
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
