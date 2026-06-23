package dev.gimme.netherreset.mixin;

import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets the bargain feature retune a trade's stock ceiling in place by writing the otherwise-final {@code maxUses},
 * which preserves every other field (cost, result, {@code rewardExp}, demand) far more cleanly than rebuilding the
 * offer. {@code uses} stays vanilla (the real "times used").
 */
@Mixin(MerchantOffer.class)
public interface MerchantOfferAccessor {

    @Accessor("maxUses")
    @Mutable
    void nether_reset$setMaxUses(int maxUses);
}
