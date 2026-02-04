package me.senseiwells.glide.entity

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.entity.accelerator.BoosterAccelerator
import me.senseiwells.glide.entity.accelerator.DirectionalBoosterAccelerator
import me.senseiwells.glide.entity.accelerator.StaticThermalAccelerator
import me.senseiwells.glide.entity.accelerator.ThermalAccelerator
import me.senseiwells.glide.entity.checkpoint.BeaconBeam
import me.senseiwells.glide.entity.ring.Ring
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory

object GlideEntities {
    val DYNAMIC_THERMAL = register(
        "dynamic_thermal",
        EntityType.Builder.of(::ThermalAccelerator, MobCategory.MISC).clientTrackingRange(32)
    )
    val STATIC_THERMAL = register(
        "static_thermal",
        EntityType.Builder.of(::StaticThermalAccelerator, MobCategory.MISC).clientTrackingRange(32)
    )
    val BOOSTER = register(
        "booster",
        EntityType.Builder.of(::BoosterAccelerator, MobCategory.MISC).clientTrackingRange(32)
    )
    val DIRECTIONAL_BOOSTER = register(
        "directional_booster",
        EntityType.Builder.of(::DirectionalBoosterAccelerator, MobCategory.MISC).clientTrackingRange(32)
    )
    val BEACON_BEAM = register(
        "beacon_beam",
        EntityType.Builder.of(::BeaconBeam, MobCategory.MISC).clientTrackingRange(32)
    )
    val RING = register(
        "ring",
        EntityType.Builder.of(::Ring, MobCategory.MISC).clientTrackingRange(32)
    )

    fun noop() {

    }

    private fun <T: Entity> register(path: String, builder: EntityType.Builder<T>): EntityType<T> {
        val key = ResourceKey.create(Registries.ENTITY_TYPE, GlideMod.id(path))
        val type = builder.build(key)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type)
        PolymerEntityUtils.registerType(type)
        return type
    }
}