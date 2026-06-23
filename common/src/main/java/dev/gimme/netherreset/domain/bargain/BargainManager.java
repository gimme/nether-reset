package dev.gimme.netherreset.domain.bargain;

import dev.gimme.netherreset.application.VillagerAttachmentAccessor;
import dev.gimme.netherreset.domain.config.ServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.Optional;

/**
 * The "bargaining" economy: villagers must socialize with other professions to stock their trades.
 *
 * <p>The whole feature rides on vanilla's own trade fields, kept honest:
 * <ul>
 *   <li><b>{@code maxUses}</b> is the bargain-driven stock ceiling. It starts at 0 (no free stock) and a restock
 *       raises it toward today's bargain count (a random {@code 0..bargains} per trade, never lowering what's there).
 *       Set via a {@code @Mutable} accessor in the mixin layer.</li>
 *   <li><b>{@code uses}</b> keeps its true meaning — times the trade was actually used — by <em>players and by other
 *       villagers</em>. A bargain is a villager-to-villager use ({@link #applyBargainUse}), so it consumes stock and
 *       feeds vanilla demand exactly like a sale would. There is no artificial "decay".</li>
 * </ul>
 *
 * <p>That makes the counterforce purely behavioural: bargaining spends stock, and with no bargains a villager has
 * nothing to restock from — so an isolated villager is a finite, non-refilling resource rather than an exploit. It
 * also means there is no background time-decay to desync from the world: stock simply freezes while unloaded.
 *
 * <p>Bargains themselves are a {@link BargainData} attachment keyed by a single stored day; a stale record reads as
 * empty and is cleaned up the next time a bargain is registered — no per-tick bookkeeping.
 */
public class BargainManager {

    private final VillagerAttachmentAccessor attachments;
    private final ServerConfig config;

    public BargainManager(VillagerAttachmentAccessor attachments, ServerConfig config) {
        this.attachments = attachments;
        this.config = config;
    }

    public boolean isEnabled() {
        return config.villagerBargainEnabled();
    }

    private int cap() {
        return config.villagerBargainCap();
    }

    private static long dayOf(Villager villager) {
        return villager.level().getGameTime() / 24000L;
    }

    /** This villager's profession identity, or empty if it is a plain (professionless) wildcard. */
    private static Optional<Identifier> employedProfession(Villager villager) {
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        ResourceKey<VillagerProfession> key = profession.unwrapKey().orElse(null);
        if (key == null || key.equals(VillagerProfession.NONE)) return Optional.empty();
        return Optional.of(key.identifier());
    }

    /** Today's bargain record, treating a stale (previous-day) record as empty — the lazy daily reset. */
    private BargainData today(Villager villager, long day) {
        BargainData data = attachments.getBargainData(villager);
        return data.day() == day ? data : BargainData.emptyOn(day);
    }

    /** Total bargains this villager has struck today. */
    public int bargainCount(Villager villager) {
        if (!isEnabled()) return 0;
        return today(villager, dayOf(villager)).bargains();
    }

    /** A villager with no bargains has nothing to restock from, so there is no reason to go to work. */
    public boolean shouldSkipWork(Villager villager) {
        return isEnabled() && bargainCount(villager) <= 0;
    }

    // --- Socializing --------------------------------------------------------------------------------------------

    /**
     * Two villagers meet while socializing. If they mutually agree to bargain, both sides record the other's identity
     * (an employed partner as that profession, a plain partner as a wildcard) and each employed side spends a little
     * of its own stock on the exchange.
     */
    public void onSocialize(Villager a, Villager b) {
        if (!isEnabled() || a == b || a.isBaby() || b.isBaby()) return;

        Optional<Identifier> pa = employedProfession(a);
        Optional<Identifier> pb = employedProfession(b);
        if (pa.isEmpty() && pb.isEmpty()) return; // two plain villagers: nothing to trade

        long day = dayOf(a);
        BargainData da = today(a, day);
        BargainData db = today(b, day);

        // The cap bounds how much stock a villager can bank, so it only gates employed villagers. A plain villager
        // banks no stock; it still records partners (below) to dedup, but stays an unlimited wildcard.
        if (pa.isPresent() && da.bargains() >= cap()) return;
        if (pb.isPresent() && db.bargains() >= cap()) return;

        // Neither may be, or already hold, the other's profession.
        if (pa.isPresent() && pb.isPresent() && pa.get().equals(pb.get())) return;
        if (pb.isPresent() && da.hasBargainedWith(pb.get())) return;
        if (pa.isPresent() && db.hasBargainedWith(pa.get())) return;

        // Agreed. Each side records the other; plain villagers keep their record too, so the same profession can't
        // count a given plain villager twice in a day. (This write also persists the lazy daily reset.)
        attachments.setBargainData(a, pb.map(da::withProfession).orElseGet(da::withWildcard));
        attachments.setBargainData(b, pa.map(db::withProfession).orElseGet(db::withWildcard));

        // The bargain is a villager-to-villager use: each employed side spends some of its own stock.
        if (pa.isPresent()) applyBargainUse(a.getOffers(), a.getRandom());
        if (pb.isPresent()) applyBargainUse(b.getOffers(), b.getRandom());
    }

    /** A bargain uses up some of the merchant's wares: each in-stock trade has a 50% chance to be sold one unit. */
    private static void applyBargainUse(MerchantOffers offers, RandomSource random) {
        for (MerchantOffer offer : offers) {
            if (offer.getMaxUses() - offer.getUses() > 0 && random.nextBoolean()) {
                offer.increaseUses();
            }
        }
    }

    // --- Restocking ---------------------------------------------------------------------------------------------

    /**
     * The new stock ceiling for one trade on a restock: a fresh random roll in {@code [0, bargains]}, but never below
     * what is already in stock (so a low roll can't undo earned stock). The caller resets {@code uses} to 0, leaving
     * this as the available count.
     */
    public int restockTarget(Villager villager, int priorAvailable) {
        int bargains = bargainCount(villager);
        int roll = bargains <= 0 ? 0 : villager.getRandom().nextInt(bargains + 1);
        return Math.max(priorAvailable, roll);
    }

    /** Whether the villager wants to restock at all — i.e. it has bargains to stock from. */
    public boolean wantsToRestock(Villager villager) {
        return isEnabled() && bargainCount(villager) > 0;
    }

    // --- Lifecycle ----------------------------------------------------------------------------------------------

    /** Changing profession wipes the day's bargains — a fresh slate for the new trade. */
    public void onProfessionChanged(Villager villager) {
        if (!isEnabled()) return;
        attachments.setBargainData(villager, BargainData.emptyOn(dayOf(villager)));
    }

    // --- Inspection (for tests / tooling) -----------------------------------------------------------------------

    public BargainData getBargainData(Villager villager) {
        return attachments.getBargainData(villager);
    }

    public void setBargainData(Villager villager, BargainData data) {
        attachments.setBargainData(villager, data);
    }
}
