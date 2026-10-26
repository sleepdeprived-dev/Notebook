package com.david.notebook.data.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.IOException
import java.io.InputStream

/**
 * Decodes an image no larger than [maxSide] on its longest side, upright.
 *
 * [open] is called more than once (size, pixels, rotation), because a content://
 * stream can only be read through once. Big camera photos (12–50 MP) are
 * downsampled while decoding so they never need to fit in memory at full size,
 * and the EXIF rotation is applied, since phone cameras save the pixels sideways
 * and only record "rotate me 90°" in the metadata.
 */
fun decodeBitmap(maxSide: Int, open: () -> InputStream): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open().use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Not an image")

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = open().use { BitmapFactory.decodeStream(it, null, options) }
        ?: throw IOException("Cannot decode image")

    val orientation = try {
        open().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
    } catch (e: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
    }
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/** The centre square of [source], scaled to [size]×[size] — for avatar-style logos. */
fun centerSquare(source: Bitmap, size: Int): Bitmap {
    val side = minOf(source.width, source.height)
    val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
    return Bitmap.createScaledBitmap(square, size, size, true)
}
