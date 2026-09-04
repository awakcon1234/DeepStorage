package com.expectale.storage_cell

import org.bukkit.inventory.ItemStack

/**
 * The contents of one storage cell: up to [itemAmount] distinct item types and [capacity] items in total.
 * Keys of [dataMap] are always single-item stacks, so two stacks of the same item share one entry.
 */
class CellData(
    val capacity: Int,
    val itemAmount: Int,
    val dataMap: MutableMap<ItemStack, Int>
) {

    fun getStoredItemTypeAmount(): Int =
        dataMap.size

    fun getStoredBytesAmount(): Int =
        dataMap.values.sum()

    fun getItems(): Map<ItemStack, Int> =
        dataMap.toMap()

    fun get(item: ItemStack): Int =
        dataMap[singleStack(item)] ?: 0

    /**
     * Adds [amount] of [item], returning how many did not fit.
     */
    fun add(item: ItemStack, amount: Int): Int {
        val key = singleStack(item)
        val current = dataMap[key] ?: 0

        if (current == 0 && (getStoredItemTypeAmount() >= itemAmount || getStoredBytesAmount() >= capacity))
            return amount

        val free = capacity - getStoredBytesAmount()
        val stored = minOf(amount, free)
        if (stored <= 0)
            return amount

        dataMap[key] = current + stored
        return amount - stored
    }

    /**
     * Removes [amount] of [item], returning how many could not be removed.
     */
    fun remove(item: ItemStack, amount: Int): Int {
        val key = singleStack(item)
        val current = dataMap[key] ?: return amount

        if (current > amount) {
            dataMap[key] = current - amount
            return 0
        }

        dataMap.remove(key)
        return amount - current
    }

    private fun singleStack(item: ItemStack): ItemStack {
        val clone = item.clone()
        clone.amount = 1
        return clone
    }

}
