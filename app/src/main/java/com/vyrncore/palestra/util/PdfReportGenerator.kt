package com.vyrncore.palestra.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.vyrncore.palestra.data.local.dao.PersonalRecord
import com.vyrncore.palestra.data.local.dao.SessionSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a one-page PDF progress report for a PT to hand a client - drawn directly with
 * [android.graphics.pdf.PdfDocument] and [android.graphics.Canvas], so it needs no PDF library
 * dependency and nothing here has to be verified against a third-party API surface.
 */
object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f

    fun shareClientReport(
        context: Context,
        clientName: String,
        sessions: List<SessionSummary>,
        personalRecords: List<PersonalRecord>,
    ) {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true }
        val headingPaint = Paint().apply { textSize = 15f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 12f }
        val mutedPaint = Paint().apply { textSize = 11f; color = 0xFF666666.toInt() }

        var y = MARGIN + 20f
        canvas.drawText("Report allenamento — $clientName", MARGIN, y, titlePaint)
        y += 20f
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.ITALY)
        canvas.drawText("Generato il ${dateFormat.format(Date())}", MARGIN, y, mutedPaint)
        y += 36f

        val completed = sessions.filter { it.endedAtEpochMs != null }
        val totalVolumeKg = completed.sumOf { it.totalVolumeKg }
        val totalSets = completed.sumOf { it.setCount }

        canvas.drawText("Riepilogo generale", MARGIN, y, headingPaint)
        y += 22f
        listOf(
            "Allenamenti completati: ${completed.size}",
            "Serie totali registrate: $totalSets",
            "Volume totale sollevato: ${"%.0f".format(totalVolumeKg)} kg",
        ).forEach {
            canvas.drawText(it, MARGIN, y, bodyPaint)
            y += 18f
        }
        y += 20f

        canvas.drawText("Record personali (1RM stimato)", MARGIN, y, headingPaint)
        y += 22f
        if (personalRecords.isEmpty()) {
            canvas.drawText("Nessun record ancora registrato.", MARGIN, y, mutedPaint)
            y += 18f
        } else {
            personalRecords.take(15).forEach { record ->
                canvas.drawText(
                    "${record.exerciseName}: ${"%.1f".format(record.estimatedOneRepMaxKg)} kg",
                    MARGIN,
                    y,
                    bodyPaint,
                )
                y += 18f
                if (y > PAGE_HEIGHT - MARGIN) return@forEach
            }
        }
        y += 20f

        canvas.drawText("Ultimi allenamenti", MARGIN, y, headingPaint)
        y += 22f
        val timeFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        completed.sortedByDescending { it.startedAtEpochMs }.take(10).forEach { session ->
            if (y > PAGE_HEIGHT - MARGIN) return@forEach
            val duration = session.endedAtEpochMs?.let { (it - session.startedAtEpochMs) / 60_000 }
            canvas.drawText(
                "${timeFormat.format(Date(session.startedAtEpochMs))} · ${session.setCount} serie · " +
                    "${"%.0f".format(session.totalVolumeKg)} kg" + (duration?.let { " · $it min" } ?: ""),
                MARGIN,
                y,
                bodyPaint,
            )
            y += 18f
        }

        document.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "report_${clientName.replace(" ", "_")}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Condividi report"))
    }
}
