package me.senseiwells.glide.checkpoint

import com.google.common.collect.HashMultimap
import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import it.unimi.dsi.fastutil.doubles.DoubleList
import me.senseiwells.glide.font.GlideFont
import net.casual.arcade.utils.ComponentUtils
import net.casual.arcade.utils.ComponentUtils.shadowless
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

class CheckpointManager(
    private val checkpoints: List<Checkpoint>
) {
    private val lastIndex = this.checkpoints.maxOf { it.index }
    private val byIndex = this.createCheckpointsByIndex()
    private val distances = this.calculateCheckpointDistances()

    val totalLength: Double = this.distances.last()
    val progressBar = this.createProgressBar()

    init {
        this.validate()
    }

    fun get(index: Int): Collection<Checkpoint> {
        return this.byIndex.get(index)
    }

    fun isFinish(index: Int): Boolean {
        return index == this.lastIndex
    }

    fun getNearest(index: Int, position: Vec3): Checkpoint {
        val checkpoints = this.get(index)
        require(checkpoints.isNotEmpty()) {
            "Cannot get nearest checkpoint because there are none at index $index"
        }

        return checkpoints.minBy { it.position.distanceTo(position) }
    }

    fun getNearestDistance(index: Int, position: Vec3): Double {
        val checkpoints = this.get(index)
        require(checkpoints.isNotEmpty()) {
            "Cannot get nearest checkpoint because there are none at index $index"
        }

        return checkpoints.minOf { it.position.distanceTo(position) }
    }

    fun getDistance(index: Int): Double {
        if (index !in this.distances.indices) {
            return 0.0
        }
        return this.distances.getDouble(index)
    }

    fun getPercentageDistance(index: Int): Double {
        return this.getDistance(index) / this.totalLength
    }

    fun all(): List<Checkpoint> {
        return this.checkpoints
    }

    private fun createProgressBar(): Component {
        val progressBar = GlideFont.Progressbar.BACKGROUND.append(ComponentUtils.space(-306))
        var lastOffset = 0
        for (checkpoint in this.all()) {
            if (!checkpoint.shouldMarkOnProgressBar) {
                continue
            }

            val distance = this.getPercentageDistance(checkpoint.index)
            val offset = Mth.clamp(Math.round(distance * 300).toInt(), 0, 300)

            progressBar.append(ComponentUtils.space(offset - lastOffset))
            progressBar.append(GlideFont.Progressbar.getCheckpoint(0))
            lastOffset = offset + 3
        }
        progressBar.append(ComponentUtils.space(306 - lastOffset))
        return progressBar.shadowless()
    }

    private fun createCheckpointsByIndex(): HashMultimap<Int, Checkpoint> {
        val map = HashMultimap.create<Int, Checkpoint>()
        for (checkpoint in this.checkpoints) {
            map.put(checkpoint.index, checkpoint)
        }
        return map
    }

    private fun calculateCheckpointDistances(): DoubleList {
        val distances = DoubleArrayList()
        distances.add(0.0)
        var totalDistance = 0.0
        var previousPosition = this.get(0).first().position
        for (index in 1.. this.lastIndex) {
            val closestCheckpoint = this.getNearest(index, previousPosition)
            totalDistance += closestCheckpoint.position.distanceTo(previousPosition)
            previousPosition = closestCheckpoint.position
            distances.add(totalDistance)
        }
        return distances
    }

    private fun validate() {
        for (checkpoint in this.get(this.lastIndex)) {
            require(checkpoint is FinishLine) { "Last checkpoints should be finish lines!" }
        }
    }
}