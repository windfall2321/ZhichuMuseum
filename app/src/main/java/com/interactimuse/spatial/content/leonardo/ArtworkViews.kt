package com.interactimuse.spatial.content.leonardo

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.gesture.detectSpatialTransformGesture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class ArtImage(
    val bitmap: ImageBitmap? = null,
    val loading: Boolean = true,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
)

@Composable
private fun artImage(id: String, maxEdge: Int): ArtImage {
    val assets = LocalContext.current.assets
    return produceState(ArtImage(), id, maxEdge) {
        value = ArtImage()
        value = withContext(Dispatchers.IO) {
            try {
                val path = "leonardo/$id.jpg"
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
                val size = maxOf(bounds.outWidth, bounds.outHeight)
                var sample = 1
                while (size / sample > maxEdge) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
                ArtImage(bitmap?.asImageBitmap(), false, bounds.outWidth, bounds.outHeight)
            } catch (error: java.io.IOException) {
                Log.w("LeonardoGallery", "Image unavailable: $id", error)
                ArtImage(loading = false)
            }
        }
    }.value
}

private data class ImagePose(val scale: Float = 1f, val x: Float = 0f, val y: Float = 0f, val rotation: Float = 0f) {
    fun change(zoom: Float = 1f, dx: Float = 0f, dy: Float = 0f, turn: Float = 0f) =
        ImagePose(
            (scale * zoom).coerceIn(.35f, 10f),
            (x + dx).coerceIn(-2f, 2f),
            (y + dy).coerceIn(-2f, 2f),
            (rotation + turn) % 360,
        )

    companion object {
        fun focus(hotspot: Hotspot, image: ImageBitmap?, viewport: IntSize): ImagePose {
            if (image == null || viewport.width == 0 || viewport.height == 0) return ImagePose(3.5f)
            val point = fittedPoint(image, viewport, hotspot.x to hotspot.y)
            return ImagePose(3.5f, -point.x * 3.5f / viewport.width, -point.y * 3.5f / viewport.height)
        }
    }
}

private fun fittedPoint(image: ImageBitmap, viewport: IntSize, point: Pair<Float, Float>): Offset {
    val factor = minOf(viewport.width.toFloat() / image.width, viewport.height.toFloat() / image.height)
    return Offset((point.first - .5f) * image.width * factor, (point.second - .5f) * image.height * factor)
}

private fun alignEyeLine(
    painting: ImageBitmap,
    reference: ImageBitmap,
    viewport: IntSize,
    eyes: List<Pair<Float, Float>>,
    referenceEyes: List<Pair<Float, Float>>,
): ImagePose {
    val first = fittedPoint(painting, viewport, eyes[0])
    val sketchFirst = fittedPoint(reference, viewport, referenceEyes[0])
    val baseline = fittedPoint(painting, viewport, eyes[1]) - first
    val sketchBaseline = fittedPoint(reference, viewport, referenceEyes[1]) - sketchFirst
    val zoom = (baseline.getDistance() / sketchBaseline.getDistance()).coerceIn(.35f, 10f)
    val angle = atan2(baseline.y, baseline.x) - atan2(sketchBaseline.y, sketchBaseline.x)
    val rotated = Offset(
        sketchFirst.x * cos(angle) - sketchFirst.y * sin(angle),
        sketchFirst.x * sin(angle) + sketchFirst.y * cos(angle),
    )
    val move = first - rotated * zoom
    return ImagePose(zoom, move.x / viewport.width, move.y / viewport.height, angle * (180 / Math.PI).toFloat())
}

