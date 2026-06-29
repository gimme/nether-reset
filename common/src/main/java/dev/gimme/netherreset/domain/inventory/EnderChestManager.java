package dev.gimme.netherreset.domain.inventory;

import dev.gimme.netherreset.application.PlayerAttachmentAccessor;
import dev.gimme.netherreset.domain.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Gives the Nether its own Ender Chest and gates transfer behind a deliberate, key-only ritual. The ritual is
 * symmetric — it extracts whichever side you are <em>not</em> currently on — but because the keys exist only in
 * the Overworld (Ancient Cities) and can't be carried across, in practice resources only ever travel OUT of the
 * Nether, never in.
 *
 * <p>The Ender Chest is the single storage vanilla shares across every dimension, so left alone it moves items in
 * <em>both</em> directions. Instead, the contents are swapped on each Nether crossing exactly like the player's
 * inventory already is: in the Nether you get a separate Ender Chest (your "Nether stash"), and your Overworld
 * contents are set aside. Nothing is auto-merged and nothing is ever dropped on a crossing.
 *
 * <p>To transfer, the player performs an explicit ritual: right-clicking an Ender Chest with a Recovery Compass
 * (reusable) or an Echo Shard (consumed) spits the <em>other</em> side's stash out of the chest as item drops —
 * the Nether stash when used outside the Nether, the set-aside Overworld contents when used inside it. A key click
 * works at <em>any</em> Ender Chest and always attempts recovery rather than opening the chest, so its feedback
 * (the stash spilling out, or a note that it's empty) is never hidden behind the chest UI. The Ancient City Ender
 * Chest — the guaranteed first one a player meets — is additionally held shut even with an empty hand, showing a
 * hint that teaches the ritual; every other Ender Chest opens normally when used without a key.
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
     * <p>Using a key (Recovery Compass or Echo Shard) is always taken as a recovery attempt: it spills the other
     * side's stash out as drops rather than opening the chest, so its feedback is never buried under the chest UI.
     * Without a key, only the Ancient City chest is held shut (with a hint to teach the ritual); every other Ender
     * Chest stays a normal chest.
     */
    public boolean onUseEnderChest(ServerPlayer player, ItemStack heldItem, BlockPos chestPos) {
        if (!serverConfig.isolateNetherEnderChest()) return false; // feature off: vanilla Ender Chest

        boolean usingKey = heldItem.is(Items.RECOVERY_COMPASS) || heldItem.is(Items.ECHO_SHARD);
        if (usingKey) {
            if (!recover(player, heldItem, chestPos)) {
                playLockSound(player.level(), chestPos);
                player.sendSystemMessage(EMPTY_STASH_MESSAGE, true); // nothing to recover
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

        // Spill the side the player is NOT on: the set-aside Overworld chest while isolated in the Nether,
        // otherwise the persisted Nether stash. stashedOverworldEnder is present exactly while on the Nether side.
        boolean onNetherSide = data.stashedOverworldEnder().isPresent();
        InventorySnapshot stash = (onNetherSide ? data.stashedOverworldEnder() : data.netherEnder())
                .orElseGet(InventorySnapshot::empty);

        boolean hasItems = stash.items().stream().anyMatch(item -> !item.isEmpty());
        if (!hasItems) return false; // nothing to recover; the caller reports this and consumes the click

        // Use Containers.dropContents, not Block.popResource: popResource obeys the doTileDrops gamerule, so on a
        // server with block drops off a recovery would silently void the stash. Copy the stacks — dropContents
        // drains the ones it is handed.
        Level level = player.level();
        NonNullList<ItemStack> drops = NonNullList.create();
        for (ItemStack item : stash.items()) {
            if (!item.isEmpty()) drops.add(item.copy());
        }
        Containers.dropContents(level, chestPos, drops);
        DimInvData emptied = onNetherSide
                ? data.withStashedOverworldEnder(InventorySnapshot.empty()) // stay isolated, just emptied
                : data.withNetherEnder(InventorySnapshot.empty());
        playerAttachmentAccessor.setDimInvData(player, emptied);

        if (heldItem.is(Items.ECHO_SHARD)) heldItem.shrink(1); // the Recovery Compass is reusable; the shard is not

        playLockSound(level, chestPos);
        // Jitter upward only: 0.5 is the engine's pitch floor, so a symmetric jitter would be half-eaten by the
        // clamp; staying just above it keeps the sound low but slightly varied.
        float openPitch = 0.5F + level.getRandom().nextFloat() * 0.1F;
        level.playSound(null, chestPos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 1.0F, openPitch);
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
