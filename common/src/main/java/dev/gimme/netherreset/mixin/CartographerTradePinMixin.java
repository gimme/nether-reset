package dev.gimme.netherreset.mixin;

import dev.gimme.netherreset.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
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
 * Pins the Ancient City Map trades to the Cartographer's Expert trade set, to provide a way for players to
 * consistently find the structure.
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
        if (!Main.INSTANCE.getServerConfig().isAncientCityMapTradeEnabled()) return;
        if (resourceKey.equals(TradeSets.CARTOGRAPHER_LEVEL_4)) {
            // Pin the Ancient City Map trades to the Expert level.
            nether_reset$addOffer(level, offers, OCEAN_ANCIENT_CITY_MAP_TRADE);
            nether_reset$addOffer(level, offers, TRIAL_ANCIENT_CITY_MAP_TRADE);
        }
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
