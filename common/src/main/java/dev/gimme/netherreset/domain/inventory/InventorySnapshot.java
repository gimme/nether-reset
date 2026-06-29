package dev.gimme.netherreset.domain.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record InventorySnapshot(
        List<ItemStack> items
) {

    public static final Codec<InventorySnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(InventorySnapshot::items)
    ).apply(inst, InventorySnapshot::new));

    public static InventorySnapshot empty() {
        return new InventorySnapshot(List.of());
    }

    public static InventorySnapshot of(List<ItemStack> items) {
        return new InventorySnapshot(items);
    }

    public static InventorySnapshot fromPlayer(ServerPlayer player) {
        return fromContainer(player.getInventory());
    }

    /** Captures a copy of every slot in the given container. */
    public static InventorySnapshot fromContainer(Container container) {
        var out = new ArrayList<ItemStack>(container.getContainerSize());
        for (int i = 0; i < container.getContainerSize(); i++) {
            out.add(container.getItem(i).copy());
        }
        return new InventorySnapshot(List.copyOf(out));
    }

    public void applyTo(ServerPlayer player) {
        applyToContainer(player.getInventory());
        player.inventoryMenu.broadcastChanges();
    }

    /** Overwrites the given container with this snapshot, clearing any slots the snapshot doesn't fill. */
    public void applyToContainer(Container container) {
        // Clear first to avoid leftover items if sizes differ
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        int n = Math.min(items.size(), container.getContainerSize());
        for (int i = 0; i < n; i++) {
            container.setItem(i, items.get(i));
        }

        container.setChanged();
    }
}
