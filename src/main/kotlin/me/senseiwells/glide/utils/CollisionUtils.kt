package me.senseiwells.glide.utils

import net.minecraft.world.phys.AABB

fun AABB.intersects(swept: Iterable<AABB>): Boolean {
    return swept.any(this::intersects)
}