@Composable
internal fun PaintingPage(work: Artwork, onCompare: () -> Unit) {
    var pose by remember(work.id) { mutableStateOf(ImagePose()) }
    var hotspot by remember(work.id) { mutableStateOf<Hotspot?>(null) }
    val image = artImage(work.id, 3600)
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ArtViewport(
                modifier = Modifier.fillMaxWidth().weight(1f).onSizeChanged { viewport = it },
                primary = image,
                primaryName = work.name,
                primaryPose = pose,
                detailId = work.id,
                markers = work.hotspots,
                currentMarker = hotspot,
                onTransform = { zoom, x, y, turn -> pose = pose.change(zoom, x, y, turn) },
            )
            PoseControls(pose, { pose = it })
        }
        Column(
            Modifier.width(312.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeading(work.name)
            GalleryText(work.catalog)
            GalleryText("双手缩放或旋转，单手平移。也可使用画面下方的按钮。")
            if (work.hotspots.isNotEmpty()) {
                GalleryText("观察热点", emphasis = true)
                work.hotspots.forEach { point ->
                    Button(onClick = {
                        hotspot = point
                        pose = ImagePose.focus(point, image.bitmap, viewport)
                    }) { Text(point.name) }
                }
                hotspot?.let { GalleryText(it.note) }
            }
            if (work.evidence.isNotEmpty()) {
                Button(onClick = onCompare) { Text("打开对应物证") }
            }
        }
    }
}

