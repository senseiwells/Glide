package me.senseiwells.glide.extension

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.senseiwells.glide.entity.GlideCollider
import me.senseiwells.glide.entity.accelerator.Accelerator
import me.senseiwells.glide.entity.checkpoint.Checkpoint
import me.senseiwells.glide.entity.checkpoint.FinishLine
import me.senseiwells.glide.minigame.GlideMinigame
import net.casual.arcade.extensions.DataExtension
import net.casual.arcade.extensions.PlayerExtension
import net.casual.arcade.utils.PlayerUtils.getExtension
import net.casual.arcade.utils.location.Location
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerPlayer
import org.apache.commons.lang3.mutable.MutableInt

class PlayerGlideExtension(
    player: ServerPlayer
): PlayerExtension(player), DataExtension {
    private val accelerators = Object2ObjectOpenHashMap<Accelerator, MutableInt>()

    private var lastCheckpoint = 0
    private var finished = false
    var liftForceModifier = 1.0
        private set

    var respawnLocation = Location.of()

    fun collideWithAccelerator(accelerator: Accelerator): Boolean {
        if (!this.accelerators.containsKey(accelerator)) {
            this.accelerators[accelerator] = MutableInt()
            return true
        }
        return false
    }

    fun collideWithCheckpoint(checkpoint: Checkpoint): Boolean {
        if (this.lastCheckpoint + 1 == checkpoint.index) {
            println("Collided next with checkpoint!")
            this.lastCheckpoint = checkpoint.index
            return true
        }
        println("Collided with invalid checkpoint!")
        return false
    }

    fun collideWithFinishLine(finish: FinishLine): Boolean {
        if (!this.finished) {
            this.finished = true;
            return true
        }
        return false
    }

    fun setLiftForceModifier(modifier: Double) {
        if (modifier > this.liftForceModifier) {
            this.liftForceModifier = modifier
        }
    }

    fun tick(minigame: GlideMinigame) {
        // This should be big enough for any intersecting entities
        this.player.xo
        val boundingBox = this.player.boundingBox.inflate(40.0)
        val intersecting = this.player.serverLevel().getEntities(this.player, boundingBox)
        for (entity in intersecting) {
            if (entity is GlideCollider && entity.boundingBox.intersects(this.player.boundingBox)) {
                entity.collide(this.player, minigame)
            }
        }

        this.liftForceModifier = 1.0
        val iterator = this.accelerators.iterator()
        for (entry in iterator) {
            val (accelerator, ticks) = entry
            if (!accelerator.accelerate(this.player, ticks)) {
                iterator.remove()
            }
        }
    }

    fun clearAccelerators() {
        this.liftForceModifier = 1.0
        this.player.isNoGravity = false
        this.accelerators.clear()
    }

    fun reset() {
        this.finished = false
        this.lastCheckpoint = 0
        this.clearAccelerators()
    }

    override fun getName(): String {
        return "glide_extension"
    }

    override fun deserialize(element: Tag) {

    }

    override fun serialize(): Tag? {
        return null
    }

    companion object {
        @JvmStatic
        val ServerPlayer.glide
            get() = this.getExtension(PlayerGlideExtension::class.java)
    }
}