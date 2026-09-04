package com.expectale.item

import com.expectale.DeepStorage
import com.expectale.registry.Items
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.addToInventoryOrDrop
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.util.item.retrieveData
import xyz.xenondevs.nova.util.item.storeData
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import java.util.UUID

private const val OWNER_KEY = "owner"

/**
 * A card bound to one player. Placed in a unit's card inventory, it grants that player access
 * while the unit's whitelist is on. Shift right-clicking it hands back an [EmptySecurityCard].
 */
interface SecurityCard {

    fun setOwner(card: ItemStack, player: Player)

    fun getOwner(card: ItemStack): UUID?

    fun clear(card: ItemStack)

    fun isOwner(card: ItemStack, player: Player): Boolean =
        player.uniqueId == getOwner(card)

    companion object : ItemBehaviorFactory<Default> {

        override fun create(item: NovaItem): Default = Default()

    }

    class Default : ItemBehavior, SecurityCard {

        override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
            if (!player.isSneaking || !action.isRightClick || wrappedEvent.actionPerformed)
                return

            wrappedEvent.actionPerformed = true
            itemStack.subtract()
            player.addToInventoryOrDrop(Items.EMPTY_SECURITY_CARD.createItemStack())
        }

        override fun setOwner(card: ItemStack, player: Player) {
            if (getOwner(card) != null)
                return

            card.storeData(DeepStorage, OWNER_KEY, player.uniqueId)
        }

        override fun getOwner(card: ItemStack): UUID? =
            card.retrieveData<UUID>(DeepStorage, OWNER_KEY)

        override fun clear(card: ItemStack) {
            card.storeData<UUID>(DeepStorage, OWNER_KEY, null)
        }

        override fun modifyClientSideStack(player: Player?, server: ItemStack, client: ItemStack): ItemStack {
            val owner = getOwner(server)
            val ownerName = owner?.let { Bukkit.getOfflinePlayer(it).name } ?: ""

            client.editMeta { meta ->
                meta.itemName(
                    Component.translatable(
                        "item.deep_storage.security_card",
                        Component.text(ownerName, NamedTextColor.BLUE)
                    ).withoutPreFormatting()
                )
            }

            val lore = client.lore() ?: mutableListOf()
            lore += Component.translatable("item.deep_storage.security_card.lore", NamedTextColor.GRAY).withoutPreFormatting()
            client.lore(lore)

            return client
        }

    }

}
