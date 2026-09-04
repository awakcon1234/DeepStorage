package com.expectale.item

import com.expectale.registry.Items
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.addToInventoryOrDrop
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent

/**
 * A card without an owner. Shift right-clicking it turns it into a [SecurityCard] owned by the clicker.
 */
object EmptySecurityCard : ItemBehavior {

    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
        if (!player.isSneaking || !action.isRightClick || wrappedEvent.actionPerformed)
            return

        wrappedEvent.actionPerformed = true
        itemStack.subtract()

        val card = Items.SECURITY_CARD.createItemStack()
        Items.SECURITY_CARD.getBehaviorOrNull<SecurityCard>()?.setOwner(card, player)
        player.addToInventoryOrDrop(card)
    }

    override fun modifyClientSideStack(player: Player?, server: ItemStack, client: ItemStack): ItemStack {
        val lore = client.lore() ?: mutableListOf()
        lore += Component.translatable("item.deep_storage.empty_security_card.lore", NamedTextColor.GRAY).withoutPreFormatting()
        client.lore(lore)
        return client
    }

}
