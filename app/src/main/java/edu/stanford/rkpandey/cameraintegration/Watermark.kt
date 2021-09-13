package edu.stanford.rkpandey.cameraintegration

import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Paint.DITHER_FLAG
import android.os.Build
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
fun addWatermark(
    bitmap: Bitmap,
    watermarkText: String,
    options: WatermarkOptions = WatermarkOptions()
): Bitmap {
    val result = bitmap.copy(bitmap.config, true)
    val canvas = Canvas(result)
    val paint = Paint(ANTI_ALIAS_FLAG or DITHER_FLAG)
    val paintFontMetrics = Paint.FontMetrics()
    paint.getFontMetrics(paintFontMetrics)

    paint.textAlign = when (options.corner) {
        Corner.TOP_LEFT,
        Corner.BOTTOM_LEFT -> Paint.Align.LEFT
        Corner.TOP_RIGHT,
        Corner.BOTTOM_RIGHT -> Paint.Align.RIGHT
    }
    val textSize = result.width * options.textSizeToWidthRatio
    paint.textSize = textSize

    if (options.shadowColor != null) {
        paint.setShadowLayer(textSize / 2, 0f, 0f, options.shadowColor)
    }
    if (options.typeface != null) {
        paint.typeface = options.typeface
    }
    val padding = result.width * options.paddingToWidthRatio
    val coordinates =
        calculateCoordinates(watermarkText, paint, options, canvas.width, canvas.height, padding)
    paint.color = Color.argb(0.3f, 0f, 0f, 0f)

    canvas.drawRect(0f, bitmap.height.toFloat(), bitmap.width.toFloat(), bitmap.height.toFloat() - 900.0f, paint)
    paint.color = options.textColor

    //canvas.drawText(watermarkText, coordinates.x, coordinates.y, paint)
    drawMultilineText(watermarkText, coordinates.x, coordinates.y - 700f, paint, canvas)
    return result
}

private fun calculateCoordinates(
    watermarkText: String,
    paint: Paint,
    options: WatermarkOptions,
    width: Int,
    height: Int,
    padding: Float
): PointF {
    val x = when (options.corner) {
        Corner.TOP_LEFT,
        Corner.BOTTOM_LEFT -> {
            padding
        }
        Corner.TOP_RIGHT,
        Corner.BOTTOM_RIGHT -> {
            width - padding
        }
    }
    val y = when (options.corner) {
        Corner.BOTTOM_LEFT,
        Corner.BOTTOM_RIGHT -> {
            height - padding
        }
        Corner.TOP_LEFT,
        Corner.TOP_RIGHT -> {
            val bounds = Rect()
            paint.getTextBounds(watermarkText, 0, watermarkText.length, bounds)
            val textHeight = bounds.height()
            textHeight + padding

        }
    }
    return PointF(x, y)
}

enum class Corner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

data class WatermarkOptions(
    val corner: Corner = Corner.BOTTOM_LEFT,
    val textSizeToWidthRatio: Float = 0.04f,
    val paddingToWidthRatio: Float = 0.03f,
    @ColorInt val textColor: Int = Color.WHITE,
    @ColorInt val shadowColor: Int? = Color.BLACK,
    val typeface: Typeface? = null
)

fun drawMultilineText(str: String, x: Float, y: Float, paint: Paint, canvas: Canvas) {
    var bounds = Rect()
    var lineHeight = 0
    var yoffset = 0
    val lines = str.split("\n").toTypedArray()

    // set height of each line (height of text + 20%)
    paint.getTextBounds("Ig", 0, 2, bounds)
    lineHeight = (bounds.height().toFloat() * 1.4).toInt()
    // draw each line
    for (i in lines.indices) {
        canvas.drawText(lines[i], x, (y + yoffset), paint)
        yoffset += lineHeight
    }
}