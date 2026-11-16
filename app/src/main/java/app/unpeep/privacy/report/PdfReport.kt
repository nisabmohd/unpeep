package app.unpeep.privacy.report

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import app.unpeep.privacy.R
import app.unpeep.privacy.data.SavedReport
import app.unpeep.privacy.domain.Gap
import app.unpeep.privacy.domain.KnownAs
import app.unpeep.privacy.ui.components.decodeScaled
import app.unpeep.privacy.ui.label
import app.unpeep.privacy.ui.report.ReportWords
import app.unpeep.privacy.ui.theme.Brand
import app.unpeep.privacy.ui.theme.DEFAULT_ACCENT_HUE
import app.unpeep.privacy.ui.theme.seedColorScheme
import app.unpeep.privacy.ui.theme.warningColors
import java.io.OutputStream

/**
 * The report as a PDF (`PdfDocument`, no library), as 10g draws it: the mark, "Unpeep" and "Page 1 of 2" on every
 * page; the name, date and place, its two tags; then the same sections in the same order as the screen (Checks ·
 * Found · Photos · Marked as known · Couldn't check), each a label over two-column rows with thin lines between;
 * the honesty line at the end. Written where the user chose (SAF). The same words as the screen (ReportWords).
 */
class PdfReport(private val words: ReportWords, private val res: android.content.res.Resources, private val font: Typeface?) {
    private val colors = seedColorScheme(DEFAULT_ACCENT_HUE, dark = false)
    private val ink = colors.onSurface.toArgb()
    private val muted = colors.onSurfaceVariant.toArgb()
    private val line = colors.outlineVariant.toArgb()
    private val greyTag = colors.surfaceContainerHigh.toArgb()
    private val amber = warningColors(dark = false)
    private val accent = colors.primaryContainer.toArgb()

    private fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(font ?: Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private val brandName = paint(BRAND, ink, bold = true)
    private val pageText = paint(SMALL, muted)
    private val title = paint(TITLE, ink, bold = true)
    private val sub = paint(BODY, muted)
    private val label = paint(LABEL, muted)
    private val left = paint(BODY, ink, bold = true)
    private val right = paint(BODY, muted)
    private val tag = paint(SMALL, ink, bold = true)
    private val foot = paint(SMALL, muted)

    private lateinit var doc: PdfDocument
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var pageNumber = 0
    private var pages = 1
    private var y = 0f

    fun write(report: SavedReport, out: OutputStream) {
        // Twice: the first pass only counts pages, so every page can say "Page 1 of 2".
        pages = draw(report, null)
        draw(report, out)
    }

    /** Draws the whole report; writes it to [out] when given. Returns how many pages it took. */
    private fun draw(report: SavedReport, out: OutputStream?): Int {
        doc = PdfDocument()
        // A fresh document each pass: the last pass's page belongs to a closed document.
        page = null
        canvas = null
        pageNumber = 0
        try {
            newPage()
            text(words.name(report), title, after = SMALL_GAP)
            text(res.getString(R.string.two_parts, words.whenLine(report), res.getString(ReportWords.placeLower(report.place))), sub, after = GAP)
            tags(report)

            // Checks: the ones that ran on one line, each skipped one on its own.
            section(res.getString(R.string.pdf_checks))
            val ran = report.planned.filter { it in report.ran }
            if (ran.isNotEmpty()) row(ran.joinToString(" · ") { res.getString(it.label) }, res.getString(R.string.pdf_ran))
            report.planned.filter { it !in report.ran }.forEach { row(res.getString(it.label), res.getString(R.string.pdf_skipped), notChecked = true) }

            val found = report.findings.filter { it.finding.toLookAt }
            if (found.isNotEmpty()) {
                section(res.getString(R.string.found))
                found.forEach { f ->
                    val name = listOfNotNull(words.title(f.finding), words.source(f.finding), f.finding.detail.takeIf { it.isNotBlank() && f.finding.source == app.unpeep.privacy.domain.FindingSource.WIFI })
                    val why = (listOf(words.line(f.finding).substringAfter(" · ")) + words.reasons(f.finding).map { it.replaceFirstChar { c -> c.lowercase() } }).distinct()
                    row(name.joinToString(" · "), why.joinToString(" · ").replaceFirstChar { it.uppercase() })
                }
            }

            if (report.photos.isNotEmpty()) {
                section(res.getString(R.string.photos), divider = false)
                photos(report.photos)
            }

            val known = report.findings.filter { !it.finding.toLookAt }
            if (known.isNotEmpty()) {
                section(res.getString(R.string.marked_as_known))
                known.forEach { f ->
                    val how = when (f.finding.knownAs) {
                        KnownAs.MINE -> res.getString(R.string.its_mine)
                        else -> res.getString(R.string.part_of_the_room)
                    }
                    row(words.title(f.finding), how)
                }
            }

            section(res.getString(R.string.couldnt_check))
            report.gaps.forEach { g ->
                if (g is Gap.NotRun) row(res.getString(g.check.label), res.getString(R.string.pdf_skipped), notChecked = true)
                else row(words.gap(g), res.getString(R.string.pdf_unseen), notChecked = true)
            }

            y += GAP
            divider()
            y += SMALL_GAP
            text(res.getString(R.string.report_not_safe_note) + " " + res.getString(R.string.report_pdf_footer), foot, after = 0f)
            page?.let { doc.finishPage(it) }
            out?.let { doc.writeTo(it) }
            return pageNumber
        } finally {
            doc.close()
        }
    }

    private fun newPage() {
        page?.let { doc.finishPage(it) }
        pageNumber++
        val p = doc.startPage(PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, pageNumber).create())
        page = p
        canvas = p.canvas
        y = MARGIN
        header()
    }

