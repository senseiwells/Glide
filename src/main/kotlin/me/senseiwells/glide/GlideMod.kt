package me.senseiwells.glide

import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData
import me.senseiwells.glide.GlideMod.logger
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.entity.accelerator.Booster
import me.senseiwells.glide.entity.accelerator.DirectionalBooster
import me.senseiwells.glide.entity.accelerator.DynamicThermal
import me.senseiwells.glide.entity.accelerator.StaticThermal
import me.senseiwells.glide.entity.checkpoint.BeaconBeam
import me.senseiwells.glide.entity.checkpoint.FinishLine
import me.senseiwells.glide.entity.checkpoint.RespawnCheckpoint
import me.senseiwells.glide.entity.ring.Ring
import me.senseiwells.glide.entity.ring.RingType
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.level.LevelTickEvent
import net.casual.arcade.events.player.PlayerJoinEvent
import net.casual.arcade.events.server.ServerStoppingEvent
import net.casual.arcade.minigame.Minigames
import net.casual.arcade.resources.ArcadePacks
import net.casual.arcade.resources.creator.NamedResourcePackCreator
import net.casual.arcade.resources.hosting.DirectoryPackSupplier
import net.casual.arcade.resources.hosting.PackHost
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.ResourcePackUtils.addFont
import net.casual.arcade.utils.ResourcePackUtils.addSounds
import net.casual.arcade.utils.ResourcePackUtils.sendResourcePack
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Display
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

// TODO:
//  - Behaviour of going back on checkpoints
//  - Booster / Thermal implementation
//  - UI implementation
object GlideMod: ModInitializer {
    const val MOD_ID = "glide"

    val logger: Logger = LogManager.getLogger(MOD_ID)

    val resourcePack = NamedResourcePackCreator.named("glide_pack") {
        addAssetSource(MOD_ID)
        addFont(GlideFont)
        addFont(GlideFont.Timer)
        addFont(GlideFont.Speedometer)
        addSounds(GlideSounds)
        packDescription = "GlidePack".literal()
    }

    fun id(path: String): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
    }

    override fun onInitialize() {
        GlideItems.noop()
        GlideEntities.noop()

        // TODO: For testing purposes
        val packDir = FabricLoader.getInstance().gameDir.resolve("glide_packs")
        resourcePack.buildTo(packDir)
        ArcadePacks.SPACES_FONT_PACK.buildTo(packDir)
        ArcadePacks.NO_SHADOW_PACK.buildTo(packDir)
        val host = PackHost()
        host.addPacks(DirectoryPackSupplier(packDir))
        host.start()
        GlobalEventHandler.register<ServerStoppingEvent> {
            host.shutdown()
        }

        GlobalEventHandler.register<PlayerJoinEvent> {
            it.player.sendResourcePack(host.getHostedPack("glide_pack")!!.toPackInfo())
            it.player.sendResourcePack(host.getHostedPack("spaces_font")!!.toPackInfo())
            it.player.sendResourcePack(host.getHostedPack("no_shadow")!!.toPackInfo())
        }

        Minigames.registerFactory(GlideMinigame.ID, GlideMinigame)

        // GlobalEventHandler.register<LevelTickEvent> { (level) ->
        //     for (entity in level.allEntities) {
        //         if (entity is Display) {
        //             fixEntityData(entity, level)
        //         }
        //     }
        // }
    }
}