@Composable
internal fun EvidencePage(work: Artwork, onWorkChanged: (Artwork) -> Unit) {
    var evidence by remember(work.id) { mutableStateOf(work.evidence.first()) }
    var isOverlay by remember { mutableStateOf(false) }
    var showGuides by remember { mutableStateOf(false) }
    var opacity by remember { mutableFloatStateOf(.5f) }
    var overlayViewport by remember { mutableStateOf(IntSize.Zero) }
    var pose by remember(work.id, evidence.id) { mutableStateOf(ImagePose()) }
    val painting = artImage(work.id, 3600)
    val reference = artImage(evidence.id, 3600)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            coreWorks.forEach { item ->
                Button(onClick = { onWorkChanged(item) }, enabled = work != item) { Text(item.name) }
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (isOverlay) {
                ArtViewport(
                    modifier = Modifier.weight(1f).fillMaxHeight().onSizeChanged { overlayViewport = it },
                    primary = painting,
                    primaryName = work.name,
                    primaryGuide = if (showGuides) work.guide else emptyList(),
                    secondary = reference,
                    secondaryName = evidence.name,
                    secondaryPose = pose,
                    secondaryGuide = if (showGuides) evidence.guide else emptyList(),
                    opacity = opacity,
                    onTransform = { zoom, x, y, turn -> pose = pose.change(zoom, x, y, turn) },
                )
            } else {
                Column(Modifier.weight(1f)) {
                    GalleryText(work.name, emphasis = true)
                    ArtViewport(
                        modifier = Modifier.fillMaxSize(),
                        primary = painting,
                        primaryName = work.name,
                        primaryGuide = if (showGuides) work.guide else emptyList(),
                    )
                }
                Column(Modifier.weight(1f)) {
                    GalleryText(evidence.name, emphasis = true)
                    ArtViewport(
                        modifier = Modifier.fillMaxSize(),
                        primary = reference,
                        primaryName = evidence.name,
                        primaryPose = pose,
                        primaryGuide = if (showGuides) evidence.guide else emptyList(),
                        onTransform = { zoom, x, y, turn -> pose = pose.change(zoom, x, y, turn) },
                    )
                }
            }
            Column(
                Modifier.width(267.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionHeading("物证比对")
                work.evidence.forEach { option ->
                    Button(onClick = { evidence = option }, enabled = evidence != option) {
                        Text(option.kind + " · " + option.name, maxLines = 2)
                    }
                }
                GalleryText(evidence.prompt)
                Button(onClick = { showGuides = !showGuides }) {
                    Text(if (showGuides) "隐藏轮廓线" else "显示轮廓线")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { isOverlay = !isOverlay }) { Text(if (isOverlay) "切换为并置" else "切换为叠加") }
            if (isOverlay) {
                Button(onClick = { opacity = (opacity - .1f).coerceAtLeast(.1f) }) { Text("减淡") }
                GalleryText("叠层 ${(opacity * 100).roundToInt()}%")
                Button(onClick = { opacity = (opacity + .1f).coerceAtMost(.9f) }) { Text("加深") }
                val base = painting.bitmap
                val sketch = reference.bitmap
                if (work.id == "mona" && evidence.id == "portrait-study" && base != null && sketch != null &&
                    overlayViewport.width > 0 && overlayViewport.height > 0
                ) {
                    Button(onClick = {
                        pose = alignEyeLine(base, sketch, overlayViewport, work.guide, evidence.guide)
                        showGuides = true
                    }) { Text("按眼线初对齐") }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GalleryText("调整比对图")
            PoseControls(pose, { pose = it }, compact = true)
        }
    }
}

@Composable
private fun PoseControls(pose: ImagePose, onPose: (ImagePose) -> Unit, compact: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { onPose(pose.change(zoom = .77f)) }) { Text(if (compact) "缩小" else "缩小 −") }
        if (!compact) GalleryText("${(pose.scale * 100).roundToInt()}%")
        Button(onClick = { onPose(pose.change(zoom = 1.3f)) }) { Text(if (compact) "放大" else "放大 ＋") }
        Button(onClick = { onPose(pose.change(turn = 15f)) }) { Text("旋转") }
        if (compact) {
            Button(onClick = { onPose(pose.change(dx = -.025f)) }) { Text("左移") }
            Button(onClick = { onPose(pose.change(dx = .025f)) }) { Text("右移") }
            Button(onClick = { onPose(pose.change(dy = -.025f)) }) { Text("上移") }
            Button(onClick = { onPose(pose.change(dy = .025f)) }) { Text("下移") }
        }
        Button(onClick = { onPose(ImagePose()) }) { Text("复位") }
    }
}

@Composable
private fun ArtViewport(
    modifier: Modifier,
    primary: ArtImage,
    primaryName: String,
    primaryPose: ImagePose = ImagePose(),
    primaryGuide: List<Pair<Float, Float>> = emptyList(),
    secondary: ArtImage? = null,
    secondaryName: String = "",
    secondaryPose: ImagePose = ImagePose(),
    secondaryGuide: List<Pair<Float, Float>> = emptyList(),
    opacity: Float = 1f,
    markers: List<Hotspot> = emptyList(),
    currentMarker: Hotspot? = null,
    detailId: String? = null,
    onTransform: ((Float, Float, Float, Float) -> Unit)? = null,
) {
    val context = LocalContext.current
    val input = if (onTransform == null) Modifier else {
        Modifier.pointerInput(primaryName, secondaryName, context) {
            detectSpatialTransformGesture(context) { value ->
                onTransform(
                    value.scaleValue,
                    value.dragAmount.x / size.width.coerceAtLeast(1),
                    value.dragAmount.y / size.height.coerceAtLeast(1),
                    value.rotation.toQuaternion().toEulerAngles().roll,
                )
            }
        }.pointerInput(primaryName, secondaryName) {
            detectTransformGestures { _, pan, zoom, turn ->
                if (zoom != 1f || turn != 0f) {
                    onTransform(zoom, pan.x / size.width.coerceAtLeast(1), pan.y / size.height.coerceAtLeast(1), turn)
                }
            }
        }
    }
    BoxWithConstraints(modifier.clip(RoundedCornerShape(8.dp)).background(Sienna).clipToBounds().then(input)) {
        val viewport = with(LocalDensity.current) { Offset(maxWidth.toPx(), maxHeight.toPx()) }
        val bitmap = primary.bitmap
        if (bitmap == null) {
            GalleryText(
                if (primary.loading) "正在载入图像" else "暂无可用图像",
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            ArtworkLayer(bitmap, primaryName, primaryPose, viewport, 1f, primaryGuide, Gold, markers, currentMarker,
                detailId, primary.sourceWidth, primary.sourceHeight)
        }
        if (secondary != null) {
            secondary.bitmap?.let {
                ArtworkLayer(it, secondaryName, secondaryPose, viewport, opacity, secondaryGuide, Ink)
            } ?: GalleryText(
                if (secondary.loading) "正在载入比对图" else "暂无比对图像",
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.ArtworkLayer(
    bitmap: ImageBitmap,
    name: String,
    pose: ImagePose,
    viewport: Offset,
    opacity: Float,
    guide: List<Pair<Float, Float>>,
    guideColor: androidx.compose.ui.graphics.Color,
    markers: List<Hotspot> = emptyList(),
    currentMarker: Hotspot? = null,
    detailId: String? = null,
    sourceWidth: Int = 0,
    sourceHeight: Int = 0,
) {
    val ratio = bitmap.width.toFloat() / bitmap.height
    val width = minOf(maxWidth, maxHeight * ratio)
    val height = width / ratio
    val displaySize = with(LocalDensity.current) { Offset(width.toPx(), height.toPx()) }
    val region = if (pose.scale >= 2.5f && sourceWidth > bitmap.width) {
        visibleSourceRegion(viewport, displaySize, sourceWidth, sourceHeight, pose.scale,
            Offset(pose.x * viewport.x, pose.y * viewport.y), pose.rotation)
    } else {
        null
    }
    val tile = detailId?.let { detailTile(it, region) }
    Box(
        Modifier.align(Alignment.Center).width(width).height(height)
            .graphicsLayer {
                scaleX = pose.scale
                scaleY = pose.scale
                translationX = pose.x * viewport.x
                translationY = pose.y * viewport.y
                rotationZ = pose.rotation
                alpha = opacity
            },
    ) {
        Image(bitmap, contentDescription = name, modifier = Modifier.fillMaxSize())
        tile?.let {
            Image(
                it.bitmap,
                contentDescription = null,
                modifier = Modifier
                    .offset(width * (it.pixels.left.toFloat() / sourceWidth), height * (it.pixels.top.toFloat() / sourceHeight))
                    .size(width * (it.pixels.width().toFloat() / sourceWidth), height * (it.pixels.height().toFloat() / sourceHeight)),
            )
        }
        if (guide.isNotEmpty() || markers.isNotEmpty()) {
            Canvas(Modifier.fillMaxSize()) {
                guide.zipWithNext().forEach { (a, b) ->
                    drawLine(guideColor, Offset(a.first * size.width, a.second * size.height),
                        Offset(b.first * size.width, b.second * size.height), 2.5.dp.toPx())
                }
                markers.forEach { marker ->
                    val center = Offset(marker.x * size.width, marker.y * size.height)
                    val radius = (if (marker == currentMarker) 7.dp else 4.dp).toPx() / pose.scale
                    drawCircle(Gold, radius, center, style = Stroke(1.4.dp.toPx() / pose.scale))
                }
            }
        }
    }
}

@Composable
internal fun CollectionPage(onOpenWork: (Artwork) -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading("作品瀑布流")
        GalleryText("核心六件配有观察热点。目录也收录合作作品、壁画及归属有争议的作品，并逐项标注。")
        Row(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            repeat(3) { lane ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    allWorks.filterIndexed { index, _ -> index % 3 == lane }.forEach { work ->
                        CollectionTile(work) { onOpenWork(work) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionTile(work: Artwork, onOpen: () -> Unit) {
    val image = artImage(work.id, 500)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Sienna).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val bitmap = image.bitmap
        if (bitmap != null) {
            Image(bitmap, work.name, Modifier.fillMaxWidth()
                .aspectRatio((bitmap.width.toFloat() / bitmap.height).coerceIn(.7f, 2.2f)))
        } else {
            GalleryText(if (image.loading) "正在载入缩略图" else "暂无可用缩略图")
        }
        Button(onClick = onOpen) { Text(work.name) }
        GalleryText(work.catalog)
    }
}

@Composable
internal fun StationArtwork(work: Artwork, onOpen: () -> Unit) {
    val image = artImage(work.id, 400)
    Column(Modifier.width(118.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        image.bitmap?.let { bitmap ->
            Image(bitmap, work.name, Modifier.fillMaxWidth().height(78.dp))
        }
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("展牌 · ${work.name}", maxLines = 2) }
    }
}
