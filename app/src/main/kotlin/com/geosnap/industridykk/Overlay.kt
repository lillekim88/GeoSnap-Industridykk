
package com.geosnap.industridykk

import android.graphics.*
import java.text.SimpleDateFormat
import java.util.*

object Overlay {
    fun buildLines(utmText: String, address: String, place: String, comment: String, timestamp: Long): List<String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm (zzz)", Locale.getDefault())
        val ts = sdf.format(Date(timestamp))
        return listOf(
            "UTM: $utmText",
            "Adresse: $address",
            "Sted: $place",
            "Kommentar: $comment",
            "Dato/tid: $ts"
        )
    }

    fun drawOverlay(src: Bitmap, lines: List<String>): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val density = result.density.toFloat().takeIf { it>0 } ?: 440f
        val pad = (16 * (density/440f)).toInt()
        val textSize = 28f * (result.width / 1080f).coerceAtLeast(0.9f)

        val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#66000000") }
        val paintTx = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.MONOSPACE
        }
        val fm = paintTx.fontMetrics
        val lineH = fm.bottom - fm.top
        val blockW = lines.maxOf { paintTx.measureText(it) } + pad*2
        val blockH = lineH * lines.size + pad*2
        val x = pad.toFloat()
        val y = result.height - blockH - pad
        canvas.drawRoundRect(RectF(x, y, x+blockW, y+blockH), 18f, 18f, paintBg)
        var ty = y + pad - fm.top
        for (line in lines) {
            canvas.drawText(line, x + pad, ty, paintTx)
            ty += lineH
        }
        return result
    }
}
