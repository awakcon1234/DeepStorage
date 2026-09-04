package com.expectale.registry

import com.expectale.DeepStorage
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.world.item.NovaItem

/**
 * The GUI items, drawn in the style of Nova's own: buttons are opaque 16x16 plates with the
 * standard dark frame, placeholders are dark outlines on a transparent background. The old
 * `nova:item/gui/gui_item` model parent no longer exists; [createGuiModel] composes the
 * inventory background behind the texture instead.
 */
@Init(stage = InitStage.PRE_PACK)
object GuiItems {

    val STORAGE_CELL_PLACEHOLDER = placeholder("storage_cell")
    val SECURITY_CARD_PLACEHOLDER = placeholder("security_card")

    val STORAGE_CELLS_BTN = button("storage_cells", "menu.deep_storage.items.storage_cell")
    val SECURITY_CARDS_BTN = button("security_cards", "menu.deep_storage.items.security_card")
    val SORT_ALPHABETICAL_BTN = button("sort_alphabetical", "menu.deep_storage.items.alphabetical_sort")
    val SORT_STACK_BTN = button("sort_stack", "menu.deep_storage.items.stack_sort")
    val WHITELIST_ON_BTN = button("whitelist_on", "menu.deep_storage.items.whitelist_on")
    val WHITELIST_OFF_BTN = button("whitelist_off", "menu.deep_storage.items.whitelist_off")

    private fun button(name: String, localizedName: String): NovaItem = DeepStorage.item("gui/opaque/btn/$name") {
        localizedName(localizedName)
        hidden(true)

        modelDefinition {
            model = buildModel {
                createGuiModel(background = true, stretched = false, "item/gui/btn/$name")
            }
        }
    }

    private fun placeholder(name: String): NovaItem = DeepStorage.item("gui/transparent/placeholder/$name") {
        name(null)
        hidden(true)

        modelDefinition {
            model = buildModel {
                createGuiModel(background = false, stretched = false, "item/gui/placeholder/$name")
            }
        }
    }

}
