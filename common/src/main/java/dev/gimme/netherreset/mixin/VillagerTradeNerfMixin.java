package dev.gimme.netherreset.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies a nerf on the previous-level emerald bids as soon as a villager has earned enough XP to level up,
 * instead of waiting for the (delayed) level-up.
 */
@Mixin(Villager.class)
public class VillagerTradeNerfMixin {

    @Unique
    private static final int MAX_NERFED_BID_USES = 3;

    @Shadow
    private boolean increaseProfessionLevelOnUpdate;

    @Shadow
    private void resendOffersToTradingPlayer() {
        throw new AssertionError();
    }

    @Inject(method = "rewardTradeXp", at = @At("TAIL"))
    private void nether_reset$nerfBidsWhenReadyToLevel(MerchantOffer offer, CallbackInfo ci) {
        // Vanilla sets this the instant the villager has enough XP to level up.
        if (!this.increaseProfessionLevelOnUpdate) return;

        MerchantOffers offers = ((Villager) (Object) this).getOffers();
        boolean changed = false;
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer current = offers.get(i);
            // Only the still-generous emerald (selling) bids.
            if (current.getResult().is(Items.EMERALD) && current.getMaxUses() > MAX_NERFED_BID_USES) {
                offers.set(i, new MerchantOffer(
                    current.getItemCostA(), current.getItemCostB(), current.getResult(),
                    current.getUses(), MAX_NERFED_BID_USES, 1, current.getPriceMultiplier()));
                changed = true;
            }
        }

        if (changed) {
            this.resendOffersToTradingPlayer();
        }
    }
}
