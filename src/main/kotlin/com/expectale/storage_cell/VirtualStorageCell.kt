package com.expectale.storage_cell

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.nova.world.item.NovaItem

/**
 * A storage cell taken out of its item form while it sits in a unit: the cell's contents live
 * here and are written back into an [ItemStack] with [toItem] when the cell leaves the unit.
 */
class VirtualStorageCell(
    val cellData: CellData,
    val novaItem: NovaItem
) {

    fun add(item: ItemStack, amount: Int): Int =
        cellData.add(item, amount)

    fun remove(item: ItemStack, amount: Int): Int =
        cellData.remove(item, amount)

    fun getItems(): Map<ItemStack, Int> =
        cellData.getItems()

    fun isEmpty(): Boolean =
        cellData.getStoredBytesAmount() == 0 && cellData.getStoredItemTypeAmount() == 0

    fun toItem(): ItemStack {
        val itemStack = novaItem.createItemStack()
        novaItem.getBehaviorOrNull<StorageCell>()?.setCellData(itemStack, cellData)
        return itemStack
    }

    /**
     * The client-side stack shown in the unit's cell inventory in place of the real cell.
     */
    fun toDisplay(): ItemBuilder =
        novaItem.createClientsideItemBuilder()
            .addLoreLines(
                StorageCell.byteDisplay(cellData.getStoredBytesAmount(), cellData.capacity),
                StorageCell.typeDisplay(cellData.getStoredItemTypeAmount(), cellData.itemAmount)
            )

}
