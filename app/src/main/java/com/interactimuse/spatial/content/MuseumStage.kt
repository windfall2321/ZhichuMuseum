package com.interactimuse.spatial.content

import android.util.Log
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pico.spatial.core.ecs.AccessibilityComponent
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.DirectionalLightComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.PointLightComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.content.setAccessibilityActions
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialTapGesture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private const val RoomWidth = 7.4f
private const val RoomHeight = 3.5f
private const val DisplayDepth = 2.83f
private const val PlinthHeight = 0.92f
private val ExhibitInk = Color(0xFF214741) // design-style: fixed-figma-color reference watercolor signage
private val PlaquePaper = Color(0xE6E9F2E7) // design-style: fixed-figma-color reference tinted plaque

@Composable
fun MuseumStage() {
    val context = LocalContext.current
    val displays = remember { mutableStateMapOf<Exhibit, ExhibitDisplay>() }
    var selected by remember { mutableStateOf<Exhibit?>(null) }
    fun toggle(exhibit: Exhibit) {
        selected = if (selected == exhibit) null else exhibit
    }

    LaunchedEffect(selected, displays.size) {
        val active = selected
        coroutineScope {
            for ((exhibit, display) in displays) {
                launch { display.reveal(exhibit == active) }
            }
        }
    }

    SpatialView(
        modifier = Modifier.pointerInput(context) {
            detectSpatialTapGesture(context, TargetEntity.any { target ->
                displays.values.any { it.target == target }
            }) { hit ->
                displays.entries.firstOrNull { it.value.target == hit.targetEntity }
                    ?.let { toggle(it.key) }
            }
        },
        initial = { content, attachments ->
            displays.clear()
            val gallery = Entity()
            content.addEntity(gallery)
            gallery.buildRoom()

            attachments.entity("museum-title")?.apply {
                place(0f, 2.43f, -3.43f)
                transform.setScaleVector(Vector3(8f, 8f, 8f))
                gallery.addChild(this)
            }

            exhibits.forEachIndexed { index, exhibit ->
                // Each bay faces the center, in clockwise order from the entry view.
                val bay = Entity()
                gallery.addChild(bay)
                bay.place(0f, 0f, 0f, yaw = -90f * index)
                attachments.entity(exhibit.model)?.apply {
                    place(0.79f, 1.56f, -DisplayDepth + 0.18f)
                    transform.setScaleVector(Vector3(4.4f, 4.4f, 4.4f))
                    bay.addChild(this)
                }
                bay.addExhibit(exhibit)?.let { display ->
                    displays[exhibit] = display
                    val id = exhibit.model.substringAfterLast('/').substringBeforeLast('.')
                    content.setAccessibilityActions(display.target, semanticId = "exhibit.$id") {
                        onClick(label = "切换藏品点亮效果") {
                            toggle(exhibit)
                            true
                        }
                    }
                }
            }
        },
        attachments = {
            AttachmentPanel("museum-title") {
                Text(
                    text = "智触博物",
                    modifier = Modifier.semantics { heading() },
                    style = PicoTheme.typography.titleLarge,
                    color = ExhibitInk,
                )
            }
            exhibits.forEach { exhibit ->
                AttachmentPanel(exhibit.model) {
                    ExhibitPlaque(exhibit, selected == exhibit, displays.containsKey(exhibit)) {
                        toggle(exhibit)
                    }
                }
            }
        },
    )
}

@Composable
private fun ExhibitPlaque(
    exhibit: Exhibit,
    selected: Boolean,
    available: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier.width(300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PlaquePaper)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = exhibit.name,
            modifier = Modifier.semantics { heading() },
            style = PicoTheme.typography.titleMedium,
            color = ExhibitInk,
        )
        Text(
            text = exhibit.metadata,
            style = PicoTheme.typography.bodySmall,
            color = ExhibitInk,
        )
        Text(
            text = exhibit.description,
            style = PicoTheme.typography.bodyMedium,
            color = ExhibitInk,
        )
        Text(
            text = exhibit.detail,
            style = PicoTheme.typography.bodySmall,
            color = ExhibitInk,
        )
        Button(onClick = onToggle, enabled = available) {
            Text(if (selected) "熄灭藏品" else "点亮藏品")
        }
    }
}

