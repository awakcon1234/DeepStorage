package com.expectale.registry

import com.expectale.DeepStorage
import com.expectale.item.EmptySecurityCard
import com.expectale.item.SecurityCard
import com.expectale.storage_cell.StorageCell
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.NovaItemBuilder

@Init(stage = InitStage.PRE_PACK)
object Items {

    // Storage cells
    val STORAGE_CELL_1K = cell("storage_cell_1k")
    val STORAGE_CELL_4K = cell("storage_cell_4k")
    val STORAGE_CELL_16K = cell("storage_cell_16k")
    val STORAGE_CELL_64K = cell("storage_cell_64k")

    val STORAGE_CELLS: List<NovaItem> = listOf(STORAGE_CELL_1K, STORAGE_CELL_4K, STORAGE_CELL_16K, STORAGE_CELL_64K)

    // Cell components
    val CELL_COMPONENT_1K = component("cell_component_1k")
    val CELL_COMPONENT_4K = component("cell_component_4k")
    val CELL_COMPONENT_16K = component("cell_component_16k")
    val CELL_COMPONENT_64K = component("cell_component_64k")

    // Security cards
    val EMPTY_SECURITY_CARD = DeepStorage.item("empty_security_card") {
        behaviors(EmptySecurityCard)
        texture("item/security_card/empty_security_card")
    }
    val SECURITY_CARD = DeepStorage.item("security_card") {
        behaviors(SecurityCard)
        texture("item/security_card/security_card")
    }

    // Blocks
    val DEEP_STORAGE_UNIT = DeepStorage.registerItem(Blocks.DEEP_STORAGE_UNIT)

    private fun cell(name: String): NovaItem = DeepStorage.item(name) {
        behaviors(StorageCell)
        texture("item/cells/$name")
    }

    private fun component(name: String): NovaItem = DeepStorage.item(name) {
        texture("item/component/$name")
    }

    /**
     * Uses the flat texture at [path] (relative to the addon's texture root) as this item's model,
     * the way the old materials.json mapped item ids to texture paths.
     */
    private fun NovaItemBuilder.texture(path: String) {
        modelDefinition {
            model = buildModel { createLayeredModel(path) }
        }
    }

}
