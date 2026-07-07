package com.nabil.aireels.data.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
            textSize = videoWidth * 0.055f
            textAlign = Paint.Align.CENTER
        }

        val backgroundPaint = Paint().apply {
            color = Color.parseColor("#B3000000")
        }

        val maxTextWidth = (videoWidth * 0.85f).toInt()
        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(1.1f, 1.1f)
            .build()

        val textBlockHeight = staticLayout.height
        val bottomMargin = videoHeight * 0.12f
        val paddingVertical = 24f
        val barTop = videoHeight - bottomMargin - textBlockHeight - paddingVertical
        val barBottom = videoHeight - bottomMargin + paddingVertical

        canvas.drawRect(0f, barTop, videoWidth.toFloat(), barBottom, backgroundPaint)

        canvas.save()
        canvas.translate((videoWidth - maxTextWidth) / 2f, barTop + paddingVertical / 2f)
        staticLayout.draw(canvas)
        canvas.restore()

        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return outputFile.absolutePath
    }
}
