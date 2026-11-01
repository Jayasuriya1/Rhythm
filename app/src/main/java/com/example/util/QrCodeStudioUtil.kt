package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class QrStudioTemplate(
    val templateId: String,
    val displayName: String,
    val description: String,
    val defaultFgColor: Int,
    val defaultBgColor: Int,
    val accentColor: Int,
    val category: String
) {
    MINIMAL_BW(
        templateId = "MINIMAL_BW",
        displayName = "Minimal B&W",
        description = "High contrast, crisp pure black on white paper",
        defaultFgColor = Color.BLACK,
        defaultBgColor = Color.WHITE,
        accentColor = Color.DKGRAY,
        category = "Classic"
    ),
    MODERN_OLED(
        templateId = "MODERN_OLED",
        displayName = "Modern OLED",
        description = "Deep charcoal with crisp white code and teal accent",
        defaultFgColor = Color.WHITE,
        defaultBgColor = Color.parseColor("#121212"),
        accentColor = Color.parseColor("#4ED8C2"),
        category = "Dark"
    ),
    SOFT_PASTEL(
        templateId = "SOFT_PASTEL",
        displayName = "Soft Sage",
        description = "Calming forest green on soft warm linen background",
        defaultFgColor = Color.parseColor("#264639"),
        defaultBgColor = Color.parseColor("#F5F7F4"),
        accentColor = Color.parseColor("#4D7C66"),
        category = "Pastel"
    ),
    MODERN_TEAL(
        templateId = "MODERN_TEAL",
        displayName = "Modern Teal",
        description = "Vibrant deep ocean cyan for study desks and monitors",
        defaultFgColor = Color.parseColor("#0F4C5C"),
        defaultBgColor = Color.WHITE,
        accentColor = Color.parseColor("#009688"),
        category = "Color"
    ),
    PURPLE_FOCUS(
        templateId = "PURPLE_FOCUS",
        displayName = "Purple Focus",
        description = "Rich twilight purple for nightstands and study spaces",
        defaultFgColor = Color.parseColor("#3B185F"),
        defaultBgColor = Color.parseColor("#FAF5FF"),
        accentColor = Color.parseColor("#7B2CBF"),
        category = "Color"
    ),
    HOME_ESSENTIALS(
        templateId = "HOME_ESSENTIALS",
        displayName = "Home Essentials",
        description = "Clean framed card for kitchen coffee, fridge, or toothpaste",
        defaultFgColor = Color.parseColor("#1E293B"),
        defaultBgColor = Color.WHITE,
        accentColor = Color.parseColor("#F59E0B"),
        category = "Household"
    ),
    MINIMAL_LABEL(
        templateId = "MINIMAL_LABEL",
        displayName = "Compact Sticker",
        description = "Compact format with bold room hint, perfect for small items",
        defaultFgColor = Color.BLACK,
        defaultBgColor = Color.parseColor("#FAFAFA"),
        accentColor = Color.parseColor("#2563EB"),
        category = "Household"
    ),
    LARGE_WALL_PRINT(
        templateId = "LARGE_WALL_PRINT",
        displayName = "Wall Poster",
        description = "Generous margins, bold instructions, highly visible from across room",
        defaultFgColor = Color.BLACK,
        defaultBgColor = Color.WHITE,
        accentColor = Color.parseColor("#DC2626"),
        category = "Print"
    ),
    MOTIVATIONAL_MORNING(
        templateId = "MOTIVATIONAL_MORNING",
        displayName = "Morning Rise",
        description = "Warm golden sunrise tones with 'Wake Up & Conquer the Day'",
        defaultFgColor = Color.parseColor("#78350F"),
        defaultBgColor = Color.parseColor("#FFFBEB"),
        accentColor = Color.parseColor("#D97706"),
        category = "Inspirational"
    ),
    CUSTOM(
        templateId = "CUSTOM",
        displayName = "Custom Studio",
        description = "Personalize colors, label, location hint, and typography",
        defaultFgColor = Color.BLACK,
        defaultBgColor = Color.WHITE,
        accentColor = Color.parseColor("#3B82F6"),
        category = "Custom"
    );

    companion object {
        fun fromId(id: String): QrStudioTemplate {
            return entries.find { it.templateId.equals(id, ignoreCase = true) } ?: MINIMAL_BW
        }
    }
}

