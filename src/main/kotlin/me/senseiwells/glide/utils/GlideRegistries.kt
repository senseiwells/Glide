package me.senseiwells.glide.utils

import com.mojang.serialization.MapCodec
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.checkpoint.Checkpoint
import net.casual.arcade.utils.registries.RegistryKeySupplier
import net.casual.arcade.utils.registries.RegistrySupplier

object GlideRegistryKeys: RegistryKeySupplier(GlideMod.MOD_ID) {
    val CHECKPOINT_TEMPLATE = create<MapCodec<out Checkpoint>>("checkpoint_template")
}

object GlideRegistries: RegistrySupplier() {
    val CHECKPOINT_TEMPLATE = create(GlideRegistryKeys.CHECKPOINT_TEMPLATE, Checkpoint::bootstrap)
}