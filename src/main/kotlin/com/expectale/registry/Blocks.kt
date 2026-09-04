package com.expectale.registry

import com.expectale.DeepStorage
import com.expectale.tileentity.DeepStorageUnit
import org.bukkit.Material
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.resources.builder.layout.block.BackingStateCategory
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.behavior.BlockSounds
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.block.behavior.TileEntityDrops
import xyz.xenondevs.nova.world.block.behavior.TileEntityInteractive
import xyz.xenondevs.nova.world.block.behavior.TileEntityLimited
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.block.state.property.DefaultScopedBlockStateProperties.FACING_HORIZONTAL
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers

@Init(stage = InitStage.PRE_PACK)
object Blocks {

    private val STONE = Breakable(3.0, setOf(VanillaToolCategories.PICKAXE), VanillaToolTiers.IRON, false, Material.STONE)

    val DEEP_STORAGE_UNIT: NovaTileEntityBlock = DeepStorage.tileEntity("deep_storage_unit", ::DeepStorageUnit) {
        behaviors(STONE, BlockSounds(SoundGroup.STONE), TileEntityLimited, TileEntityDrops, TileEntityInteractive)
        stateProperties(FACING_HORIZONTAL)
        stateBacked(BackingStateCategory.NOTE_BLOCK, BackingStateCategory.MUSHROOM_BLOCK) {
            defaultModel.rotated()
        }
    }

}
