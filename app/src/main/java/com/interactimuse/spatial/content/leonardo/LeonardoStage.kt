package com.interactimuse.spatial.content.leonardo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.interactimuse.spatial.content.buildRoom
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import kotlin.math.cos
import kotlin.math.sin

// Full-bleed exhibition surfaces extend the existing rose and sandstone gallery palette.
internal val Paper = Color(0xFFF9EFE2) // design-style: fixed-figma-color existing gallery palette
internal val Ink = Color(0xFF213A36) // design-style: fixed-figma-color existing gallery palette
internal val Oxide = Color(0xFF834838) // design-style: fixed-figma-color exhibition titles
internal val Gold = Color(0xFFC29045) // design-style: fixed-figma-color gold itinerary from brief
internal val Sienna = Color(0xFFE8D6C3) // design-style: fixed-figma-color warm exhibition panels
private val GreenWash = Color(0xFFDAE2D6) // design-style: fixed-figma-color map wash

private enum class GalleryPage(val title: String) {
    Timeline("生平时间线"), Study("真迹细读"), Compare("物证比对"), Collection("作品瀑布流")
}

@Composable
internal fun LeonardoStage() {
    var page by remember { mutableStateOf(GalleryPage.Timeline) }
    var selectedEvent by remember { mutableIntStateOf(0) }
    var station by remember { mutableIntStateOf(-1) }
    var selectedWork by remember { mutableStateOf(coreWorks.first()) }
    fun openWork(work: Artwork) {
        selectedWork = work
        page = GalleryPage.Study
    }

    SpatialView(
        initial = { content, attachments ->
            content.addEntity(Entity().apply { buildRoom() })
            attachments.entity("leonardo-gallery")?.let { panel ->
                panel.placeAroundViewer(0f, 2.18f, 1.63f, 3.8f)
                content.addEntity(panel)
            }
            phases.indices.forEach { index ->
                attachments.entity("phase-$index")?.let { panel ->
                    panel.placeAroundViewer(stationAngle(index), 3.22f, 1.64f, 4.25f)
                    content.addEntity(panel)
                }
            }
        },
        update = { _, attachments ->
            val angle = if (station < 0) 0f else stationAngle(station)
            attachments.entity("leonardo-gallery")?.placeAroundViewer(angle, 2.18f, 1.63f, 3.8f)
        },
        attachments = {
            AttachmentPanel("leonardo-gallery") {
                LeonardoGallery(
                    page, { page = it }, selectedEvent, { selectedEvent = it },
                    selectedWork, { selectedWork = it }, ::openWork,
                )
            }
            phases.forEachIndexed { index, phase ->
                AttachmentPanel("phase-$index") {
                    val anchors = lifeEvents.withIndex().filter { (_, event) -> event.phase == index && event.art != null }
                    Column(
                        Modifier.width(272.dp).clip(RoundedCornerShape(12.dp))
                            .background(Paper).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        GalleryText("${phase.period} · ${phase.city}")
                        Button(onClick = {
                            selectedEvent = lifeEvents.indexOfFirst { it.phase == index }
                            station = index
                            page = GalleryPage.Timeline
                        }) { Text("进入${phase.name}") }
                        if (anchors.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                anchors.forEach { (eventIndex, event) ->
                                    val work = coreWorks.first { it.id == event.art }
                                    StationArtwork(work) {
                                        selectedEvent = eventIndex
                                        station = index
                                        openWork(work)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

private fun stationAngle(index: Int) = 45f * (index + 1)

private fun Entity.placeAroundViewer(angle: Float, radius: Float, height: Float, scale: Float) {
    val radians = angle * (Math.PI / 180).toFloat()
    components[TransformComponent::class.java]?.apply {
        setPosition(Vector3(sin(radians) * radius, height, -cos(radians) * radius))
        setEulerAngles(EulerAngles(0f, -angle, 0f))
        setScaleVector(Vector3(scale))
    }
}

@Composable
private fun LeonardoGallery(
    page: GalleryPage,
    openPage: (GalleryPage) -> Unit,
    selectedEvent: Int,
    selectEvent: (Int) -> Unit,
    selectedWork: Artwork,
    selectWork: (Artwork) -> Unit,
    openWork: (Artwork) -> Unit,
) {
    Column(
        modifier = Modifier.size(1080.dp, 660.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Paper)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeading("智触博物 · 达芬奇")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GalleryPage.entries.forEach { destination ->
                    Button(
                        onClick = { openPage(destination) },
                        enabled = page != destination,
                    ) { Text(destination.title) }
                }
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (page) {
                GalleryPage.Timeline -> TimelinePage(selectedEvent, selectEvent, openWork)
                GalleryPage.Study -> PaintingPage(selectedWork, onCompare = { openPage(GalleryPage.Compare) })
                GalleryPage.Compare -> EvidencePage(
                    if (selectedWork.evidence.isEmpty()) coreWorks.first { it.id == "mona" } else selectedWork,
                    onWorkChanged = selectWork,
                )
                GalleryPage.Collection -> CollectionPage(openWork)
            }
        }
    }
}

@Composable
internal fun SectionHeading(text: String) {
    Text(
        text,
        modifier = Modifier.semantics { heading() },
        color = Oxide,
        style = PicoTheme.typography.titleLarge,
    )
}

@Composable
internal fun GalleryText(text: String, emphasis: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = Ink,
        style = if (emphasis) PicoTheme.typography.titleMedium else PicoTheme.typography.bodyMedium,
    )
}

@Composable
private fun TimelinePage(selected: Int, onSelect: (Int) -> Unit, onOpenWork: (Artwork) -> Unit) {
    val selectedEvent = lifeEvents[selected]
    val activePhase = phases[selectedEvent.phase]
    val work = coreWorks.firstOrNull { it.id == selectedEvent.art }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(
            Modifier.width(424.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeading("人生轨迹")
            RouteMap(selectedEvent.phase) { selectedPhase ->
                onSelect(lifeEvents.indexOfFirst { it.phase == selectedPhase })
            }
            GalleryText("转身可找到七座阶段展牌。地图标示地点，路线距离未按比例绘制。")
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Sienna).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GalleryText("${activePhase.name} · ${activePhase.period} · ${activePhase.city}")
                SectionHeading("${selectedEvent.date}  ${selectedEvent.title}")
                GalleryText(selectedEvent.story)
                work?.let {
                    Button(onClick = { onOpenWork(it) }) { Text("细读《${it.name}》") }
                }
            }
        }

        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GalleryText("生平事件", emphasis = true, modifier = Modifier.weight(1.28f))
                GalleryText("阶段挂接画作", emphasis = true, modifier = Modifier.weight(1f))
            }
            var lastPhase = -1
            lifeEvents.forEachIndexed { index, event ->
                if (event.phase != lastPhase) {
                    val phase = phases[event.phase]
                    GalleryText("${event.phase + 1} · ${phase.name}  ${phase.period}", emphasis = true)
                    lastPhase = event.phase
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1.28f),
                        enabled = index != selected,
                    ) { Text("${event.date}  ${event.title}", maxLines = 1) }
                    val eventWork = coreWorks.firstOrNull { it.id == event.art }
                    Box(Modifier.weight(1f)) {
                        if (eventWork != null) {
                            Button(onClick = { onSelect(index); onOpenWork(eventWork) }) {
                                Text(eventWork.name, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteMap(active: Int, onPhase: (Int) -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(274.dp).clip(RoundedCornerShape(12.dp)).background(GreenWash),
    ) {
        val nodeSize = 42.dp
        Canvas(Modifier.fillMaxSize()) {
            // Axes are schematic; geographic labels and chronology carry the meaning.
            val coast = Path().apply {
                moveTo(size.width * .05f, size.height * .56f)
                lineTo(size.width * .28f, size.height * .53f)
                lineTo(size.width * .43f, size.height * .45f)
                lineTo(size.width * .59f, size.height * .5f)
                lineTo(size.width * .66f, size.height * .62f)
                lineTo(size.width * .73f, size.height * .68f)
                lineTo(size.width * .77f, size.height * .91f)
                lineTo(size.width * .91f, size.height)
                lineTo(size.width, size.height)
                lineTo(size.width, 0f)
                lineTo(0f, 0f)
                lineTo(0f, size.height * .56f)
                close()
            }
            drawPath(coast, Paper.copy(alpha = .68f))
            val dash = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 7.dp.toPx()))
            phases.zipWithNext().forEach { (a, b) ->
                drawLine(Gold, Offset(a.x * size.width, a.y * size.height),
                    Offset(b.x * size.width, b.y * size.height), 2.dp.toPx(), pathEffect = dash)
            }
            val point = phases[active]
            val center = Offset(point.x * size.width, point.y * size.height)
            drawCircle(Gold.copy(alpha = .36f), 29.dp.toPx(), center)
        }
        GalleryText("法国", modifier = Modifier.padding(12.dp))
        GalleryText("意大利", modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp))
        phases.forEachIndexed { index, phase ->
            Box(
                Modifier.padding(
                    start = (maxWidth * phase.x - nodeSize / 2).coerceAtLeast(0.dp),
                    top = (maxHeight * phase.y - nodeSize / 2).coerceAtLeast(0.dp),
                ),
            ) {
                Button(
                    onClick = { onPhase(index) },
                    modifier = Modifier.size(nodeSize).semantics {
                        contentDescription = "${index + 1} ${phase.city}"
                    },
                ) { Text("${index + 1}") }
            }
        }
    }
}
