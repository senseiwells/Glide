package me.senseiwells.glide

import com.google.gson.JsonObject
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData
import me.senseiwells.glide.GlideMod.logger
import me.senseiwells.glide.entity.GlideEntities
import me.senseiwells.glide.entity.accelerator.Booster
import me.senseiwells.glide.entity.accelerator.DirectionalBooster
import me.senseiwells.glide.entity.accelerator.DynamicThermal
import me.senseiwells.glide.entity.accelerator.StaticThermal
import me.senseiwells.glide.entity.checkpoint.BeaconBeam
import me.senseiwells.glide.entity.checkpoint.FinishLine
import me.senseiwells.glide.entity.checkpoint.ProgressCheckpoint
import me.senseiwells.glide.entity.checkpoint.RespawnCheckpoint
import me.senseiwells.glide.entity.ring.Ring
import me.senseiwells.glide.entity.ring.RingType
import me.senseiwells.glide.extension.GlideExtensions
import me.senseiwells.glide.font.GlideFont
import me.senseiwells.glide.item.GlideItems
import me.senseiwells.glide.minigame.GlideMinigame
import me.senseiwells.glide.sound.GlideSounds
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.level.LevelTickEvent
import net.casual.arcade.events.player.PlayerJoinEvent
import net.casual.arcade.events.server.ServerLoadedEvent
import net.casual.arcade.events.server.ServerStoppingEvent
import net.casual.arcade.minigame.Minigames
import net.casual.arcade.resources.ArcadePacks
import net.casual.arcade.resources.creator.NamedResourcePackCreator
import net.casual.arcade.resources.hosting.DirectoryPackSupplier
import net.casual.arcade.resources.hosting.PackHost
import net.casual.arcade.utils.ComponentUtils.literal
import net.casual.arcade.utils.JsonUtils
import net.casual.arcade.utils.JsonUtils.array
import net.casual.arcade.utils.JsonUtils.obj
import net.casual.arcade.utils.JsonUtils.objects
import net.casual.arcade.utils.JsonUtils.string
import net.casual.arcade.utils.JsonUtils.stringOrNull
import net.casual.arcade.utils.ResourcePackUtils.addFont
import net.casual.arcade.utils.ResourcePackUtils.addSounds
import net.casual.arcade.utils.ResourcePackUtils.sendResourcePack
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.Vec3i
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Brightness
import net.minecraft.world.entity.Display
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Path
import kotlin.io.path.reader

// TODO:
//  - Behaviour of going back on checkpoints
//  - Booster / Thermal implementation
//  - UI implementation
object GlideMod: ModInitializer {
    const val MOD_ID = "glide"

    const val debug: Boolean = true

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

        GlideExtensions.register()

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

        // GlobalEventHandler.register<ServerLoadedEvent> {
        //     readEntityData(it.server)
        // }
        // registerEntityDataFixer()
    }
}

// TODO:
//  THIS IS ALL TEMPORARY FOR PORTING THE OLD MAPS
//  ONCE WE'VE PORTED THEM PLEASE YEET THIS!!!!!!!

private fun readEntityData(server: MinecraftServer) {
    val json = Path.of("/Users/sensei/Documents/Coding/Kotlin/Glide/src/main/resources/rules/temple.json").reader().use {
        JsonUtils.GSON.fromJson(it, JsonObject::class.java)
    }
    for (rule in json.array("children").objects()) {
        if (rule.string("name") == "LevelRules") {
            for (subrule in rule.array("children").objects()) {
                parseRule(subrule, server)
            }
            break
        }
    }
}

var lastIndex = 0

