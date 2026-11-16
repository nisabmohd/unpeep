package app.keyhole.privacy.report

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.withTranslation
import app.keyhole.privacy.R
import app.keyhole.privacy.data.SavedReport
import app.keyhole.privacy.ui.components.decodeScaled
import app.keyhole.privacy.ui.report.ReportWords
import app.keyhole.privacy.ui.theme.DEFAULT_ACCENT_HUE
import app.keyhole.privacy.ui.theme.seedColorScheme
import java.io.OutputStream

/**
 * The report as a PDF (`PdfDocument`, no library), written to where the user chose through the system's "save as"
 * (SAF): A4 pages with the name, what was checked, what was found and why, what was marked as known, what couldn't
 * be checked, and the photos. The same words as the screen (ReportWords).
 */
class PdfReport(private val words: ReportWords, private val res: android.content.res.Resources, private val font: Typeface?) {
    private val colors = seedColorScheme(DEFAULT_ACCENT_HUE, dark = false)
    private val ink = colors.onSurface.toArgb()
    private val muted = colors.onSurfaceVariant.toArgb()
    private val alert = colors.error.toArgb()
    private val box = colors.surfaceContainerLow.toArgb()

    private fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(font ?: Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private val title = paint(TITLE, ink, bold = true)
    private val heading = paint(HEADING, ink, bold = true)
    private val body = paint(BODY, ink)
    private val small = paint(SMALL, muted)

    private lateinit var doc: PdfDocument
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var pageNumber = 0
    private var y = 0f

    fun write(report: SavedReport, out: OutputStream) {
        doc = PdfDocument()
        try {
            newPage()
            text(res.getString(R.string.report_pdf_title), small)
            text(words.name(report), title)
            text(words.whenLine(report), small)
            text(res.getString(R.string.two_parts, words.toLookAt(report), words.checksOf(report)), body, after = GAP)
            text(words.skippedLine(report), small, after = GAP)

            val found = report.findings.filter { it.finding.toLookAt }
            if (found.isNotEmpty()) {
                text(res.getString(R.string.found), heading)
                found.forEach { f ->
                    text(words.title(f.finding), body.withColor(if (f.finding.risk == app.keyhole.privacy.domain.Risk.LIKELY_CAMERA) alert else ink))
                    text(words.line(f.finding), small)
                    words.reasons(f.finding).forEach { text("• $it", small) }
                    if (f.finding.note.isNotBlank() && f.finding.source == app.keyhole.privacy.domain.FindingSource.CHECKLIST) text("“${f.finding.note}”", small)
                    y += LINE_GAP
                }
                y += GAP
            }
            val known = report.findings.filter { !it.finding.toLookAt }
            if (known.isNotEmpty()) {
                text(res.getString(R.string.marked_as_known), heading)
                known.forEach { text("• " + words.title(it.finding) + " · " + words.line(it.finding), small) }
                y += GAP
            }
            text(res.getString(R.string.couldnt_check), heading)
            report.gaps.forEach { text("• " + words.gap(it), small) }
            y += GAP
            text(res.getString(R.string.report_not_safe_note), body, after = GAP)

            if (report.photos.isNotEmpty()) {
                text(res.getString(R.string.photos), heading)
                photos(report.photos)
            }
            text(res.getString(R.string.report_pdf_footer), small, after = 0f)
            doc.finishPage(page)
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    private fun TextPaint.withColor(c: Int) = TextPaint(this).apply { color = c }

    private fun newPage() {
        if (pageNumber > 0) doc.finishPage(page)
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, pageNumber).create())
        canvas = page.canvas
        y = MARGIN
    }

    /** One paragraph, wrapped to the page; a new page when it doesn't fit. */
    private fun text(s: String, p: TextPaint, after: Float = LINE_GAP) {
        val width = (WIDTH - 2 * MARGIN).toInt()
        val layout = StaticLayout.Builder.obtain(s, 0, s.length, p, width).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        if (y + layout.height > HEIGHT - MARGIN) newPage()
        canvas.withTranslation(MARGIN, y) { layout.draw(this) }
        y += layout.height + after
    }

    /** Photos, three to a row, each scaled into its square. */
    private fun photos(paths: List<String>) {
        val size = (WIDTH - 2 * MARGIN - 2 * PHOTO_GAP) / PHOTOS_PER_ROW
        paths.chunked(PHOTOS_PER_ROW).forEach { row ->
            if (y + size > HEIGHT - MARGIN) newPage()
            row.forEachIndexed { i, path ->
                val left = MARGIN + i * (size + PHOTO_GAP)
                val rect = RectF(left, y, left + size, y + size)
                val bitmap = decodeScaled(path, PHOTO_PX)
                if (bitmap == null) {
                    canvas.drawRect(rect, Paint().apply { color = box })
                } else {
                    val side = minOf(bitmap.width, bitmap.height)
                    val src = android.graphics.Rect((bitmap.width - side) / 2, (bitmap.height - side) / 2, (bitmap.width + side) / 2, (bitmap.height + side) / 2)
                    canvas.drawBitmap(bitmap, src, rect, Paint(Paint.FILTER_BITMAP_FLAG))
                    bitmap.recycle()
                }
            }
            y += size + PHOTO_GAP
        }
        y += GAP
    }

    private companion object {
        /** A4 in PostScript points. */
        const val WIDTH = 595
        const val HEIGHT = 842
        const val MARGIN = 48f
        const val TITLE = 20f
        const val HEADING = 14f
        const val BODY = 11f
        const val SMALL = 9.5f
        const val LINE_GAP = 4f
        const val GAP = 14f
        const val PHOTOS_PER_ROW = 3
        const val PHOTO_GAP = 8f
        const val PHOTO_PX = 400
    }
}
