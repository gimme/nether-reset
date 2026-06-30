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
    public void recoveryFromNetherSideSpitsOutOverworldStash(GameTestHelper helper) {
        NetherResetGameTests.recoveryFromNetherSideSpitsOutOverworldStash(helper);
    }

    @GameTest
    public void netherEnderRestoredOnRespawnOutOfNether(GameTestHelper helper) {
        NetherResetGameTests.netherEnderRestoredOnRespawnOutOfNether(helper);
    }

    @GameTest
    public void netherDeathRespawnsAtEntryPortal(GameTestHelper helper) {
        NetherResetGameTests.netherDeathRespawnsAtEntryPortal(helper);
    }

    @GameTest
    public void netherDeathRespawnUnchangedWhenDisabled(GameTestHelper helper) {
        NetherResetGameTests.netherDeathRespawnUnchangedWhenDisabled(helper);
    }

    @GameTest
    public void deathOutsideNetherKeepsVanillaRespawn(GameTestHelper helper) {
        NetherResetGameTests.deathOutsideNetherKeepsVanillaRespawn(helper);
    }

    @GameTest
    public void netherDeathWithoutEntryKeepsVanillaRespawn(GameTestHelper helper) {
        NetherResetGameTests.netherDeathWithoutEntryKeepsVanillaRespawn(helper);
    }

    @GameTest
    public void netherRespawnAnchorWinsOverEntryPortal(GameTestHelper helper) {
        NetherResetGameTests.netherRespawnAnchorWinsOverEntryPortal(helper);
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
