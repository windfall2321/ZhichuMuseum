package com.interactimuse.spatial.content.leonardo

import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

internal data class DetailTile(
    val pixels: Rect,
    val bitmap: ImageBitmap,
)

/** Decode only the visible source pixels at deep zoom; the overview stays beneath this tile. */
@Composable
internal fun detailTile(id: String, pixels: Rect?): DetailTile? {
    val assets = LocalContext.current.assets
    return produceState<DetailTile?>(null, id, pixels) {
        value = null
        if (pixels == null) return@produceState
        delay(110)
        value = withContext(Dispatchers.IO) {
            try {
                assets.open("leonardo/$id.jpg").use { input ->
                    val decoder = BitmapRegionDecoder.newInstance(input) ?: return@use null
                    try {
                        val options = BitmapFactory.Options()
                        while (maxOf(pixels.width(), pixels.height()) / options.inSampleSize.coerceAtLeast(1) > 3000) {
                            options.inSampleSize = options.inSampleSize.coerceAtLeast(1) * 2
                        }
                        decoder.decodeRegion(pixels, options)?.let { DetailTile(pixels, it.asImageBitmap()) }
                    } finally {
                        decoder.recycle()
                    }
                }
            } catch (_: java.io.IOException) {
                // The sampled overview remains visible if a region cannot be decoded.
                null
            }
        }
    }.value
}

internal fun visibleSourceRegion(
    viewport: Offset,
    displayed: Offset,
    sourceWidth: Int,
    sourceHeight: Int,
    scale: Float,
    pan: Offset,
    rotation: Float,
): Rect? {
    if (viewport.x <= 0f || viewport.y <= 0f || displayed.x <= 0f || displayed.y <= 0f) return null
    val radians = rotation * (Math.PI / 180).toFloat()
    val cosine = cos(radians)
    val sine = sin(radians)
    val corners = listOf(Offset.Zero, Offset(viewport.x, 0f), viewport, Offset(0f, viewport.y)).map { screen ->
        val centered = screen - (viewport * .5f) - pan
        val x = (centered.x * cosine + centered.y * sine) / (displayed.x * scale) + .5f
        val y = (-centered.x * sine + centered.y * cosine) / (displayed.y * scale) + .5f
        Offset(x, y)
    }
    // Round regions to a tile grid so small gestures do not trigger a new decode.
    val grid = 128f
    val left = (floor(corners.minOf { it.x } * sourceWidth / grid) * grid).toInt().coerceIn(0, sourceWidth)
    val right = (ceil(corners.maxOf { it.x } * sourceWidth / grid) * grid).toInt().coerceIn(0, sourceWidth)
    val top = (floor(corners.minOf { it.y } * sourceHeight / grid) * grid).toInt().coerceIn(0, sourceHeight)
    val bottom = (ceil(corners.maxOf { it.y } * sourceHeight / grid) * grid).toInt().coerceIn(0, sourceHeight)
    return if (left < right && top < bottom) Rect(left, top, right, bottom) else null
}
