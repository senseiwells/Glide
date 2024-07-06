package me.senseiwells.glide.utils

import com.mojang.serialization.MapCodec
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.addon.serialization.CheckpointTemplate
import net.casual.arcade.utils.registries.RegistryKeySupplier
import net.casual.arcade.utils.registries.RegistrySupplier

object GlideRegistryKeys: RegistryKeySupplier(GlideMod.MOD_ID) {
    val CHECKPOINT_TEMPLATE = create<MapCodec<out CheckpointTemplate>>("checkpoint_template")
}

object GlideRegistries: RegistrySupplier() {
    val CHECKPOINT_TEMPLATE = create(GlideRegistryKeys.CHECKPOINT_TEMPLATE, CheckpointTemplate::bootstrap)
}