package dev.gimme.netherreset.mixin;

import dev.gimme.netherreset.Main;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wires villagers into the "bargaining" economy (see {@code BargainManager}). Each hook delegates to the live
 * {@code Main.INSTANCE} villager handler so the rules stay testable and out of the mixin. Inherited members
 * ({@code getOffers}, {@code getVillagerData}) are reached by cast rather than {@code @Shadow}, matching the
 * convention in {@code VillagerTradeNerfMixin}. Stock lives in {@code maxUses} (via {@link MerchantOfferAccessor});
 * {@code uses} is left as vanilla's real "times used", so demand keeps working.
 */
@Mixin(Villager.class)
public class VillagerBargainMixin {

    @Unique
    private boolean nether_reset$enabled() {
        return Main.INSTANCE.getServerConfig().villagerBargainEnabled();
    }

    /** Socializing at the bell (or anywhere) is where a bargain is struck. */
    @Inject(method = "gossip", at = @At("HEAD"))
    private void nether_reset$bargainOnSocialize(ServerLevel level, Villager target, long timestamp, CallbackInfo ci) {
        Main.INSTANCE.getVillagerHandler().onSocialize((Villager) (Object) this, target);
    }

    /**
     * Vanilla restock would reset every trade to full ({@code uses = 0} against the old {@code maxUses}). Instead,
     * retune {@code maxUses} to the bargain-driven ceiling first (topping up, never lowering), then reset {@code uses}.
     * Vanilla's demand update ran just before this, off the real {@code uses}, so prices stay honest.
     */
    @Redirect(method = "restock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;resetUses()V"))
    private void nether_reset$bargainRestock(MerchantOffer offer) {
        if (!nether_reset$enabled()) {
            offer.resetUses();
            return;
        }
        int priorAvailable = offer.getMaxUses() - offer.getUses();
        int target = Main.INSTANCE.getVillagerHandler().restockTarget((Villager) (Object) this, priorAvailable);
        ((MerchantOfferAccessor) (Object) offer).nether_reset$setMaxUses(target);
        offer.resetUses();
    }

    /**
     * Vanilla only restocks once a trade has been used; here stock is driven by bargains, not sales, so let a villager
     * with bargains restock even when nothing has sold (this also bootstraps trades from their empty starting state).
     */
    @Inject(method = "needsToRestock", at = @At("HEAD"), cancellable = true)
    private void nether_reset$restockFromBargains(CallbackInfoReturnable<Boolean> cir) {
        if (nether_reset$enabled()) {
            cir.setReturnValue(Main.INSTANCE.getVillagerHandler().wantsToRestock((Villager) (Object) this));
        }
    }

    /** Newly generated trades (hire, level-up, profession change) start with no stock. */
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void nether_reset$zeroFreshStock(ServerLevel level, CallbackInfo ci) {
        if (!nether_reset$enabled()) return;
        for (MerchantOffer offer : ((Villager) (Object) this).getOffers()) {
            ((MerchantOfferAccessor) (Object) offer).nether_reset$setMaxUses(0);
        }
    }

    /** Changing profession resets the day's bargains. */
    @Inject(method = "setVillagerData", at = @At("HEAD"))
    private void nether_reset$resetBargainsOnProfessionChange(VillagerData data, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (!self.getVillagerData().profession().equals(data.profession())) {
            Main.INSTANCE.getVillagerHandler().onProfessionChanged(self);
        }
    }
}
