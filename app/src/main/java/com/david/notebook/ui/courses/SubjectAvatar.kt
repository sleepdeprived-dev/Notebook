package com.david.notebook.ui.courses

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Decoded logos, so scrolling back to a card (or returning from a subject) shows the
 * logo at once instead of flashing the letter first. Logos are 256 px, about 256 kB each.
 * Keyed by path, and each new logo gets a new file name, so a cached entry is never stale.
 */
private val logoCache = LruCache<String, ImageBitmap>(32)

/** The subject's logo if it has one, otherwise its first letter on an accent tile. */
@Composable
fun SubjectAvatar(
    name: String,
    logo: File?,
    size: Dp = 52.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
) {
    val bitmap by produceState(logo?.let { logoCache.get(it.path) }, logo) {
        if (logo != null && value == null) {
            value = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(logo.path)?.asImageBitmap()?.also { logoCache.put(logo.path, it) }
            }
        }
    }
    Surface(
        shape = RoundedCornerShape(size * 0.3f),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(size),
    ) {
        val image = bitmap
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(name.trim().take(1).uppercase(), style = textStyle)
            }
        }
    }
}