private fun Entity.buildRoom() {
    val rose = texturedMaterial("textures/rose-watercolor.png")
    val limestone = texturedMaterial("textures/sand-limestone.png")
    val trim = material(0.68f, 0.47f, 0.30f)
    val brass = material(0.82f, 0.62f, 0.40f)
    val light = material(1f, 0.89f, 0.67f)

    box(limestone, RoomWidth, 0.15f, RoomWidth, 0f, -0.075f, 0f)
    box(limestone, RoomWidth, 0.15f, RoomWidth, 0f, RoomHeight + 0.075f, 0f)

    repeat(4) { side ->
        val wall = Entity()
        addChild(wall)
        wall.place(0f, 0f, 0f, yaw = -90f * side)
        val edge = -RoomWidth / 2f
        wall.box(rose, RoomWidth, RoomHeight, 0.12f, 0f, RoomHeight / 2f, edge)
        wall.box(trim, RoomWidth, 0.07f, 0.055f, 0f, 0.035f, edge + 0.07f)

        // A slender brass frame marks the exhibit bay on the continuous wash.
        wall.box(brass, 3.82f, 0.038f, 0.085f, 0f, 2.84f, edge + 0.09f)
        for (x in listOf(-1.89f, 1.89f)) {
            wall.box(brass, 0.034f, 2.78f, 0.085f, x, 1.46f, edge + 0.09f)
        }
        wall.box(trim, RoomWidth, 0.075f, 0.19f, 0f, RoomHeight - 0.038f, edge + 0.16f)
        wall.box(light, RoomWidth - 0.24f, 0.015f, 0.035f, 0f, RoomHeight - 0.082f, edge + 0.19f)
    }

    addChild(Entity().apply {
        place(0f, 0f, 0f, yaw = 30f, pitch = -42f)
        components.set(DirectionalLightComponent(Color4(1f, 0.91f, 0.78f, 1f), 2100f, false))
    })
}

private suspend fun Entity.addExhibit(exhibit: Exhibit): ExhibitDisplay? {
    val focalX = -0.52f
    box(material(0.74f, 0.62f, 0.48f), 1.18f, 0.79f, 0.96f, focalX, 0.395f, -DisplayDepth)
    box(material(0.86f, 0.72f, 0.52f), 1.22f, 0.03f, 1.0f, focalX, 0.795f, -DisplayDepth)
    box(
        material(0.91f, 0.79f, 0.62f), 1.22f, 0.11f, 1.0f,
        focalX, PlinthHeight - 0.055f, -DisplayDepth,
    )

    addChild(Entity().apply {
        place(focalX + 0.28f, 2.65f, -DisplayDepth + 0.8f)
        components.set(PointLightComponent(Color4(1f, 0.94f, 0.83f, 1f), 3100f, 4f))
    })

    val mount = Entity()
    addChild(mount)
    try {
        val model = Entity.loadSuspend("asset://${exhibit.model}")
        mount.addChild(model)
        val initialBounds = model.getVisualBounds(mount)
        val size = initialBounds.size
        check(size.x.isFinite() && size.y.isFinite() && size.z.isFinite())
        check(size.x > 0f && size.y > 0f && size.z > 0f)
        val scale = minOf(0.94f / size.x, 0.86f / size.y, 0.76f / size.z)
        mount.transform.setScaleVector(Vector3(scale, scale, scale))
        mount.place(
            focalX - initialBounds.center.x * scale,
            PlinthHeight - initialBounds.min.y * scale,
            -DisplayDepth - initialBounds.center.z * scale,
        )
        val displayBounds = model.getVisualBounds(this)
        // The hit box shares the displayed bounds even when the imported model has an off-center pivot.
        val target = Entity().apply {
            val center = displayBounds.center
            place(center.x, center.y, center.z)
            components.set(InteractableComponent())
            components.set(CollisionComponent(
                collisionShape = listOf(ShapeResource.createBox(displayBounds.size)),
                physicsMaterial = PhysicsMaterialResource(),
            ))
            components.set(AccessibilityComponent(label = exhibit.name, value = "未点亮"))
        }
        addChild(target)

        val color = when (exhibit.category) {
            "青铜器" -> Color4(0.12f, 0.9f, 0.56f, 1f)
            "陶瓷" -> Color4(0.43f, 0.82f, 0.41f, 1f)
            "玉器" -> Color4(0.46f, 0.92f, 0.72f, 1f)
            else -> Color4(1f, 0.70f, 0.30f, 1f)
        }
        val sparks = revealSparks(color).apply {
            val center = displayBounds.center
            place(center.x, center.y, center.z)
            enabled = false
        }
        addChild(sparks)
        val light = PointLightComponent(if (exhibit.category == "绘画") Color4.WHITE else color, 0f, 2.2f)
        addChild(Entity().apply {
            place(focalX + 0.10f, 1.58f, -DisplayDepth + 0.68f)
            components.set(light)
        })

        Log.i("MuseumStage", "Loaded ${exhibit.model}; bounds=${displayBounds.min} to ${displayBounds.max}")
        return ExhibitDisplay(exhibit, target, model, sparks, light, color)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Log.e("MuseumStage", "Unable to display ${exhibit.model}", failure)
        return null
    }
}

