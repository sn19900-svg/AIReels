package com.nabil.aireels.data.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptionRenderer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun renderCaptionPng(
        text: String,
        videoWidth: Int,
        videoHeight: Int,
        outputFile: File
    ): String {
        val bitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = videoWidth * 0.052f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }

        val backgroundPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#CC000000")
        }

        val accentPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#E94F8C")
        }

        val maxTextWidth = (videoWidth * 0.82f).toInt()
        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(1.15f, 1.15f)
            .build()

        val textBlockHeight = staticLayout.height
        val paddingVertical = 28f
        val paddingHorizontal = 32f

        // منطقة آمنة: إنستغرام يغطي آخر ~450 بكسل من الأسفل بأزرار التفاعل والكابشن
        val bottomSafeZone = videoHeight * 0.27f
        val barHeight = textBlockHeight + paddingVertical * 2
        val barBottom = videoHeight - bottomSafeZone
        val barTop = barBottom - barHeight

        val barWidth = maxTextWidth + paddingHorizontal * 2
        val barLeft = (videoWidth - barWidth) / 2f
        val barRight = barLeft + barWidth

        val rect = RectF(barLeft, barTop, barRight, barBottom)
        canvas.drawRoundRect(rect, 20f, 20f, backgroundPaint)

        // خط علوي رفيع بلون العلامة التجارية
        canvas.drawRoundRect(
            RectF(barLeft, barTop, barRight, barTop + 6f),
            3f, 3f, accentPaint
        )

        canvas.save()
        canvas.translate((videoWidth - maxTextWidth) / 2f, barTop + paddingVertical)
        staticLayout.draw(canvas)
        canvas.restore()

        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return outputFile.absolutePath
    }
}