enum class PdfPrintPageSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 Page (Standard)", 595, 842),
    A5("A5 Page (Half Sheet)", 420, 595),
    STICKER_SINGLE("Sticker (3.5\" x 4.5\")", 252, 324),
    STICKER_SHEET_A4("A4 Multi-Sticker Sheet (4-Up)", 595, 842)
}

data class QrCardDesignConfig(
    val template: QrStudioTemplate = QrStudioTemplate.MINIMAL_BW,
    val title: String = "Alarm Dismissal Code",
    val locationHint: String? = "Kitchen / Living Room",
    val subtext: String = "Scan to dismiss alarm and start your day",
    val codeValue: String = "",
    val customFgColor: Int? = null,
    val customBgColor: Int? = null,
    val showCutLines: Boolean = true
) {
    val fgColor: Int get() = customFgColor ?: template.defaultFgColor
    val bgColor: Int get() = customBgColor ?: template.defaultBgColor
}

object QrCodeStudioUtil {

    fun generateUniqueCodeString(): String {
        val randomPart = UUID.randomUUID().toString().take(8).uppercase()
        return "RHYTHM-FOCUS-$randomPart"
    }

    /**
     * Contrast ratio according to WCAG 2.1 specifications.
     * Values < 4.5 fail minimum contrast standards for scannability.
     */
    fun calculateContrastRatio(color1: Int, color2: Int): Double {
        val l1 = getLuminance(color1)
        val l2 = getLuminance(color2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun getLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0

        val rL = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
        val gL = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
        val bL = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)

        return 0.2126 * rL + 0.7152 * gL + 0.0722 * bL
    }

    fun isContrastSufficient(color1: Int, color2: Int): Boolean {
        return calculateContrastRatio(color1, color2) >= 4.5
    }