private class ExhibitDisplay(
    private val exhibit: Exhibit,
    val target: Entity,
    model: Entity,
    private val sparks: Entity,
    private val light: PointLightComponent,
    private val color: Color4,
) {
    private val surfaces = model.glowSurfaces()
    private var amount = 0f

    suspend fun reveal(selected: Boolean) {
        val end = if (selected) 1f else 0f
        if (end == amount) return
        target.components[AccessibilityComponent::class.java]?.value = if (selected) "已点亮" else "未点亮"
        sparks.enabled = true
        animate(amount, end, animationSpec = tween(if (selected) 540 else 230)) { value, _ ->
            amount = value
            light.intensity = 9200f * value
            sparks.transform.setScaleVector(Vector3(0.72f + 0.28f * value))
            sparks.transform.setEulerAngles(EulerAngles(0f, -16f * (1f - value), 0f))
            val strength = when (exhibit.category) {
                "青铜器" -> 1.05f
                "绘画" -> 0.22f
                else -> 0.45f
            }
            for ((surface, base) in surfaces) {
                val emission = strength * value
                surface.setEmissiveColor(Color4(
                    base.red + color.red * emission,
                    base.green + color.green * emission,
                    base.blue + color.blue * emission,
                    base.alpha,
                ))
            }
        }
        sparks.enabled = selected
        Log.i("MuseumStage", "${exhibit.model} illuminated=$selected")
    }
}

private fun Entity.glowSurfaces(): Map<PhysicallyBasedMaterial, Color4> {
    val surfaces = mutableMapOf<PhysicallyBasedMaterial, Color4>()
    fun visit(entity: Entity) {
        entity.components[ModelComponent::class.java]?.materials?.forEach { material ->
            if (material is PhysicallyBasedMaterial && material !in surfaces) {
                surfaces[material] = material.getEmissiveColor()
            }
        }
        entity.getChildren().forEach(::visit)
    }
    visit(this)
    for ((surface, base) in surfaces) {
        if (base.red == 0f && base.green == 0f && base.blue == 0f) {
            val texture = surface.getBaseColorTextureParameter()
            if (texture.texture.valid) {
                surface.setEmissiveTextureParameter(texture)
            }
        }
    }
    return surfaces
}

private fun revealSparks(accent: Color4): Entity = Entity().apply {
    val gold = material(1f, 0.89f, 0.55f).apply { setApplyToneMapping(false) }
    val jewel = UnlitMaterial.create().apply {
        setApplyToneMapping(false)
        setBaseColor(accent)
    }
    val rayMesh = MeshResource.createBox(Vector3(0.009f, 0.075f, 0.008f))
    val sparkMesh = MeshResource.createBox(Vector3(0.025f, 0.025f, 0.025f))
    for (index in 0 until 28) {
        // Two rising strands wrap around the display, leaving the front readable.
        val step = (index % 14) / 13f
        val angle = step * 4.1f + (index / 14) * 3.141593f
        val radius = 0.58f - 0.04f * step
        val x = cos(angle) * radius
        val y = -0.47f + step * 1.08f
        val z = sin(angle) * 0.39f
        if (index % 7 == 2) {
            for (roll in listOf(0f, 90f)) {
                addChild(ModelEntity(mesh = rayMesh, material = gold).apply {
                    place(x, y, z)
                    transform.setEulerAngles(EulerAngles(0f, 0f, roll))
                })
            }
        } else {
            addChild(ModelEntity(mesh = sparkMesh, material = if (index % 3 == 0) gold else jewel).apply {
                place(x, y, z)
                transform.setEulerAngles(EulerAngles(30f, index * 19f, 45f))
                transform.setScaleVector(Vector3(if (index % 5 == 0) 1.1f else 0.56f))
            })
        }
    }
}

private fun texturedMaterial(path: String) = UnlitMaterial.create().apply {
    setApplyToneMapping(false)
    setBaseColorTexture(TextureResource.load(path))
}

private fun material(red: Float, green: Float, blue: Float) =
    UnlitMaterial.create().apply { setBaseColor(Color4(red, green, blue, 1f)) }

private fun Entity.box(
    material: UnlitMaterial,
    width: Float,
    height: Float,
    depth: Float,
    x: Float,
    y: Float,
    z: Float,
) {
    val box = ModelEntity(mesh = MeshResource.createBox(Vector3(width, height, depth)), material = material)
    box.place(x, y, z)
    addChild(box)
}

private val Entity.transform: TransformComponent
    get() = checkNotNull(components[TransformComponent::class.java])

private fun Entity.place(x: Float, y: Float, z: Float, yaw: Float = 0f, pitch: Float = 0f) {
    transform.setPosition(Vector3(x, y, z))
    transform.setEulerAngles(EulerAngles(pitch, yaw, 0f))
}
