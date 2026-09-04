package com.expectale.protection

import com.expectale.tileentity.DeepStorageUnit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.Nova
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.tileentity.TileEntity
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.pos

/**
 * Keeps players without access from opening or breaking a whitelisted unit, and keeps other
 * tile entities (quarries, block breakers) from breaking any unit at all.
 */
@Init(stage = InitStage.POST_WORLD)
object DeepStorageProtection : ProtectionIntegration {

    @InitFun
    private fun register() {
        Nova.getNova().registerProtectionIntegration(this)
    }

    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        isAllowed(player, location)

    override fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean =
        unitAt(location) == null

    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean =
        true

    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        isAllowed(player, location)

    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean =
        true

    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
        true

    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
        true

    private fun isAllowed(player: OfflinePlayer, location: Location): Boolean {
        val unit = unitAt(location) ?: return true
        val online = player.player ?: return false
        return unit.isAllowed(online)
    }

    private fun unitAt(location: Location): DeepStorageUnit? =
        WorldDataManager.getTileEntity(location.pos) as? DeepStorageUnit

}
