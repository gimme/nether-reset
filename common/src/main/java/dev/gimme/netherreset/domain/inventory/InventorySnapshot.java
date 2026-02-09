package dev.gimme.netherreset.domain.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
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
        var inv = player.getInventory();

        var out = new ArrayList<ItemStack>(inv.getContainerSize());
        for (int i = 0; i < inv.getContainerSize(); i++) {
            out.add(inv.getItem(i).copy());
        }
        return new InventorySnapshot(List.copyOf(out));
    }

    public void applyTo(ServerPlayer player) {
        var inv = player.getInventory();

        // Clear first to avoid leftover items if sizes differ
        for (int i = 0; i < inv.getContainerSize(); i++) {
            inv.setItem(i, ItemStack.EMPTY);
        }

        int n = Math.min(items.size(), inv.getContainerSize());
        for (int i = 0; i < n; i++) {
            inv.setItem(i, items.get(i));
        }

        inv.setChanged();
        player.inventoryMenu.broadcastChanges();
    }
}
