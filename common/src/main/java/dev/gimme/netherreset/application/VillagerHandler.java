package dev.gimme.netherreset.application;

import dev.gimme.netherreset.domain.bargain.BargainData;
import dev.gimme.netherreset.domain.bargain.BargainManager;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Entry point the villager mixins call into, mirroring {@link PlayerHandler}. Keeps the mixins thin and the bargaining
 * logic in {@link BargainManager}, which the game tests drive directly through {@code Main.INSTANCE}.
 */
public class VillagerHandler {

    private final BargainManager bargainManager;

    public VillagerHandler(BargainManager bargainManager) {
        this.bargainManager = bargainManager;
    }

    /** Two villagers socialized (vanilla {@code Villager.gossip}). */
    public void onSocialize(Villager self, Villager target) {
        bargainManager.onSocialize(self, target);
    }

    /** The new stock ceiling for one trade being restocked, given its current available count. */
    public int restockTarget(Villager self, int priorAvailable) {
        return bargainManager.restockTarget(self, priorAvailable);
    }

    /** Whether the villager has bargains to restock from (drives vanilla's restock trigger). */
    public boolean wantsToRestock(Villager self) {
        return bargainManager.wantsToRestock(self);
    }

    /** Whether this villager should skip working (no bargains, nothing to restock). */
    public boolean shouldSkipWork(Villager self) {
        return bargainManager.shouldSkipWork(self);
    }

    /** The villager changed profession — reset its bargains. */
    public void onProfessionChanged(Villager self) {
        bargainManager.onProfessionChanged(self);
    }

    public int bargainCount(Villager self) {
        return bargainManager.bargainCount(self);
    }

    public BargainData getBargainData(Villager self) {
        return bargainManager.getBargainData(self);
    }

    public void setBargainData(Villager self, BargainData data) {
        bargainManager.setBargainData(self, data);
    }
}
