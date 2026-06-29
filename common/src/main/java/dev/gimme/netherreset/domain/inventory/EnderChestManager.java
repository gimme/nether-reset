package dev.gimme.netherreset.domain.inventory;

import dev.gimme.netherreset.application.PlayerAttachmentAccessor;
import dev.gimme.netherreset.domain.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Gives the Nether its own Ender Chest and gates extraction behind a deliberate ritual, so the Ender Chest can
 * only be used to take resources OUT of the Nether, never to carry them in.
 *
 * <p>The Ender Chest is the single storage vanilla shares across every dimension, so left alone it moves items in
 * <em>both</em> directions. Instead, the contents are swapped on each Nether crossing exactly like the player's
 * inventory already is: in the Nether you get a separate Ender Chest (your "Nether stash"), and your Overworld
 * contents are set aside. Nothing is auto-merged and nothing is ever dropped on a crossing.
 *
 * <p>To extract, the player performs an explicit ritual outside the Nether: right-clicking an Ender Chest with
 * a Recovery Compass (reusable) or an Echo Shard (consumed) spits their whole Nether stash out of the chest as
 * item drops. Because the only thing that moves the stash is this one-way ritual, items can never travel the other
 * way. The ritual works at <em>any</em> Ender Chest in any non-Nether dimension, and a key click always attempts
 * recovery rather than opening the chest, so its feedback (the stash spilling out, or a note that it's empty) is
 * never hidden behind the chest UI. The Ancient City Ender Chest — the guaranteed first one a player meets — is
 * additionally held shut even with an empty hand, showing a hint that teaches the ritual; every other Ender Chest
 * opens normally when used without a key.
 *
 * <p>The config is only consulted when entering the Nether (whether to isolate) and on the ritual. The restore on
 * the way out always runs whenever the player is isolated, so toggling the feature off never strands a player's
 * Overworld Ender Chest. The "isolated" flag also makes every crossing idempotent.
 */
public class EnderChestManager {

    private static final ResourceKey<Structure> ANCIENT_CITY =
            ResourceKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("minecraft", "ancient_city"));

    private static final Component HINT_MESSAGE = Component.translatableWithFallback(
            "netherreset.ender_chest.hint",
            "Use a Recovery Compass or Echo Shard to recover items from the Nether.").withStyle(ChatFormatting.YELLOW);

    private static final Component EMPTY_STASH_MESSAGE = Component.translatableWithFallback(
            "netherreset.ender_chest.empty",
            "Nothing to recover").withStyle(ChatFormatting.YELLOW);

    private final PlayerAttachmentAccessor playerAttachmentAccessor;
    private final ServerConfig serverConfig;

    public EnderChestManager(PlayerAttachmentAccessor playerAttachmentAccessor, ServerConfig serverConfig) {
        this.playerAttachmentAccessor = playerAttachmentAccessor;
        this.serverConfig = serverConfig;
    }

    /**
     * Swaps the Ender Chest to/from the Nether stash on a portal/command dimension crossing.
     */
    public void onChangeDimension(ServerPlayer player, ResourceKey<Level> fromDimension, ResourceKey<Level> toDimension) {
        if (fromDimension == toDimension) return;

        boolean toNether = toDimension == Level.NETHER;
        boolean fromNether = fromDimension == Level.NETHER;

        if (toNether && !fromNether) {
            isolate(player);
        } else if (fromNether && !toNether) {
            restore(player);
        }
    }

    /**
     * Restores the Ender Chest after a respawn. A player who dies in the Nether and respawns elsewhere never fires
     * a dimension-change event, so the swap-back has to happen here instead; a player who respawns back in the
     * Nether stays isolated.
     */
    public void onRespawn(ServerPlayer player) {
        if (player.level().dimension() == Level.NETHER) return;
        restore(player);
    }

    /**
     * Handles a right-click on an Ender Chest with an item in hand. Returns {@code true} if the interaction was
     * consumed (the chest should NOT open), {@code false} to let the chest open as normal.
     *
     * <p>Using a key (Recovery Compass or Echo Shard) is always taken as a recovery attempt, anywhere: it never
     * opens the chest, so its feedback — the stash spilling out, or a note that there's nothing to recover yet —
     * is never buried under the chest UI. Without a key, only the Ancient City chest is held shut (with a hint to
     * teach the ritual); every other Ender Chest stays a normal chest.
     */
    public boolean onUseEnderChest(ServerPlayer player, ItemStack heldItem, BlockPos chestPos) {
        if (!serverConfig.isolateNetherEnderChest()) return false; // feature off: vanilla Ender Chest
        if (player.level().dimension() == Level.NETHER) return false; // in the Nether the chest opens normally

        boolean usingKey = heldItem.is(Items.RECOVERY_COMPASS) || heldItem.is(Items.ECHO_SHARD);
        if (usingKey) {
            // A key click is always a recovery attempt, so it consumes the interaction wherever the player is —
            // the feedback never gets hidden behind the chest UI.
            if (!recover(player, heldItem, chestPos)) {
                playLockSound(player.level(), chestPos);
                player.sendSystemMessage(EMPTY_STASH_MESSAGE, true); // nothing to recover yet
            }
            return true;
        }

        // No key: only the Ancient City chest — the guaranteed first encounter — is held shut to teach the
        // ritual; every other Ender Chest opens as normal.
        if (isInAncientCity(player.level(), chestPos)) {
            playLockSound(player.level(), chestPos);
            player.sendSystemMessage(HINT_MESSAGE, true);
            return true;
        }
        return false;
    }

    private void isolate(ServerPlayer player) {
        if (!serverConfig.isolateNetherEnderChest()) return;

        DimInvData data = playerAttachmentAccessor.getOrCreateDimInvData(player);
        if (data.stashedOverworldEnder().isPresent()) return; // already isolated; keep the existing stash

        var enderChest = player.getEnderChestInventory();
        InventorySnapshot overworldEnder = InventorySnapshot.fromContainer(enderChest);
        InventorySnapshot netherStash = data.netherEnder().orElseGet(InventorySnapshot::empty);

        playerAttachmentAccessor.setDimInvData(player, data.withStashedOverworldEnder(overworldEnder));
        netherStash.applyToContainer(enderChest); // hand the player their (initially empty) Nether Ender Chest
    }

    private void restore(ServerPlayer player) {
        DimInvData data = playerAttachmentAccessor.getOrCreateDimInvData(player);
        if (data.stashedOverworldEnder().isEmpty()) return; // not isolated; nothing to restore

        var enderChest = player.getEnderChestInventory();
        InventorySnapshot netherStash = InventorySnapshot.fromContainer(enderChest); // what they leave behind

        // Save the Nether stash for later recovery, restore the Overworld chest, and drop the isolated flag.
        data.stashedOverworldEnder().get().applyToContainer(enderChest);
        playerAttachmentAccessor.setDimInvData(player,
                data.withNetherEnder(netherStash).withStashedOverworldEnder(null));
    }

    private boolean recover(ServerPlayer player, ItemStack heldItem, BlockPos chestPos) {
        DimInvData data = playerAttachmentAccessor.getOrCreateDimInvData(player);
        InventorySnapshot stash = data.netherEnder().orElseGet(InventorySnapshot::empty);
        boolean hasItems = stash.items().stream().anyMatch(item -> !item.isEmpty());
        if (!hasItems) return false; // nothing to recover; the caller reports this and consumes the click

        Level level = player.level();
        for (ItemStack item : stash.items()) {
            if (item.isEmpty()) continue;
            Block.popResource(level, chestPos, item.copy()); // spit it out of the chest
        }
        playerAttachmentAccessor.setDimInvData(player, data.withNetherEnder(InventorySnapshot.empty()));

        if (heldItem.is(Items.ECHO_SHARD)) heldItem.shrink(1); // the Recovery Compass is reusable; the shard is not

        playLockSound(level, chestPos);
        level.playSound(null, chestPos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 0.5F);
        return true;
    }

    private static void playLockSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 1.0F, 0.5F);
    }

    private static boolean isInAncientCity(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        return serverLevel.structureManager()
                .getStructureWithPieceAt(pos, holder -> holder.is(ANCIENT_CITY))
                .isValid();
    }
}
