package me.senseiwells.glide.entity

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils
import me.senseiwells.glide.GlideMod
import me.senseiwells.glide.entity.accelerator.Booster
import me.senseiwells.glide.entity.accelerator.DirectionalBooster
import me.senseiwells.glide.entity.accelerator.DynamicThermal
import me.senseiwells.glide.entity.accelerator.StaticThermal
import me.senseiwells.glide.entity.checkpoint.*
import me.senseiwells.glide.entity.ring.Ring
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory

object GlideEntities {
    val DYNAMIC_THERMAL = register(
        "dynamic_thermal",
        EntityType.Builder.of(::DynamicThermal, MobCategory.MISC)
    )
    val STATIC_THERMAL = register(
        "static_thermal",
        EntityType.Builder.of(::StaticThermal, MobCategory.MISC)
    )
    val BOOSTER = register(
        "booster",
        EntityType.Builder.of(::Booster, MobCategory.MISC)
    )
    val DIRECTIONAL_BOOSTER = register(
        "directional_booster",
        EntityType.Builder.of(::DirectionalBooster, MobCategory.MISC)
    )
    val BEACON_BEAM = register(
        "beacon_beam",
        EntityType.Builder.of(::BeaconBeam, MobCategory.MISC)
    )
    val RING = register(
        "ring",
        EntityType.Builder.of(::Ring, MobCategory.MISC)
    )

    fun noop() {

    }

    private fun <T: Entity> register(key: String, builder: EntityType.Builder<T>): EntityType<T> {
        val type = builder.build(key)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, GlideMod.id(key), type)
        PolymerEntityUtils.registerType(type)
        return type
    }
}