    /**
     * Pure ZXing QR Bitmap generator. 100% offline.
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE,
        margin: Int = 2
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to margin
        )

        val bitMatrix = QRCodeWriter().encode(
            content.ifBlank { "RHYTHM-FOCUS-SAMPLE" },
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) fgColor else bgColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Render a styled card containing header, location tag, QR code, and footer instructions.
     */
    fun renderStyledCardBitmap(
        config: QrCardDesignConfig,
        cardWidth: Int = 800,
        cardHeight: Int = 1100
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), bgPaint)

        // Outer Accent Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.template.accentColor
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val borderMargin = 28f
        val cardRect = RectF(borderMargin, borderMargin, cardWidth - borderMargin, cardHeight - borderMargin)
        canvas.drawRoundRect(cardRect, 24f, 24f, borderPaint)

        // Inner Card Background Header Banner
        val headerHeight = 160f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.template.accentColor
            alpha = 24
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(borderMargin + 4, borderMargin + 4, cardWidth - borderMargin - 4, borderMargin + headerHeight),
            20f, 20f, headerPaint
        )

        // Header Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.fgColor
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(config.title.take(32), cardWidth / 2f, borderMargin + 72f, titlePaint)

        // Location / Category Pill
        val locationText = if (!config.locationHint.isNullOrBlank()) "📍 ${config.locationHint}" else "⏰ Alarm Mission Target"
        val locationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.template.accentColor
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(locationText.take(40), cardWidth / 2f, borderMargin + 124f, locationPaint)

        // Render QR Code in center
        val qrSize = 520
        val qrBitmap = generateQrBitmap(
            content = config.codeValue,
            size = qrSize,
            fgColor = config.fgColor,
            bgColor = config.bgColor,
            margin = 1
        )
        val qrLeft = (cardWidth - qrSize) / 2f
        val qrTop = borderMargin + headerHeight + 50f
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        // Code Value label underneath QR
        val codeValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.fgColor
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            alpha = 190
        }
        val displayCode = if (config.codeValue.length > 28) config.codeValue.take(28) + "…" else config.codeValue
        canvas.drawText(displayCode, cardWidth / 2f, qrTop + qrSize + 48f, codeValPaint)

        // Subtext / Instructions
        val subtextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.fgColor
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(config.subtext.take(48), cardWidth / 2f, qrTop + qrSize + 104f, subtextPaint)

        // App Branding Footer
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.template.accentColor
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            alpha = 170
        }
        canvas.drawText("✦ Rhythm & Focus — Offline Verified Alarm Mission ✦", cardWidth / 2f, cardHeight - borderMargin - 30f, brandPaint)

        return bitmap
    }

    /**
     * Save high-resolution PNG to cache or external files dir and return Content URI.
     */
    fun exportPng(context: Context, config: QrCardDesignConfig): Uri? {
        return try {
            val bitmap = renderStyledCardBitmap(config, 1024, 1400)
            val exportDir = File(context.cacheDir, "exported_qr").apply { mkdirs() }
            val fileName = "rhythm_qr_${System.currentTimeMillis()}.png"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate print-ready PDF using android.graphics.pdf.PdfDocument.
     */
    fun exportPdf(
        context: Context,
        config: QrCardDesignConfig,
        pageSize: PdfPrintPageSize = PdfPrintPageSize.A4
    ): Uri? {
        return try {
            val document = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(pageSize.widthPt, pageSize.heightPt, 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Background of page is always white for printing
                val pageBgPaint = Paint().apply { color = Color.WHITE }
                canvas.drawRect(0f, 0f, pageSize.widthPt.toFloat(), pageSize.heightPt.toFloat(), pageBgPaint)

                when (pageSize) {
                    PdfPrintPageSize.STICKER_SHEET_A4 -> {
                        // Draw 4 cards tiled 2x2 on A4 with cut lines
                        val cardW = 260
                        val cardH = 360
                        val cardBitmap = renderStyledCardBitmap(config, cardW * 2, cardH * 2)

                        val startX = (pageSize.widthPt - (cardW * 2 + 30)) / 2f
                        val startY = (pageSize.heightPt - (cardH * 2 + 40)) / 2f

                        val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.LTGRAY
                            style = Paint.Style.STROKE
                            strokeWidth = 1f
                            pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 6f), 0f)
                        }

                        for (row in 0 until 2) {
                            for (col in 0 until 2) {
                                val left = startX + col * (cardW + 30)
                                val top = startY + row * (cardH + 40)
                                val destRect = RectF(left, top, left + cardW, top + cardH)
                                canvas.drawBitmap(cardBitmap, null, destRect, null)

                                if (config.showCutLines) {
                                    canvas.drawRect(destRect, cutPaint)
                                }
                            }
                        }

                        // Top Sheet Title
                        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.DKGRAY
                            textSize = 14f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            textAlign = Paint.Align.CENTER
                        }
                        canvas.drawText("Rhythm & Focus — 4-Up Alarm Barcode Mission Stickers", pageSize.widthPt / 2f, 35f, headerPaint)
                    }
                    PdfPrintPageSize.STICKER_SINGLE, PdfPrintPageSize.A5, PdfPrintPageSize.A4 -> {
                        // Single centered card
                        val margin = 36f
                        val availableW = pageSize.widthPt - (margin * 2)
                        val availableH = pageSize.heightPt - (margin * 2)

                        val targetW: Float
                        val targetH: Float
                        if (availableW * 1.35f <= availableH) {
                            targetW = availableW
                            targetH = availableW * 1.35f
                        } else {
                            targetH = availableH
                            targetW = availableH / 1.35f
                        }

                        val left = (pageSize.widthPt - targetW) / 2f
                        val top = (pageSize.heightPt - targetH) / 2f

                        val cardBitmap = renderStyledCardBitmap(config, 900, 1215)
                        val destRect = RectF(left, top, left + targetW, top + targetH)
                        canvas.drawBitmap(cardBitmap, null, destRect, null)

                        if (config.showCutLines && pageSize != PdfPrintPageSize.STICKER_SINGLE) {
                            val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.LTGRAY
                                style = Paint.Style.STROKE
                                strokeWidth = 1f
                                pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
                            }
                            canvas.drawRect(destRect, cutPaint)
                        }
                    }
                }

                document.finishPage(page)

                val exportDir = File(context.cacheDir, "exported_pdf").apply { mkdirs() }
                val fileName = "rhythm_code_${System.currentTimeMillis()}.pdf"
                val file = File(exportDir, fileName)

                FileOutputStream(file).use { out ->
                    document.writeTo(out)
                }

                try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (_: Exception) {
                    Uri.fromFile(file)
                }
            } finally {
                try {
                    document.close()
                } catch (_: Throwable) {}
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Launch Android system share sheet for PNG or PDF.
     */
    fun shareFile(context: Context, uri: Uri, mimeType: String, title: String = "Share QR Mission Code") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Send PDF document to Android print manager for direct home printing.
     */
    fun printPdf(context: Context, pdfUri: Uri, jobName: String = "Rhythm Alarm QR Mission") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val printAdapter = object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder("$jobName.pdf")
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                        return
                    }
                    try {
                        context.contentResolver.openInputStream(pdfUri)?.use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }

            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
