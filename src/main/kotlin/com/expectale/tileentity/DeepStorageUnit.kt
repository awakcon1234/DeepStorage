package com.expectale.tileentity

import com.expectale.block.SecurityCardHolder
import com.expectale.block.StorageCellHolder
import com.expectale.registry.Blocks.DEEP_STORAGE_UNIT
import com.expectale.registry.GuiItems
import com.expectale.registry.Items
import com.expectale.storage_cell.CellData
import com.expectale.storage_cell.StorageCell
import com.expectale.storage_cell.VirtualStorageCell
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPostUpdateEvent
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.event.UpdateReason
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.window.Window
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.ui.menu.item.BackItem
import xyz.xenondevs.nova.ui.menu.sideconfig.OpenSideConfigItem
import xyz.xenondevs.nova.ui.menu.sideconfig.SideConfigMenu
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.addToInventoryOrDrop
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.playClickSound
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.world.block.tileentity.menu.TileEntityMenuClass
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.item.NovaItem
import java.util.UUID
import kotlin.math.min

private val PREVENT_INFINITE_STORAGE by DEEP_STORAGE_UNIT.config.entry<Boolean>("prevent-infinite-storage")

private const val BYPASS_PERMISSION = "deep_storage.security.bypass"
private const val CELL_SLOTS = 12
private const val CARD_SLOTS = 14
private const val CONTENT_COLUMNS = 7
private const val CONTENT_ROWS = 3

/**
 * How many slots the item network sees. Networks allocate their snapshot arrays once, when they
 * are built, so this cannot follow the cells that happen to be inserted: it is the type capacity
 * of a unit filled with the largest cells there are.
 */
private val NETWORK_SLOTS: Int by lazy {
    CELL_SLOTS * Items.STORAGE_CELLS.maxOf { it.getBehaviorOrNull<StorageCell>()!!.itemAmount }
}

