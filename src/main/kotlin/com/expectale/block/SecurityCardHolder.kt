package com.expectale.block

import com.expectale.item.SecurityCard
import org.bukkit.entity.Player
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.world.block.tileentity.TileEntity

interface SecurityCardHolder {

    val cardInventory: VirtualInventory

    var whiteList: Boolean

    /**
     * Whether [player] may manage the card inventory: the owner, operators and bypass permission holders.
     */
    fun canInputCard(player: Player): Boolean

    /**
     * Whether a card owned by [player] sits in the card inventory.
     */
    fun hasCardAccess(player: Player): Boolean =
        cardInventory.items.any { item ->
            item != null && item.novaItem?.getBehaviorOrNull<SecurityCard>()?.isOwner(item, player) == true
        }

    fun handleCardUpdate(event: ItemPreUpdateEvent) {
        if (event.updateReason == TileEntity.SELF_UPDATE_REASON)
            return

        val newItem = event.newItem ?: return
        val reason = event.updateReason as? PlayerUpdateReason ?: return

        val card = newItem.novaItem?.getBehaviorOrNull<SecurityCard>()
        if (card?.getOwner(newItem) == null || !canInputCard(reason.player()))
            event.isCancelled = true
    }

}