    /** The mark on its brand tile, "Unpeep", and the page number at the right (10g). */
    private fun header() {
        val c = canvas ?: return
        val tile = RectF(MARGIN, y, MARGIN + MARK, y + MARK)
        c.drawRoundRect(tile, MARK_CORNER, MARK_CORNER, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Brand.toArgb() })
        runCatching {
            val mark = androidx.core.content.res.ResourcesCompat.getDrawable(res, R.drawable.ic_launcher_foreground, null)
            // The launcher foreground keeps its safe zone: draw it 1.5× and centred, as MarkTile does.
            val grow = MARK * (MARK_SCALE - 1f) / 2f
            mark?.setBounds((tile.left - grow).toInt(), (tile.top - grow).toInt(), (tile.right + grow).toInt(), (tile.bottom + grow).toInt())
            c.withClip(tile) { mark?.draw(this) }
        }
        c.drawText(res.getString(R.string.app_name), MARGIN + MARK + SMALL_GAP * 1.5f, y + MARK / 2f + BRAND / 2.8f, brandName)
        val p = res.getString(R.string.pdf_page, pageNumber, pages)
        c.drawText(p, WIDTH - MARGIN - pageText.measureText(p), y + MARK / 2f + SMALL / 2.8f, pageText)
        y += MARK + HEADER_GAP
    }

    /** The two tags under the title: what to look at (amber) or nothing found (accent), and how many checks ran. */
    private fun tags(report: SavedReport) {
        val c = canvas ?: return
        var x = MARGIN
        val look = report.toLookAt > 0
        listOf(
            words.toLookAt(report) to (if (look) amber.container.toArgb() else accent),
            words.checksOf(report) to greyTag,
        ).forEach { (text, fill) ->
            val w = tag.measureText(text) + TAG_PAD * 2
            c.drawRoundRect(RectF(x, y, x + w, y + TAG_H), TAG_H / 2, TAG_H / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
            c.drawText(text, x + TAG_PAD, y + TAG_H / 2 + SMALL / 2.8f, tag)
            x += w + SMALL_GAP
        }
        y += TAG_H + GAP
    }

    /** A section's label, and the thin line under it. */
    private fun section(name: String, divider: Boolean = true) {
        if (y + SECTION_MIN > HEIGHT - MARGIN) newPage()
        y += GAP
        text(name, label, after = SMALL_GAP)
        if (divider) divider()
    }

    private fun divider() {
        canvas?.drawLine(MARGIN, y, WIDTH - MARGIN, y, Paint().apply { color = line; strokeWidth = HAIRLINE })
    }

    /**
     * One row: [l] in bold on the left, [r] muted on the right, each wrapped in its half; a line under it. Not checked
     * ([notChecked]) gets the grey dot of "not checked" (review 2).
     */
    private fun row(l: String, r: String, notChecked: Boolean = false) {
        val inner = WIDTH - 2 * MARGIN
        val dot = if (notChecked) DOT + SMALL_GAP else 0f
        // The right side takes what it needs, up to just over half; the left gets the rest (10g).
        val rightW = minOf(right.measureText(r) + 1f, inner * RIGHT_MAX).toInt().coerceAtLeast(1)
        val leftW = (inner - rightW - GAP - dot).toInt()
        val ll = StaticLayout.Builder.obtain(l, 0, l.length, left, leftW).build()
        val rl = StaticLayout.Builder.obtain(r, 0, r.length, right, rightW).setAlignment(Layout.Alignment.ALIGN_OPPOSITE).build()
        val h = maxOf(ll.height, rl.height) + ROW_PAD * 2
        if (y + h > HEIGHT - MARGIN) newPage()
        val c = canvas ?: return
        if (notChecked) c.drawCircle(MARGIN + DOT / 2, y + ROW_PAD + BODY * 0.6f, DOT / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted })
        c.withTranslation(MARGIN + dot, y + ROW_PAD) { ll.draw(this) }
        c.withTranslation(WIDTH - MARGIN - rightW, y + ROW_PAD) { rl.draw(this) }
        y += h
        divider()
    }

    /** One paragraph, wrapped to the page; a new page when it doesn't fit. */
    private fun text(s: String, p: TextPaint, after: Float = SMALL_GAP) {
        val width = (WIDTH - 2 * MARGIN).toInt()
        val layout = StaticLayout.Builder.obtain(s, 0, s.length, p, width).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        if (y + layout.height > HEIGHT - MARGIN) newPage()
        canvas?.withTranslation(MARGIN, y) { layout.draw(this) }
        y += layout.height + after
    }

    /** Photos as rounded thumbnails, four to a row, each the centre of its picture. */
    private fun photos(paths: List<String>) {
        paths.chunked(PHOTOS_PER_ROW).forEach { rowPaths ->
            if (y + PHOTO > HEIGHT - MARGIN) newPage()
            val c = canvas ?: return
            rowPaths.forEachIndexed { i, path ->
                val l = MARGIN + i * (PHOTO + PHOTO_GAP)
                val rect = RectF(l, y, l + PHOTO, y + PHOTO)
                val clip = android.graphics.Path().apply { addRoundRect(rect, PHOTO_CORNER, PHOTO_CORNER, android.graphics.Path.Direction.CW) }
                c.withClip(clip) {
                    val bitmap = decodeScaled(path, PHOTO_PX)
                    if (bitmap == null) {
                        drawRect(rect, Paint().apply { color = greyTag })
                    } else {
                        val side = minOf(bitmap.width, bitmap.height)
                        val src = android.graphics.Rect((bitmap.width - side) / 2, (bitmap.height - side) / 2, (bitmap.width + side) / 2, (bitmap.height + side) / 2)
                        drawBitmap(bitmap, src, rect, Paint(Paint.FILTER_BITMAP_FLAG))
                        bitmap.recycle()
                    }
                }
            }
            y += PHOTO + PHOTO_GAP
        }
        y += SMALL_GAP
    }

    private companion object {
        /** A4 in PostScript points. */
        const val WIDTH = 595
        const val HEIGHT = 842
        const val MARGIN = 48f
        const val MARK = 24f
        const val MARK_CORNER = 6f
        const val MARK_SCALE = 1.5f
        const val BRAND = 12f
        const val TITLE = 24f
        const val BODY = 10.5f
        const val SMALL = 9f
        const val HEADER_GAP = 28f
        const val SMALL_GAP = 6f
        const val GAP = 14f
        const val TAG_H = 18f
        const val TAG_PAD = 8f
        const val SECTION_MIN = 80f
        const val HAIRLINE = 0.6f
        const val RIGHT_MAX = 0.55f
        const val LABEL = 10.5f
        const val ROW_PAD = 9f
        const val DOT = 5f
        const val PHOTOS_PER_ROW = 4
        const val PHOTO = 92f
        const val PHOTO_GAP = 8f
        const val PHOTO_CORNER = 10f
        const val PHOTO_PX = 400
    }
}
