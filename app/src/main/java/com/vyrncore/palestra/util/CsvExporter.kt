package com.vyrncore.palestra.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.vyrncore.palestra.data.local.entity.BodyMetricEntity
import com.vyrncore.palestra.ui.history.HistoryItemUi
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Exports the allievo's workout history as a CSV, shared via the platform's own share sheet
 * (email, Drive, WhatsApp, ...) rather than saved silently - the person picks where it goes. */
object CsvExporter {

    fun shareWorkoutHistory(context: Context, history: List<HistoryItemUi>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ITALY)
        val csv = buildString {
            appendLine("Data,Scheda,Durata (min),Serie,Volume (kg)")
            history.forEach { item ->
                appendLine(
                    listOf(
                        dateFormat.format(Date(item.startedAtEpochMs)),
                        item.planName.replace(",", " "),
                        item.durationMinutes?.toString().orEmpty(),
                        item.setCount.toString(),
                        "%.1f".format(item.totalVolumeKg),
                    ).joinToString(","),
                )
            }
        }

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "cronologia_allenamenti.csv")
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Esporta cronologia allenamenti"))
    }

    /** Everything the allievo has recorded - workout history and body metrics - as one CSV with
     * two sections, for someone who wants a full local copy of their own data. */
    fun shareFullExport(context: Context, history: List<HistoryItemUi>, bodyMetrics: List<BodyMetricEntity>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ITALY)
        val csv = buildString {
            appendLine("=== ALLENAMENTI ===")
            appendLine("Data,Scheda,Durata (min),Serie,Volume (kg)")
            history.forEach { item ->
                appendLine(
                    listOf(
                        dateFormat.format(Date(item.startedAtEpochMs)),
                        item.planName.replace(",", " "),
                        item.durationMinutes?.toString().orEmpty(),
                        item.setCount.toString(),
                        "%.1f".format(item.totalVolumeKg),
                    ).joinToString(","),
                )
            }
            appendLine()
            appendLine("=== MISURAZIONI CORPOREE ===")
            appendLine("Data,Peso (kg),Massa grassa (%),Torace (cm),Vita (cm),Fianchi (cm),Braccio (cm),Coscia (cm),Note")
            bodyMetrics.forEach { metric ->
                appendLine(
                    listOf(
                        dateFormat.format(Date(metric.dateEpochMs)),
                        metric.weightKg?.toString().orEmpty(),
                        metric.bodyFatPercent?.toString().orEmpty(),
                        metric.chestCm?.toString().orEmpty(),
                        metric.waistCm?.toString().orEmpty(),
                        metric.hipsCm?.toString().orEmpty(),
                        metric.armCm?.toString().orEmpty(),
                        metric.thighCm?.toString().orEmpty(),
                        metric.notes?.replace(",", " ").orEmpty(),
                    ).joinToString(","),
                )
            }
        }

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "vibe_fitness_export_completo.csv")
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Esporta tutti i dati"))
    }
}