class DeepStorageUnit(pos: BlockPos, blockState: NovaBlockState, data: Compound) :
    NetworkedTileEntity(pos, blockState, data), StorageCellHolder, SecurityCardHolder {

    // display stacks only; the real cells live in virtualMap and in the persistent compound
    override val cellInventory: VirtualInventory = VirtualInventory(null, IntArray(CELL_SLOTS) { 1 }).apply {
        addPreUpdateHandler(::handleCellUpdate)
        addPostUpdateHandler(::handlePostCellUpdate)
    }

    override val virtualMap: HashMap<Int, VirtualStorageCell> = loadCells()

    override val cardInventory: VirtualInventory = storedInventory(
        "card", CARD_SLOTS,
        persistent = true,
        maxStackSizes = IntArray(CARD_SLOTS) { 1 },
        preUpdateHandler = ::handleCardUpdate
    )

    private val inputInventory = VirtualInventory(null, 1).apply {
        addPreUpdateHandler(::handlePreInput)
        addPostUpdateHandler(::handlePostInput)
    }

    private val inventory = DeepStorageInventory()

    override var whiteList: Boolean by storedValue("whitelist") { false }
    private var sortMode: SortMode by storedValue("sortMode") { SortMode.HIGHER_AMOUNT }

    init {
        storedItemHolder(inventory to NetworkConnectionType.BUFFER, blockedSides = setOf(BlockSide.FRONT))
        updateCellInv()
    }

    override fun callUpdateCell() {
        inventory.rebuildIndex()
        menuContainer.forEachMenu(DeepStorageUnitMenu::updateContent)
    }

    override fun canInputCard(player: Player): Boolean =
        player.isOp || player.hasPermission(BYPASS_PERMISSION) || player.uniqueId == ownerUuid

    fun hasAccess(player: Player): Boolean =
        canInputCard(player) || hasCardAccess(player)

    /**
     * Whether [player] may open or break this unit: everybody while the whitelist is off, otherwise only those with [hasAccess].
     */
    fun isAllowed(player: Player): Boolean =
        !whiteList || hasAccess(player)

    /**
     * Cells are persisted per slot as the cell's item type plus its contents map. The map stored is
     * the very object the [VirtualStorageCell] mutates, so Nova serializes the current contents
     * whenever it saves the compound; nothing has to be written back on each transfer.
     * Persistent keys travel inside the dropped unit item, as the original addon's did.
     */
    private fun loadCells(): HashMap<Int, VirtualStorageCell> {
        val cells = HashMap<Int, VirtualStorageCell>()

        // layout of the original addon: one inventory of real cell items
        val legacy = retrieveDataOrNull<VirtualInventory>("cells")
        if (legacy != null) {
            removeData("cells")
            for ((slot, cell) in fromInventory(legacy)) {
                cells[slot] = cell
                cellInserted(slot, cell)
            }
            return cells
        }

        for (slot in 0..<CELL_SLOTS) {
            val type = retrieveDataOrNull<NovaItem>(cellTypeKey(slot)) ?: continue
            val behavior = type.getBehaviorOrNull<StorageCell>() ?: continue
            val contents = retrieveDataOrNull<MutableMap<ItemStack, Int>>(cellDataKey(slot)) ?: mutableMapOf()

            val cell = VirtualStorageCell(CellData(behavior.capacity, behavior.itemAmount, contents), type)
            cells[slot] = cell
            // re-store so the compound holds this very map instance, not a stale deserialized copy
            cellInserted(slot, cell)
        }

        return cells
    }

    override fun cellInserted(slot: Int, cell: VirtualStorageCell) {
        storeData(cellTypeKey(slot), cell.novaItem, true)
        storeData(cellDataKey(slot), cell.cellData.dataMap, true)
    }

    override fun cellRemoved(slot: Int) {
        removeData(cellTypeKey(slot))
        removeData(cellDataKey(slot))
    }

    private fun cellTypeKey(slot: Int): String =
        "cell_${slot}_type"

    private fun cellDataKey(slot: Int): String =
        "cell_${slot}_data"

    private fun handlePreInput(event: ItemPreUpdateEvent) {
        if (event.updateReason == SELF_UPDATE_REASON || !event.isAdd)
            return

        val item = event.newItem ?: return
        if (!inventory.accepts(item))
            event.isCancelled = true
    }

    private fun handlePostInput(event: ItemPostUpdateEvent) {
        if (event.updateReason == SELF_UPDATE_REASON)
            return

        val item = event.newItem ?: return
        val rest = inventory.insert(item)
        val leftover = if (rest > 0) item.clone().apply { amount = rest } else null
        inputInventory.setItem(UpdateReason.SUPPRESSED, 0, leftover)
    }

    enum class SortMode {
        ALPHABETICAL, HIGHER_AMOUNT
    }

    @TileEntityMenuClass
    inner class DeepStorageUnitMenu : GlobalTileEntityMenu() {

        private val sideConfigMenu = SideConfigMenu(
            this@DeepStorageUnit,
            inventories = mapOf(inventory to "inventory.nova.default"),
            openPrevious = ::openWindow
        )

        private val sortButton = SortButton()
        private val whitelistButton = WhitelistButton()

        override val gui: ScrollGui<Item> = ScrollGui.itemsBuilder()
            .setStructure(
                "1 - - - - - - - 2",
                "| i # # k a o s |",
                "| x x x x x x x u",
                "| x x x x x x x |",
                "| x x x x x x x d",
                "3 - - - - - - - 4")
            .addIngredient('i', inputInventory)
            .addIngredient('k', OpenCellsItem())
            .addIngredient('a', OpenCardsItem())
            .addIngredient('o', sortButton)
            .addIngredient('s', OpenSideConfigItem(sideConfigMenu))
            .setContent(createContent())
            .build()

        private val cellGui: Gui = Gui.builder()
            .setStructure(
                "# # 1 - - - 2 # #",
                "# # | c c c | # #",
                "# # | c c c | # #",
                "# # | c c c | # #",
                "# # | c c c | # #",
                "b # 3 - - - 4 # #")
            .addIngredient('c', cellInventory, GuiItems.STORAGE_CELL_PLACEHOLDER.clientsideProvider)
            .addIngredient('b', BackItem(openPrevious = ::openWindow))
            .build()

        private val cardGui: Gui = Gui.builder()
            .setStructure(
                "# # # # # # # # w",
                "1 - - - - - - - 2",
                "| c c c c c c c |",
                "| c c c c c c c |",
                "3 - - - - - - - 4",
                "b # # # # # # # #")
            .addIngredient('c', cardInventory, GuiItems.SECURITY_CARD_PLACEHOLDER.clientsideProvider)
            .addIngredient('w', whitelistButton)
            .addIngredient('b', BackItem(openPrevious = ::openWindow))
            .build()

        override fun openWindow(player: Player) {
            if (!isAllowed(player)) {
                player.sendMessage(Component.translatable("message.deep_storage.not_whitelisted", NamedTextColor.DARK_RED))
                return
            }

            super.openWindow(player)
        }

        fun updateContent() {
            gui.setContent(createContent())
        }

        private fun openSubWindow(player: Player, subGui: Gui, title: String) {
            val window = Window.builder()
                .setUpperGui(subGui)
                .setTitle(Component.translatable(title))
                .build(player)

            menuContainer.registerWindow(window)
            window.open()
        }

        /**
         * The stored items in the chosen order, padded with deposit slots so the visible area is
         * always filled and there is always an empty row to drop items into.
         */
        private fun createContent(): List<Item> {
            val displays = getItems().entries.map { (item, amount) -> ItemDisplay(item, amount) }
            val sorted = when (sortMode) {
                SortMode.HIGHER_AMOUNT -> displays.sortedByDescending { it.amount }
                SortMode.ALPHABETICAL -> displays.sortedBy { it.name }
            }

            val rows = maxOf(CONTENT_ROWS, sorted.size / CONTENT_COLUMNS + 1)
            val padding = rows * CONTENT_COLUMNS - sorted.size
            return sorted + List(padding) { DepositSlot() }
        }

        /**
         * Puts the cursor stack (or one item of it on a right click) into the cells.
         * Returns false when the cursor was empty, so the caller can treat the click as a withdrawal.
         */
        private fun depositCursor(player: Player, clickType: ClickType): Boolean {
            val cursor = player.itemOnCursor
            if (cursor.type.isAir)
                return false

            val portion = if (clickType == ClickType.RIGHT) 1 else cursor.amount
            val rest = inventory.insert(cursor.clone().apply { amount = portion })
            val stored = portion - rest
            if (stored > 0) {
                val remaining = cursor.amount - stored
                player.setItemOnCursor(if (remaining > 0) cursor.clone().apply { amount = remaining } else null)
            }

            return true
        }

        private inner class OpenCellsItem : AbstractItem() {

            override fun getItemProvider(player: Player): ItemProvider =
                GuiItems.STORAGE_CELLS_BTN.clientsideProvider

            override fun handleClick(clickType: ClickType, player: Player, click: Click) {
                player.playClickSound()
                openSubWindow(player, cellGui, "menu.deep_storage.storage_cell_inventory")
            }

        }

        private inner class OpenCardsItem : AbstractItem() {

            override fun getItemProvider(player: Player): ItemProvider =
                GuiItems.SECURITY_CARDS_BTN.clientsideProvider

            override fun handleClick(clickType: ClickType, player: Player, click: Click) {
                if (!canInputCard(player))
                    return

                player.playClickSound()
                openSubWindow(player, cardGui, "menu.deep_storage.security_card_inventory")
            }

        }

        private inner class WhitelistButton : AbstractItem() {

            override fun getItemProvider(player: Player): ItemProvider =
                (if (whiteList) GuiItems.WHITELIST_ON_BTN else GuiItems.WHITELIST_OFF_BTN).clientsideProvider

            override fun handleClick(clickType: ClickType, player: Player, click: Click) {
                player.playClickSound()
                whiteList = !whiteList
                notifyWindows()
            }

        }

        private inner class SortButton : AbstractItem() {

            override fun getItemProvider(player: Player): ItemProvider =
                (if (sortMode == SortMode.ALPHABETICAL) GuiItems.SORT_ALPHABETICAL_BTN else GuiItems.SORT_STACK_BTN).clientsideProvider

            override fun handleClick(clickType: ClickType, player: Player, click: Click) {
                player.playClickSound()
                sortMode = if (sortMode == SortMode.ALPHABETICAL) SortMode.HIGHER_AMOUNT else SortMode.ALPHABETICAL
                notifyWindows()
                updateContent()
            }

        }

        /**
         * An empty content slot: clicking it with something on the cursor stores that.
         */
        private inner class DepositSlot : AbstractItem() {

            override fun getItemProvider(player: Player): ItemProvider =
                ItemProvider.EMPTY

            override fun handleClick(clickType: ClickType, player: Player, click: Click) {
                depositCursor(player, clickType)
            }

        }

        private inner class ItemDisplay(val item: ItemStack, val amount: Int) : AbstractItem() {

            val name: String = ItemUtils.getName(item).toPlainText()

            override fun getItemProvider(player: Player): ItemProvider =
                ItemBuilder(item.clone())
                    .setName(ItemUtils.getName(item).append(Component.text(" x$amount", NamedTextColor.GREEN)))

            override fun handleClick(clickType: ClickType, player: Player, click: Click) {
                if (depositCursor(player, clickType))
                    return

                val available = getItemAmount(item)
                if (available <= 0) {
                    updateContent()
                    return
                }

                val stackSize = min(item.maxStackSize, available)
                when (clickType) {
                    ClickType.LEFT -> withdraw(stackSize)?.let(player::setItemOnCursor)
                    ClickType.RIGHT -> withdraw(1)?.let(player::setItemOnCursor)
                    ClickType.SHIFT_LEFT -> withdraw(stackSize)?.let { player.addToInventoryOrDrop(it) }
                    ClickType.SHIFT_RIGHT -> withdraw(1)?.let { player.addToInventoryOrDrop(it) }
                    ClickType.MIDDLE -> {
                        if (player.gameMode == GameMode.CREATIVE)
                            player.setItemOnCursor(item.clone().apply { amount = item.maxStackSize })
                    }

                    else -> Unit
                }
            }

            private fun withdraw(count: Int): ItemStack? {
                val stack = item.clone().apply { amount = count }
                val rest = removeItem(stack, count)
                inventory.rebuildIndex()
                menuContainer.forEachMenu(DeepStorageUnitMenu::updateContent)

                val taken = count - rest
                if (taken <= 0)
                    return null

                return stack.apply { amount = taken }
            }

        }

    }

    /**
     * The face the item network sees: one slot per stored item type, in the order the cells hold
     * them. Networks snapshot the slots with [copyContents] before a tick and address them by index
     * afterwards, so [take] leaves a hole instead of shifting the slots behind it; the order is
     * rebuilt on the next snapshot.
     */
    inner class DeepStorageInventory : NetworkedInventory {

        override val uuid: UUID = this@DeepStorageUnit.uuid

        override val size: Int
            get() = NETWORK_SLOTS

        private val index = ArrayList<ItemStack?>()

        /**
         * Whether [item] may go into the cells at all: with `prevent-infinite-storage` on, a cell
         * that holds something cannot be stored inside another cell.
         */
        fun accepts(item: ItemStack): Boolean {
            if (!PREVENT_INFINITE_STORAGE)
                return true

            val cell = item.novaItem?.getBehaviorOrNull<StorageCell>() ?: return true
            return cell.isEmpty(item)
        }

        /**
         * Stores as much of [item] as fits and returns the amount left over.
         */
        fun insert(item: ItemStack): Int {
            if (!accepts(item))
                return item.amount

            val rest = addItemToCell(item)
            if (rest < item.amount) {
                track(item)
                menuContainer.forEachMenu(DeepStorageUnitMenu::updateContent)
            }

            return rest
        }

        fun rebuildIndex() {
            index.clear()
            for (item in getItems().keys) {
                if (index.size >= NETWORK_SLOTS)
                    break

                index += item
            }
        }

        private fun track(item: ItemStack) {
            if (index.any { it != null && it.isSimilar(item) })
                return

            val key = item.clone().apply { amount = 1 }
            val hole = index.indexOf(null)
            if (hole >= 0) {
                index[hole] = key
            } else if (index.size < NETWORK_SLOTS) {
                index += key
            }
        }

        override fun add(itemStack: ItemStack, amount: Int): Int =
            insert(itemStack.clone().apply { this.amount = amount })

        override fun canTake(slot: Int, amount: Int): Boolean {
            val item = index.getOrNull(slot) ?: return false
            return getItemAmount(item) >= amount
        }

        override fun take(slot: Int, amount: Int) {
            val item = index.getOrNull(slot) ?: return
            removeItem(item, amount)
            if (getItemAmount(item) == 0)
                index[slot] = null

            menuContainer.forEachMenu(DeepStorageUnitMenu::updateContent)
        }

        override fun isFull(): Boolean =
            virtualMap.values.all { it.cellData.getStoredBytesAmount() >= it.cellData.capacity }

        override fun isEmpty(): Boolean =
            index.all { it == null }

        override fun copyContents(destination: Array<ItemStack>) {
            val items = getItems()
            index.clear()
            for (item in items.keys) {
                if (index.size >= NETWORK_SLOTS)
                    break

                index += item
            }

            for (slot in destination.indices) {
                val item = index.getOrNull(slot)
                destination[slot] = if (item == null) {
                    ItemStack.empty()
                } else {
                    item.clone().apply { amount = min(item.maxStackSize, items[item] ?: 0) }
                }
            }
        }

    }

}
