package com.soapjournal.app.ui.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.soapjournal.app.data.SoapEntryEntity
import com.soapjournal.app.data.SoapSection
import com.soapjournal.app.data.ink.InkDocument
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PdfExporter {
    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

    fun exportEntry(
        context: Context,
        entry: SoapEntryEntity,
        inkBySection: Map<SoapSection, InkDocument>
    ): android.net.Uri {
        val doc = PdfDocument()
        val pageWidth = 612
        val pageHeight = 792
        var pageNumber = 1

        writeCoverPage(doc, entry, pageWidth, pageHeight, pageNumber++)

        SoapSection.entries.forEach { section ->
            writeSectionPage(
                doc = doc,
                entry = entry,
                section = section,
                ink = inkBySection[section] ?: InkDocument(),
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                pageNumber = pageNumber++
            )
        }

        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val safeRef = entry.scriptureReference.ifBlank { "SOAP" }
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .take(40)
        val outFile = File(dir, "${safeRef}_${entry.id}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }

    private fun writeCoverPage(
        doc: PdfDocument,
        entry: SoapEntryEntity,
        pageWidth: Int,
        pageHeight: Int,
        pageNumber: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor("#F7F1E8".toColorInt())

        val titlePaint = TextPaint().apply {
            color = "#2C2416".toColorInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = TextPaint().apply {
            color = "#2C2416".toColorInt()
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val date = LocalDate.ofEpochDay(entry.entryDateEpochDay)
        canvas.drawText("SOAP Journal", 48f, 96f, titlePaint)
        canvas.drawText(date.format(dateFormatter), 48f, 140f, bodyPaint)
        canvas.drawText(
            entry.scriptureReference.ifBlank { "No reference yet" },
            48f,
            180f,
            bodyPaint.apply { textSize = 18f; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD) }
        )

        if (entry.scriptureText.isNotBlank()) {
            val layout = StaticLayout.Builder.obtain(
                entry.scriptureText,
                0,
                entry.scriptureText.length,
                bodyPaint.apply { textSize = 13f },
                pageWidth - 96
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .build()
            canvas.save()
            canvas.translate(48f, 220f)
            layout.draw(canvas)
            canvas.restore()
        }

        if (entry.tags.isNotBlank()) {
            canvas.drawText("Tags: ${entry.tags}", 48f, pageHeight - 64f, bodyPaint)
        }
        canvas.drawText(
            "Exported ${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}",
            48f,
            pageHeight - 40f,
            bodyPaint.apply { textSize = 10f; color = "#5C6B4A".toColorInt() }
        )
        doc.finishPage(page)
    }

    private fun writeSectionPage(
        doc: PdfDocument,
        entry: SoapEntryEntity,
        section: SoapSection,
        ink: InkDocument,
        pageWidth: Int,
        pageHeight: Int,
        pageNumber: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor("#F7F1E8".toColorInt())

        val titlePaint = Paint().apply {
            color = "#5C6B4A".toColorInt()
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val promptPaint = Paint().apply {
            color = "#2C2416".toColorInt()
            textSize = 12f
            isAntiAlias = true
        }

        canvas.drawText(section.title, 40f, 48f, titlePaint)
        canvas.drawText(section.prompt, 40f, 72f, promptPaint)
        if (section == SoapSection.SCRIPTURE && entry.scriptureText.isNotBlank()) {
            val textPaint = TextPaint().apply {
                color = "#2C2416".toColorInt()
                textSize = 12f
                isAntiAlias = true
            }
            val layout = StaticLayout.Builder.obtain(
                entry.scriptureText,
                0,
                entry.scriptureText.length,
                textPaint,
                pageWidth - 80
            ).build()
            canvas.save()
            canvas.translate(40f, 90f)
            layout.draw(canvas)
            canvas.restore()
        }

        drawInk(canvas, ink, left = 40f, top = 160f, maxWidth = pageWidth - 80f, maxHeight = pageHeight - 220f)
        doc.finishPage(page)
    }

    private fun drawInk(
        canvas: Canvas,
        ink: InkDocument,
        left: Float,
        top: Float,
        maxWidth: Float,
        maxHeight: Float
    ) {
        if (ink.strokes.isEmpty()) return
        val srcWidth = ink.canvasWidth.takeIf { it > 0f } ?: maxWidth
        val srcHeight = ink.canvasHeight.takeIf { it > 0f } ?: maxHeight
        val scale = minOf(maxWidth / srcWidth, maxHeight / srcHeight, 1f)

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        ink.strokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            paint.color = if (stroke.isEraser) "#F7F1E8".toColorInt() else stroke.colorArgb
            paint.strokeWidth = stroke.width * scale
            val path = Path()
            val first = stroke.points.first()
            path.moveTo(left + first.x * scale, top + first.y * scale)
            for (i in 1 until stroke.points.size) {
                val p = stroke.points[i]
                path.lineTo(left + p.x * scale, top + p.y * scale)
            }
            canvas.drawPath(path, paint)
        }
    }
}
