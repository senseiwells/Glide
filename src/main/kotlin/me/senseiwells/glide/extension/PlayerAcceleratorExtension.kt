package me.senseiwells.glide.extension

import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.entity.accelerator.Accelerator
import me.senseiwells.glide.minigame.GlideMinigame
import net.casual.arcade.extensions.PlayerExtension
import net.casual.arcade.extensions.SerializableExtension
import net.casual.arcade.minigame.utils.MinigameUtils.getMinigame
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

class PlayerAcceleratorExtension(
    player: ServerPlayer
): PlayerExtension(player), SerializableExtension {
    fun checkThermalAreas() {
        val glide = this.player.getMinigame() as? GlideMinigame ?: return
        if (this.player.level() != glide.level) {
            return
        }

        val accelerators = glide.level.getEntities(ACCELERATOR_TYPE_TEST) { true }

    }

    override fun id(): Identifier {
        return GlideMod.id("accelerator_extension")
    }

    override fun serialize(output: ValueOutput) {

    }

    override fun deserialize(input: ValueInput) {

    }

    companion object {
        private val ACCELERATOR_TYPE_TEST = EntityTypeTest.forClass<Entity, _>(Accelerator::class.java)
    }
}