private fun parseRule(rule: JsonObject, server: MinecraftServer) {
    val level = server.overworld()
    val parameters = rule.obj("parameters")
    val children = rule.array("children")
    when (rule.string("name")) {
        "Checkpoint" -> {
            val id = parameters.string("id").toInt()
            if (id == 0) {
                return
            }
            lastIndex = id

            val x0 = parameters.string("x0").toDouble()
            val y0 = parameters.string("y0").toDouble()
            val z0 = parameters.string("z0").toDouble()
            val x1 = parameters.string("x1").toDouble()
            val y1 = parameters.string("y1").toDouble()
            val z1 = parameters.string("z1").toDouble()
            val box = AABB(x0, y0, z0, x1, y1, z1)
            val size = Vec3(box.xsize, box.ysize, box.zsize)

            if (!children.isEmpty) {
                val respawnCheckpoint = RespawnCheckpoint(level, size)
                respawnCheckpoint.index = id
                respawnCheckpoint.setPos(box.center)
                for (child in children.objects()) {
                    val childParams = child.obj("parameters")
                    when (child.string("name")) {
                        "CustomBeacon" -> {
                            val x = childParams.string("spawnX").toInt()
                            val y = childParams.string("spawnY").toInt()
                            val z = childParams.string("spawnZ").toInt()
                            val spawnPos = Vec3.atCenterOf(Vec3i(x, y, z))
                            val length = childParams.string("beam_length").toFloat()
                            val beam = BeaconBeam(level, length)
                            beam.setPos(spawnPos)
                            beam.xRot = -90F
                            level.addFreshEntity(beam)
                        }
                        "UpdatePlayer" -> {
                            val x = childParams.string("spawnX").toInt()
                            val y = childParams.string("spawnY").toInt()
                            val z = childParams.string("spawnZ").toInt()
                            val yRot = childParams.string("yRot").toFloat()
                            val xRot = childParams.string("xRot").toFloat()
                            respawnCheckpoint.respawnOffset = Vec3.atCenterOf(Vec3i(x, y, z)).subtract(box.center)
                            respawnCheckpoint.respawnRotation = Vec2(xRot, yRot)
                        }
                    }
                }
                level.addFreshEntity(respawnCheckpoint)
                return
            }
            val checkpoint = ProgressCheckpoint(level, size)
            checkpoint.index = id
            checkpoint.setPos(box.center)
            level.addFreshEntity(checkpoint)
        }
        "ThermalArea" -> {
            val x0 = parameters.string("x0").toDouble()
            val y0 = parameters.string("y0").toDouble()
            val z0 = parameters.string("z0").toDouble()
            val x1 = parameters.string("x1").toDouble()
            val y1 = parameters.string("y1").toDouble()
            val z1 = parameters.string("z1").toDouble()
            val box = AABB(x0, y0, z0, x1, y1, z1)
            val size = Vec3(box.xsize, box.ysize, box.zsize)

            val staticLift = parameters.stringOrNull("staticLift")?.toDouble() ?: 0.0
            val liftForceModifier = parameters.stringOrNull("liftForceModifier")?.toDouble() ?: 1.0
            if (parameters.has("targetHeight")) {
                val targetHeight = parameters.string("targetHeight").toDouble()
                val thermal = StaticThermal(level, size)
                thermal.targetVelocity = staticLift
                thermal.liftForceModifier = liftForceModifier
                thermal.setPos(box.center.subtract(0.0, box.ysize / 2, 0.0))
                thermal.targetHeight = targetHeight - thermal.y
                level.addFreshEntity(thermal)
                return
            }
            if (!parameters.has("speedBoost")) {
                val thermal = DynamicThermal(level, size)
                thermal.targetVelocity = staticLift
                thermal.liftForceModifier = liftForceModifier
                if (staticLift < 0) {
                    thermal.isInvisible = true
                }
                thermal.setPos(box.center.subtract(0.0, box.ysize / 2, 0.0))
                level.addFreshEntity(thermal)
                return
            }

            val speedBoost = parameters.string("speedBoost").toDouble()
            val boostDirection = parameters.string("boostDirection")
            val boost = if (boostDirection.startsWith("omni")) {
                Booster(level, size)
            } else {
                DirectionalBooster(level, size)
            }
            boost.setPos(box.center)
            boost.targetVelocity = speedBoost
            val yaw = when (boostDirection.removePrefix("omni_")) {
                "plus_x" -> -90F
                "minus_x" -> 90F
                "plus_z" -> 0F
                "minus_z" -> 180F
                else -> throw IllegalStateException(boostDirection)
            }
            boost.yRot = yaw
            level.addFreshEntity(boost)
        }
        "TargetArea" -> {
            val x0 = parameters.string("x0").toDouble()
            val y0 = parameters.string("y0").toDouble()
            val z0 = parameters.string("z0").toDouble()
            val x1 = parameters.string("x1").toDouble()
            val y1 = parameters.string("y1").toDouble()
            val z1 = parameters.string("z1").toDouble()
            val box = AABB(x0, y0, z0, x1, y1, z1)
            val size = Vec3(box.xsize, box.ysize, box.zsize)

            val finish = FinishLine(level, size)
            finish.index = lastIndex + 1
            finish.setPos(box.center)
            level.addFreshEntity(finish)
        }
        // TODO: Rings
    }
}

private fun registerEntityDataFixer() {
    GlobalEventHandler.register<LevelTickEvent> { (level) ->
        for (entity in level.allEntities) {
            if (entity is Display) {
                fixEntityData(entity, level)
            }
        }
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