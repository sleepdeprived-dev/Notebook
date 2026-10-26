package com.david.notebook.ui.schedule

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.david.notebook.R
import com.david.notebook.data.images.decodeBitmap
import com.david.notebook.ui.components.EmptyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val MAX_PDF_PAGES = 10
private const val MAX_RENDER_WIDTH = 2048
private const val MAX_IMAGE_SIDE = 3000
private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

private sealed interface Pages {
    data object Loading : Pages
    data object Failed : Pages
    data class Ready(val bitmaps: List<ImageBitmap>) : Pages
}

/** Shows the schedule file (PDF pages stacked, or a single image) with pinch-zoom and pan. */
@Composable
fun ScheduleViewer(file: File, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth
        val pages by produceState<Pages>(Pages.Loading, file, widthPx) {
            if (widthPx <= 0) return@produceState
            value = withContext(Dispatchers.IO) {
                try {
                    Pages.Ready(loadPages(file, widthPx))
                } catch (e: IOException) {
                    Pages.Failed
                } catch (e: SecurityException) { // password-protected PDF
                    Pages.Failed
                } catch (e: OutOfMemoryError) {
                    Pages.Failed
                }
            }
        }

        when (val p = pages) {
            Pages.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            Pages.Failed -> EmptyState(
                icon = R.drawable.ic_event,
                title = stringResource(R.string.schedule_open_failed_title),
                message = stringResource(R.string.schedule_open_failed),
            )
            is Pages.Ready -> Zoomable(key = file) {
                p.bitmaps.forEach { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.tab_schedule),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bitmap.width.toFloat() / bitmap.height)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
            }
        }
    }
}

/**
 * Pinch to zoom, drag to pan, double-tap to toggle zoom. The content is laid out at
 * full width and may be taller than the screen, so dragging also scrolls through pages.
 */
@Composable
private fun Zoomable(key: Any, content: @Composable ColumnScope.() -> Unit) {
    var scale by remember(key) { mutableFloatStateOf(1f) }
    var offset by remember(key) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }

    // Keep the content covering the viewport; centre it on an axis where it's smaller.
    fun clamp(o: Offset, s: Float): Offset {
        fun axis(value: Float, view: Int, size: Int): Float {
            val scaled = size * s
            return if (scaled <= view) (view - scaled) / 2 else value.coerceIn(view - scaled, 0f)
        }
        return Offset(
            axis(o.x, viewport.width, contentSize.width),
            axis(o.y, viewport.height, contentSize.height),
        )
    }

    // Zoom so the content point under `focus` stays under it.
    fun zoomTo(newScale: Float, focus: Offset, pan: Offset = Offset.Zero) {
        val current = clamp(offset, scale)
        offset = clamp(focus - (focus - current) * (newScale / scale) + pan, newScale)
        scale = newScale
    }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .pointerInput(key) {
                detectTapGestures(
                    onDoubleTap = { tap -> zoomTo(if (scale > 1f) 1f else DOUBLE_TAP_ZOOM, tap) },
                )
            }
            .pointerInput(key) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    zoomTo((scale * zoom).coerceIn(1f, MAX_ZOOM), centroid, pan)
                }
            },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                // Measured including the margin below, so the clamping maths sees the
                // full width that's actually drawn (and the margin zooms with the page).
                .onSizeChanged { contentSize = it }
                .graphicsLayer {
                    val shown = clamp(offset, scale)
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = scale
                    scaleY = scale
                    translationX = shown.x
                    translationY = shown.y
                }
                .padding(12.dp),
            content = content,
        )
    }
}

private fun loadPages(file: File, viewportWidth: Int): List<ImageBitmap> =
    if (file.extension == "pdf") renderPdf(file, viewportWidth)
    else listOf(decodeImage(file).asImageBitmap())

private fun renderPdf(file: File, viewportWidth: Int): List<ImageBitmap> =
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            val count = minOf(renderer.pageCount, MAX_PDF_PAGES)
            // Twice the screen width keeps text sharp when zoomed in; long PDFs drop
            // to 1x so ten pages of bitmaps don't run the app out of memory.
            val width = if (count <= 2) minOf(viewportWidth * 2, MAX_RENDER_WIDTH) else viewportWidth
            (0 until count).map { index ->
                renderer.openPage(index).use { page ->
                    val height = (width.toLong() * page.height / page.width).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE) // PDFs have a transparent background
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap.asImageBitmap()
                }
            }
        }
    }

private fun decodeImage(file: File): Bitmap = decodeBitmap(MAX_IMAGE_SIDE) { file.inputStream() }
