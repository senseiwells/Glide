package me.senseiwells.glide.utils

import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Entity
import java.util.function.Consumer

object EntityUtils {
    fun <T: Any> EntityDataAccessor<T>.createValue(value: T): SynchedEntityData.DataValue<T> {
        return SynchedEntityData.DataValue.create(this, value)
    }

    fun <T> replaceSynchedData(
        data: MutableList<SynchedEntityData.DataValue<*>>,
        accessor: EntityDataAccessor<T>,
        replacer: (T, Consumer<SynchedEntityData.DataValue<*>>) -> Unit
    ) {
        val iter = data.listIterator()
        for (entry in iter) {
            if (entry.id == accessor.id) {
                iter.remove()
                replacer.invoke(entry.value as T, iter::add)
                break
            }
        }
    }

    fun Entity.addVelocityAndMark(deltaX: Double, deltaY: Double, deltaZ: Double) {
        this.push(deltaX, deltaY, deltaZ)
        this.hurtMarked = true
    }

    @JvmStatic
    fun Entity.setVelocityAndMark(deltaX: Double, deltaY: Double, deltaZ: Double) {
        this.setDeltaMovement(deltaX, deltaY, deltaZ)
        this.hurtMarked = true
    }
}