package com.expectale.storage_cell

import com.expectale.DeepStorage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.item.retrieveData
import xyz.xenondevs.nova.util.item.storeData
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory

private const val DATA_KEY = "cell_data"

interface StorageCell {

    /**
     * How many items the [StorageCell] can store.
     */
    val capacity: Int

    /**
     * How many different items the [StorageCell] can store.
     */
    val itemAmount: Int

    /**
     * [Map] containing all the [ItemStacks][ItemStack] stored in the [StorageCell] with their amount.
     */
    fun getItems(cell: ItemStack): Map<ItemStack, Int>

    /**
     * Whether nothing is stored in [cell].
     */
    fun isEmpty(cell: ItemStack): Boolean

    fun toVirtual(cell: ItemStack): VirtualStorageCell

    fun setCellData(cell: ItemStack, cellData: CellData)

    companion object : ItemBehaviorFactory<Default> {

        override fun create(item: NovaItem): Default {
            return Default(
                item.config.entry<Int>("capacity"),
                item.config.entry<Int>("itemAmount")
            )
        }

        fun byteDisplay(value: Int, max: Int): Component =
            Component.translatable(
                "item.deep_storage.storage_cell.bytes",
                NamedTextColor.GRAY,
                Component.text(value, NamedTextColor.GREEN),
                Component.text(max, NamedTextColor.BLUE)
            )

        fun typeDisplay(value: Int, max: Int): Component =
            Component.translatable(
                "item.deep_storage.storage_cell.types",
                NamedTextColor.GRAY,
                Component.text(value, NamedTextColor.GREEN),
                Component.text(max, NamedTextColor.BLUE)
            )

    }

    class Default(
        capacity: Provider<Int>,
        itemAmount: Provider<Int>,
    ) : ItemBehavior, StorageCell {

        override val capacity by capacity
        override val itemAmount by itemAmount

        override fun getItems(cell: ItemStack): Map<ItemStack, Int> =
            getCellData(cell).getItems()

        override fun isEmpty(cell: ItemStack): Boolean {
            val cellData = getCellData(cell)
            return cellData.getStoredBytesAmount() == 0 && cellData.getStoredItemTypeAmount() == 0
        }

        override fun toVirtual(cell: ItemStack): VirtualStorageCell =
            VirtualStorageCell(getCellData(cell), cell.novaItem!!)

        override fun setCellData(cell: ItemStack, cellData: CellData) {
            cell.storeData(DeepStorage, DATA_KEY, cellData.dataMap)
        }

        private fun getCellData(cell: ItemStack): CellData {
            val map = cell.retrieveData<MutableMap<ItemStack, Int>>(DeepStorage, DATA_KEY) ?: mutableMapOf()
            return CellData(capacity, itemAmount, map)
        }

        override fun modifyClientSideStack(player: Player?, server: ItemStack, client: ItemStack): ItemStack {
            val cellData = getCellData(server)

            val lore = client.lore() ?: mutableListOf()
            lore += byteDisplay(cellData.getStoredBytesAmount(), cellData.capacity).withoutPreFormatting()
            lore += typeDisplay(cellData.getStoredItemTypeAmount(), cellData.itemAmount).withoutPreFormatting()
            client.lore(lore)

            return client
        }

    }

}