private fun fixEntityData(display: Display, level: ServerLevel) {
    val tags = display.tags
    if ("verticalbeaconbeam" in tags) {
        val scale = display.entityData.get(DisplayTrackedData.SCALE)
        val height = scale.z
        val beam = BeaconBeam(level, height)
        beam.setPos(display.position().add(0.0, height.toDouble() / 2, 0.0))
        beam.xRot = display.xRot
        beam.yRot = display.yRot
        level.addFreshEntity(beam)
    } else if ("beaconshell" in tags) {

    } else if ("ring" in tags) {
        val type = if ("blue" in tags) {
            RingType.Blue
        } else if ("yellow" in tags) {
            RingType.Yellow
        } else if ("green" in tags) {
            RingType.Green
        } else {
            logger.error("Unknown ring type: $display")
            return
        }
        val ring = Ring(level, type)
        ring.setPos(display.position())
        ring.xRot = display.xRot
        ring.yRot = display.yRot
        level.addFreshEntity(ring)
    } else if ("omniboost" in tags || "boost" in tags || "dynamicThermal" in tags || "regularThermal" in tags) {
        val tag = CompoundTag()
        display.saveWithoutId(tag)
        val glideUtils = tag.getCompound("glideutils")
        val pos1 = glideUtils.getIntArray("pos1")
        val pos2 = glideUtils.getIntArray("pos2")
        if (pos1.size != 3 || pos2.size != 3) {
            return
        }
        val targetVelocity = glideUtils.getDouble("targetvelocity")
        val aabb = AABB(
            pos1[0].toDouble(),
            pos1[1].toDouble(),
            pos1[2].toDouble(),
            pos2[0].toDouble(),
            pos2[1].toDouble(),
            pos2[2].toDouble()
        )
        val size = Vec3(aabb.xsize, aabb.ysize, aabb.zsize)
        val scale = display.entityData.get(DisplayTrackedData.SCALE)
        val booster = if ("omniboost" in tags) {
            Booster(level, size)
        } else if ("boost" in tags) {
            DirectionalBooster(level, size)
        } else if ("dynamicThermal" in tags) {
            val brightness = Brightness.unpack(display.entityData.get(DisplayTrackedData.BRIGHTNESS))
            scale.y = (brightness.block + brightness.sky * 16).toFloat()
            DynamicThermal(level, size)
        } else {
            val brightness = Brightness.unpack(display.entityData.get(DisplayTrackedData.BRIGHTNESS))
            scale.y = (brightness.block + brightness.sky * 16).toFloat()
            val targetHeight = glideUtils.getDouble("targetheight") - display.y
            StaticThermal(level, size).also {
                it.targetHeight = targetHeight
            }
        }
        booster.setDisplaySize(scale)
        booster.setPos(display.position())
        booster.xRot = display.xRot
        booster.yRot = display.yRot
        booster.targetVelocity = targetVelocity

        booster.setDisplaySize(scale)
        level.addFreshEntity(booster)
    }  else if ("checkpoint" in tags) {
        val tag = CompoundTag()
        display.saveWithoutId(tag)
        val glideUtils = tag.getCompound("glideutils")
        val id = glideUtils.getInt("id")
        val pos1 = glideUtils.getIntArray("pos1")
        val pos2 = glideUtils.getIntArray("pos2")
        if (pos1.size != 3 || pos2.size != 3) {
            return
        }
        val aabb = AABB(
            pos1[0].toDouble(),
            pos1[1].toDouble(),
            pos1[2].toDouble(),
            pos2[0].toDouble(),
            pos2[1].toDouble(),
            pos2[2].toDouble()
        )
        val size = Vec3(aabb.xsize, aabb.ysize, aabb.zsize)
        val checkpoint = if ("finish" in tags) {
            FinishLine(level, size)
        } else {
            val resetPosition = glideUtils.getIntArray("resetposition")
            val respawnOffset = Vec3(
                resetPosition[0].toDouble(),
                resetPosition[1].toDouble(),
                resetPosition[2].toDouble()
            ).subtract(display.position())
            val respawnRotation = Vec2(
                glideUtils.getFloat("resetpitch"),
                glideUtils.getFloat("resetyaw")
            )
            val checkpoint = RespawnCheckpoint(level, size)
            checkpoint.index = id
            checkpoint.respawnOffset = respawnOffset
            checkpoint.respawnRotation = respawnRotation
            checkpoint
        }
        checkpoint.setPos(display.position())
        level.addFreshEntity(checkpoint)
    }
    display.kill()
}