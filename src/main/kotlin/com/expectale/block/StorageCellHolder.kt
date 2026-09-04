package com.expectale.block

import com.expectale.storage_cell.StorageCell
import com.expectale.storage_cell.VirtualStorageCell
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPostUpdateEvent
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason
import xyz.xenondevs.nova.util.addToInventoryOrDrop
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.world.block.tileentity.TileEntity

/**
 * Owns the storage cells of a unit. [cellInventory] only ever holds client-side display stacks:
 * a cell dropped into it is converted into a [VirtualStorageCell] in [virtualMap] right away, and
 * taking a display stack out hands the player the real cell rebuilt from that map.
 */
interface StorageCellHolder {

    val cellInventory: VirtualInventory

    val virtualMap: HashMap<Int, VirtualStorageCell>

    /**
     * Called after the set of cells changed.
     */
    fun callUpdateCell()

    /**
     * Called when [cell] was put into [slot], so the holder can persist it.
     */
    fun cellInserted(slot: Int, cell: VirtualStorageCell)

    /**
     * Called when the cell in [slot] was taken out, so the holder can forget it.
     */
    fun cellRemoved(slot: Int)

    /**
     * How many distinct item types all cells can hold together.
     */
    fun getSize(): Int =
        virtualMap.values.sumOf { it.cellData.itemAmount }

    /**
     * Every stored item with its total amount across all cells, in a stable order.
     */
    fun getItems(): Map<ItemStack, Int> {
        val merged = LinkedHashMap<ItemStack, Int>()
        for (slot in cellSlots()) {
            val cell = virtualMap[slot] ?: continue
            for ((item, amount) in cell.getItems()) {
                merged.merge(item, amount, Int::plus)
            }
        }
        return merged
    }

    /**
     * How many of [item] are stored across all cells.
     */
    fun getItemAmount(item: ItemStack): Int =
        virtualMap.values.sumOf { it.cellData.get(item) }

    /**
     * Adds [item] to the first cells with room for it and returns the amount that did not fit.
     */
    fun addItemToCell(item: ItemStack): Int {
        var remaining = item.amount
        for (slot in cellSlots()) {
            if (remaining == 0)
                break

            val cell = virtualMap[slot] ?: continue
            val before = remaining
            remaining = cell.add(item, remaining)
            if (remaining != before)
                updateCell(slot)
        }
        return remaining
    }

    /**
     * Removes [amount] of [item] from the cells holding it and returns the amount that could not be removed.
     */
    fun removeItem(item: ItemStack, amount: Int = item.amount): Int {
        var remaining = amount
        for (slot in cellSlots()) {
            if (remaining == 0)
                break

            val cell = virtualMap[slot] ?: continue
            val before = remaining
            remaining = cell.remove(item, remaining)
            if (remaining != before)
                updateCell(slot)
        }
        return remaining
    }

    /**
     * Reads cells out of an inventory of real cell items, the layout the original addon persisted.
     */
    fun fromInventory(inventory: VirtualInventory): HashMap<Int, VirtualStorageCell> {
        val map = HashMap<Int, VirtualStorageCell>()
        for (slot in 0..<inventory.size) {
            val itemStack = inventory.getItem(slot) ?: continue
            val cell = itemStack.novaItem?.getBehaviorOrNull<StorageCell>()?.toVirtual(itemStack) ?: continue
            map[slot] = cell
        }
        return map
    }

    fun updateCell(slot: Int) {
        cellInventory.setItem(TileEntity.SELF_UPDATE_REASON, slot, virtualMap[slot]?.toDisplay()?.get())
    }

    fun updateCellInv() {
        for (slot in cellSlots()) {
            updateCell(slot)
        }
        callUpdateCell()
    }

    fun handleCellUpdate(event: ItemPreUpdateEvent) {
        if (event.updateReason == TileEntity.SELF_UPDATE_REASON)
            return
        if (event.newItem == null && event.previousItem == null)
            return

        when {
            event.isAdd -> {
                event.isCancelled = event.newItem?.novaItem?.getBehaviorOrNull<StorageCell>() == null
            }

            event.isRemove -> {
                // the display stack never leaves; the player gets the real cell instead
                event.isCancelled = true
                val cell = virtualMap.remove(event.slot) ?: return
                cellRemoved(event.slot)
                (event.updateReason as? PlayerUpdateReason)?.player()?.addToInventoryOrDrop(cell.toItem())
                updateCellInv()
            }

            else -> event.isCancelled = true
        }
    }

    fun handlePostCellUpdate(event: ItemPostUpdateEvent) {
        if (event.updateReason == TileEntity.SELF_UPDATE_REASON || !event.isAdd)
            return

        val stack = event.newItem ?: return
        val cell = stack.novaItem?.getBehaviorOrNull<StorageCell>()?.toVirtual(stack) ?: return
        virtualMap[event.slot] = cell
        cellInserted(event.slot, cell)
        updateCellInv()
    }

    private fun cellSlots(): IntRange =
        0..<cellInventory.size

}
