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
    public void netherEnderChestIsolatesFromOverworld(GameTestHelper helper) {
        NetherResetGameTests.netherEnderChestIsolatesFromOverworld(helper);
    }

    @GameTest
    public void recoveryCompassSpitsOutNetherStash(GameTestHelper helper) {
        NetherResetGameTests.recoveryCompassSpitsOutNetherStash(helper);
    }

    @GameTest
    public void echoShardRecoveryConsumesTheShard(GameTestHelper helper) {
        NetherResetGameTests.echoShardRecoveryConsumesTheShard(helper);
    }

    @GameTest
    public void netherEnderRestoredOnRespawnOutOfNether(GameTestHelper helper) {
        NetherResetGameTests.netherEnderRestoredOnRespawnOutOfNether(helper);
    }

    @GameTest
    public void enderChestStaysSharedWhenIsolationDisabled(GameTestHelper helper) {
        NetherResetGameTests.enderChestStaysSharedWhenIsolationDisabled(helper);
    }

    @GameTest
    public void allowedEntityCrossingStripsCarriedItems(GameTestHelper helper) {
        NetherResetGameTests.allowedEntityCrossingStripsCarriedItems(helper);
    }

    @GameTest
    public void allowedEntityKeepsItemsWhenClearingDisabled(GameTestHelper helper) {
        NetherResetGameTests.allowedEntityKeepsItemsWhenClearingDisabled(helper);
    }
}
