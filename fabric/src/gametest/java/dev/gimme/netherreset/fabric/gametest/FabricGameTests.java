package dev.gimme.netherreset.fabric.gametest;

import dev.gimme.netherreset.gametest.NetherResetGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric test wiring, scanned via the {@code fabric-gametest} entrypoint. One {@link GameTest}
 * delegate per shared test; the structure defaults to Fabric API's built-in empty 8x8x8.
 */
public final class FabricGameTests {

    @GameTest
    public void firstNetherEntrySwapsInventoryAndAppliesGrace(GameTestHelper helper) {
        NetherResetGameTests.firstNetherEntrySwapsInventoryAndAppliesGrace(helper);
    }

    @GameTest
    public void dimensionInventoriesStayIsolatedAcrossCrossings(GameTestHelper helper) {
        NetherResetGameTests.dimensionInventoriesStayIsolatedAcrossCrossings(helper);
    }

    @GameTest
    public void bargainsAccrueAcrossProfessionsButNotDuplicatesOrSelf(GameTestHelper helper) {
        NetherResetGameTests.bargainsAccrueAcrossProfessionsButNotDuplicatesOrSelf(helper);
    }

    @GameTest
    public void plainVillagersAreWildcardPartners(GameTestHelper helper) {
        NetherResetGameTests.plainVillagersAreWildcardPartners(helper);
    }

    @GameTest
    public void bargainCapIsRespected(GameTestHelper helper) {
        NetherResetGameTests.bargainCapIsRespected(helper);
    }

    @GameTest
    public void tradesStartEmptyAndRestockScalesWithBargains(GameTestHelper helper) {
        NetherResetGameTests.tradesStartEmptyAndRestockScalesWithBargains(helper);
    }

    @GameTest
    public void isolatedVillagerCannotRefillWithoutBargains(GameTestHelper helper) {
        NetherResetGameTests.isolatedVillagerCannotRefillWithoutBargains(helper);
    }

    @GameTest
    public void professionChangeResetsBargainsAndStock(GameTestHelper helper) {
        NetherResetGameTests.professionChangeResetsBargainsAndStock(helper);
    }

    @GameTest
    public void tradeUseFeedsVanillaDemand(GameTestHelper helper) {
        NetherResetGameTests.tradeUseFeedsVanillaDemand(helper);
    }
}
