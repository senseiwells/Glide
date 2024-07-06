package me.senseiwells.glide.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.senseiwells.glide.GlideMod.id
import me.senseiwells.glide.addon.GlideAddonConfig
import me.senseiwells.glide.addon.serialization.*
import me.senseiwells.glide.checkpoint.*
import me.senseiwells.glide.entity.ring.RingType
import net.casual.arcade.utils.JsonUtils
import net.casual.arcade.utils.JsonUtils.array
import net.casual.arcade.utils.JsonUtils.obj
import net.casual.arcade.utils.JsonUtils.objects
import net.casual.arcade.utils.JsonUtils.string
import net.casual.arcade.utils.JsonUtils.stringOrNull
import net.casual.arcade.utils.location.template.ExactLocationTemplate
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Axis
import net.minecraft.core.Vec3i
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.mutable.MutableInt
import java.nio.file.Path
import java.util.*
import kotlin.collections.ArrayList
import kotlin.io.path.reader
import kotlin.io.path.writer

interface GlideGRFVisitor {
    fun visitSpawn(spawn: ExactLocationTemplate)

    fun visitCheckpoint(checkpoint: Checkpoint)

    fun visitBooster(booster: BoosterTemplate)

    fun visitThermal(thermal: ThermalTemplate)

    fun visitRing(ring: RingTemplate)
}

class GlideGRFConverter(
    val id: ResourceLocation,
    val name: Component = Component.empty(),
    val description: Component = Component.empty(),
    val authors: List<String> = listOf(),
    val version: String = "1.0.0"
): GlideGRFVisitor {
    private val boosters = ArrayList<BoosterTemplate>()
    private val thermals = ArrayList<ThermalTemplate>()
    private val checkpoints = ArrayList<Checkpoint>()
    private val rings = ArrayList<RingTemplate>()

    private var spawn = ExactLocationTemplate()

    fun convert(): GlideAddonConfig {
        return GlideAddonConfig(
            this.id,
            this.name,
            this.description,
            this.authors,
            this.version,
            this.spawn,
            this.boosters,
            this.thermals,
            this.checkpoints,
            this.rings
        )
    }

    override fun visitSpawn(spawn: ExactLocationTemplate) {
        this.spawn = spawn
    }

    override fun visitCheckpoint(checkpoint: Checkpoint) {
        this.checkpoints.add(checkpoint)
    }

    override fun visitBooster(booster: BoosterTemplate) {
        this.boosters.add(booster)
    }

    override fun visitThermal(thermal: ThermalTemplate) {
        this.thermals.add(thermal)
    }

    override fun visitRing(ring: RingTemplate) {
        this.rings.add(ring)
    }

    companion object {
        @Suppress("DEPRECATION")
        fun convertSingle(id: ResourceLocation, input: Path, output: Path) {
            val converter = GlideGRFConverter(id)
            GlideGRFReader.read(input, converter)
            val addon = converter.convert()
            output.writer().use {
                JsonUtils.encodeWith(GlideAddonConfig.CODEC, addon, it)
            }
        }
    }
}

@Deprecated("Temporary class for migrating old game-rule files")
object GlideGRFReader {
    fun read(path: Path, visitor: GlideGRFVisitor) {
        val json = path.reader().use(JsonUtils::decodeToJsonObject)
        this.read(json, visitor)
    }

    fun read(json: JsonObject, visitor: GlideGRFVisitor) {
        for (rule in json.array("children").objects()) {
            if (rule.string("name") == "LevelRules") {
                val lastCheckpoint = MutableInt()
                for (subrule in rule.array("children").objects()) {
                    this.parseRule(subrule, lastCheckpoint, visitor)
                }
                break
            }
        }
    }

    private fun parseRule(rule: JsonObject, lastCheckpoint: MutableInt, visitor: GlideGRFVisitor) {
        val parameters = rule.obj("parameters")
        val children = rule.array("children")
        when (rule.string("name")) {
            "Checkpoint" -> this.parseCheckpoint(parameters, children, lastCheckpoint, visitor)
            "ThermalArea" -> this.parseThermal(parameters, visitor)
            "TargetArea" -> this.parseFinishLine(parameters, lastCheckpoint, visitor)
            "ScoreRing" -> this.parseRing(parameters, visitor)
        }
    }

    private fun parseRing(parameters: JsonObject, visitor: GlideGRFVisitor) {
        val x = parameters.string("x").toInt()
        val y = parameters.string("y").toInt()
        val z = parameters.string("z").toInt()
        val axis = Axis.byName(parameters.string("orientation"))
            ?: throw IllegalArgumentException("Unknown orientation")
        val ringType = when (parameters.string("size")) {
            "small" -> RingType.Blue
            "medium" -> RingType.Yellow
            "large" -> RingType.Green
            else -> throw IllegalArgumentException("Unknown ring size")
        }
        val pos = Vec3.atCenterOf(Vec3i(x, y, z))
        visitor.visitRing(RingTemplate(pos, axis, ringType))
    }

