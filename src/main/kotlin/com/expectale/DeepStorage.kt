package com.expectale

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.update.ProjectDistributor

object DeepStorage : Addon() {

    override val projectDistributors = listOf(ProjectDistributor.github("awakcon1234/DeepStorage"))

}
