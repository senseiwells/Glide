package me.senseiwells.glide.utils

import net.minecraft.util.Mth
import kotlin.math.roundToInt

object ColorUtils {
    fun blend(from: Int, to: Int, delta: Float): Int {
        val clamped = Mth.clamp(delta, 0.0F, 1.0F)
        val inverse = 1.0f - clamped

        val red = ((from shr 16 and 0xff) * inverse + (to shr 16 and 0xff) * clamped).roundToInt()
        val green = ((from shr 8 and 0xff) * inverse + (to shr 8 and 0xff) * clamped).roundToInt()
        val blue = ((from and 0xff) * inverse + (to and 0xff) * clamped).roundToInt()

        return (red shl 16) or (green shl 8) or blue
    }
}