    private fun parseFinishLine(parameters: JsonObject, lastCheckpoint: MutableInt, visitor: GlideGRFVisitor) {
        val box = this.readAABB(parameters)
        visitor.visitCheckpoint(FinishLine(lastCheckpoint.value + 1, box))
    }

    private fun parseThermal(parameters: JsonObject, visitor: GlideGRFVisitor) {
        val box = this.readAABB(parameters)

        if (!parameters.has("speedBoost")) {
            val staticLift = parameters.stringOrNull("staticLift")?.toDouble() ?: 0.0
            val liftForceModifier = parameters.stringOrNull("liftForceModifier")?.toDouble() ?: 1.0
            val targetY = Optional.ofNullable(parameters.stringOrNull("targetHeight")?.toDouble())
            visitor.visitThermal(ThermalTemplate(box, staticLift, liftForceModifier, targetY))
            return
        }

        val speedBoost = parameters.string("speedBoost").toDouble()
        val boostDirection = parameters.string("boostDirection")
        val isDirectional = !boostDirection.startsWith("omni_")
        val direction = when (boostDirection.removePrefix("omni_")) {
            "plus_x" -> Direction.EAST
            "minus_x" -> Direction.WEST
            "plus_z" -> Direction.SOUTH
            "minus_z" -> Direction.NORTH
            else -> throw IllegalStateException(boostDirection)
        }
        visitor.visitBooster(BoosterTemplate(box, speedBoost, direction, isDirectional))
    }

    private fun parseCheckpoint(
        parameters: JsonObject,
        children: JsonArray,
        lastCheckpoint: MutableInt,
        visitor: GlideGRFVisitor
    ) {
        val id = parameters.string("id").toInt()
        val box = this.readAABB(parameters)
        if (id == 0) {
            val spawn = this.readRespawnPoint(children.get(0).asJsonObject.obj("parameters"))
            visitor.visitSpawn(spawn)
            visitor.visitCheckpoint(ProgressCheckpoint(id, box))
            return
        }

        if (id > lastCheckpoint.value) {
            lastCheckpoint.setValue(id)
        }
        if (children.isEmpty) {
            visitor.visitCheckpoint(ProgressCheckpoint(id, box))
            return
        }

        var spawn: ExactLocationTemplate? = null
        val beacons = ArrayList<BeaconBeamTemplate>()
        for (child in children.objects()) {
            val childParams = child.obj("parameters")
            when (child.string("name")) {
                "CustomBeacon" -> beacons.add(this.readBeaconBeam(childParams))
                "UpdatePlayer" -> spawn = this.readRespawnPoint(childParams)
                else -> throw IllegalArgumentException("Unknown child parameter")
            }
        }

        if (spawn == null) {
            throw IllegalStateException("Respawn checkpoint didn't specify a respawn point!")
        }
        visitor.visitCheckpoint(RespawnCheckpoint(id, box, spawn, beacons))
    }

    private fun readBeaconBeam(json: JsonObject): BeaconBeamTemplate {
        val x = json.string("spawnX").toInt()
        val y = json.string("spawnY").toInt()
        val z = json.string("spawnZ").toInt()
        val length = json.string("beam_length").toFloat()
        val rotation = when (json.string("beam_direction")) {
            "plus_x" -> Vec2(0.0F, -90.0F)
            "minus_x" -> Vec2(0.0F, 90.0F)
            "plus_z" -> Vec2(0.0F, 0.0F)
            "minus_z" -> Vec2(0.0F, 180.0F)
            "plus_y" -> Vec2(-90.0F, 0.0F)
            "minus_y" -> Vec2(90.0F, 0.0F)
            else -> throw IllegalArgumentException("Unknown beam direction")
        }
        val spawnPos = Vec3.atCenterOf(Vec3i(x, y, z))
        return BeaconBeamTemplate(length, ExactLocationTemplate(spawnPos, rotation))
    }

    private fun readRespawnPoint(json: JsonObject): ExactLocationTemplate {
        val x = json.string("spawnX").toInt()
        val y = json.string("spawnY").toInt()
        val z = json.string("spawnZ").toInt()
        val yRot = json.string("yRot").toFloat()
        val xRot = json.string("xRot").toFloat()
        return ExactLocationTemplate(Vec3.atCenterOf(Vec3i(x, y, z)), Vec2(xRot, yRot))
    }

    private fun readAABB(json: JsonObject): AABB {
        val x0 = json.string("x0").toDouble()
        val y0 = json.string("y0").toDouble()
        val z0 = json.string("z0").toDouble()
        val x1 = json.string("x1").toDouble()
        val y1 = json.string("y1").toDouble()
        val z1 = json.string("z1").toDouble()
        return AABB(x0, y0, z0, x1, y1, z1)
    }
}