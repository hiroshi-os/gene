package com.tom7.gene.ui.chat

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tom7.gene.data.MentionParser
import com.tom7.gene.data.Person
import kotlin.math.max
import kotlin.math.min

/** Mention chip annotations for the group-chat composer. */
object MentionField {
    const val TAG = "mention"

    fun annotate(
        text: String,
        members: List<Person>,
        chipColor: Color,
        chipBackground: Color
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        val ranges = mentionRanges(text, members)
        if (ranges.isEmpty()) return AnnotatedString(text)
        return buildAnnotatedString {
            var cursor = 0
            for (range in ranges.sortedBy { it.start }) {
                if (range.start > cursor) append(text.substring(cursor, range.start))
                pushStringAnnotation(TAG, range.personId.toString())
                // Color only — rounded chip fill is drawn separately (SpanStyle.background is sharp).
                withStyle(
                    SpanStyle(
                        color = chipColor,
                        fontWeight = FontWeight.SemiBold,
                        background = Color.Transparent
                    )
                ) {
                    append(text.substring(range.start, range.end))
                }
                pop()
                cursor = range.end
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
    }

    fun reconcile(
        previous: TextFieldValue,
        incoming: TextFieldValue,
        members: List<Person>,
        chipColor: Color,
        chipBackground: Color
    ): TextFieldValue {
        // Single-backspace deletes a whole @mention chip.
        if (
            previous.selection.collapsed &&
            incoming.selection.collapsed &&
            incoming.text.length < previous.text.length
        ) {
            val cursorBefore = previous.selection.start
            val mentions = previous.annotatedString.getStringAnnotations(TAG, 0, previous.text.length)
            val hit = mentions.firstOrNull { ann ->
                cursorBefore > ann.start && cursorBefore <= ann.end
            }
            if (hit != null) {
                val trimmed = previous.text.removeRange(hit.start, hit.end)
                    .replace(Regex(" {2,}"), " ")
                val clean = if (hit.start < trimmed.length && trimmed[hit.start].isWhitespace()) {
                    trimmed.removeRange(hit.start, hit.start + 1)
                } else trimmed
                val sel = hit.start.coerceIn(0, clean.length)
                return TextFieldValue(
                    annotatedString = annotate(clean, members, chipColor, chipBackground),
                    selection = TextRange(sel)
                )
            }
        }
        val sel = incoming.selection
        return TextFieldValue(
            annotatedString = annotate(incoming.text, members, chipColor, chipBackground),
            selection = TextRange(sel.start.coerceIn(0, incoming.text.length), sel.end.coerceIn(0, incoming.text.length)),
            composition = incoming.composition
        )
    }

    fun insertMention(
        current: TextFieldValue,
        person: Person,
        members: List<Person>,
        chipColor: Color,
        chipBackground: Color
    ): TextFieldValue {
        val text = current.text
        // Already mentioned — leave draft alone.
        if (MentionParser.mentionedPeople(text, listOf(person)).isNotEmpty()) {
            return current
        }
        val at = MentionParser.activeMentionQuery(text)?.let {
            text.lastIndexOf('@')
        } ?: -1
        val token = "@${person.name}"
        val next = if (at >= 0) {
            text.substring(0, at) + "$token "
        } else {
            val base = text.trimEnd()
            if (base.isEmpty()) "$token " else "$base $token "
        }
        val cursor = if (at >= 0) at + token.length + 1 else next.length
        return TextFieldValue(
            annotatedString = annotate(next, members, chipColor, chipBackground),
            selection = TextRange(cursor.coerceIn(0, next.length))
        )
    }

    private data class MentionRange(val start: Int, val end: Int, val personId: Long)

    private fun mentionRanges(text: String, members: List<Person>): List<MentionRange> {
        if (text.isBlank() || members.isEmpty()) return emptyList()
        val sorted = members
            .filter { !it.isSelf && it.name.isNotBlank() }
            .sortedByDescending { it.name.length }
        val lower = text.lowercase()
        val occupied = BooleanArray(text.length)
        val result = mutableListOf<MentionRange>()
        for (person in sorted) {
            val needle = "@${person.name.trim()}".lowercase()
            var from = 0
            while (true) {
                val at = lower.indexOf(needle, from)
                if (at < 0) break
                val end = at + needle.length
                val free = (at until end).none { occupied[it] }
                val beforeOk = at == 0 || !lower[at - 1].isLetterOrDigit()
                val afterOk = end >= lower.length || !lower[end].isLetterOrDigit()
                if (free && beforeOk && afterOk) {
                    for (i in at until end) occupied[i] = true
                    result.add(MentionRange(at, end, person.id))
                }
                from = at + 1
            }
        }
        return result
    }
}

/**
 * Draws pill-shaped (rounded) backgrounds behind [MentionField.TAG] annotations.
 * SpanStyle.background can't round — this is the real chip look.
 */
fun Modifier.drawMentionChips(
    annotated: AnnotatedString,
    layoutResult: TextLayoutResult?,
    background: Color,
    cornerRadius: Dp = 999.dp,
    horizontalPadding: Dp = 5.dp,
    verticalPadding: Dp = 2.dp
): Modifier = drawBehind {
    val layout = layoutResult ?: return@drawBehind
    if (annotated.isEmpty() || background.alpha <= 0f) return@drawBehind
    val mentions = annotated.getStringAnnotations(MentionField.TAG, 0, annotated.length)
    if (mentions.isEmpty()) return@drawBehind

    val hPad = horizontalPadding.toPx()
    val vPad = verticalPadding.toPx()

    for (ann in mentions) {
        if (ann.start >= ann.end) continue
        val startLine = layout.getLineForOffset(ann.start)
        val endLine = layout.getLineForOffset((ann.end - 1).coerceAtLeast(ann.start))
        for (line in startLine..endLine) {
            val lineStart = max(ann.start, layout.getLineStart(line))
            val rawEnd = layout.getLineEnd(line, visibleEnd = true)
            val lineEnd = min(ann.end, rawEnd)
            if (lineStart >= lineEnd) continue

            var left = Float.POSITIVE_INFINITY
            var top = Float.POSITIVE_INFINITY
            var right = Float.NEGATIVE_INFINITY
            var bottom = Float.NEGATIVE_INFINITY
            for (i in lineStart until lineEnd) {
                val box = layout.getBoundingBox(i)
                left = min(left, box.left)
                top = min(top, box.top)
                right = max(right, box.right)
                bottom = max(bottom, box.bottom)
            }
            if (!left.isFinite() || !right.isFinite()) continue

            val rect = Rect(
                left = left - hPad,
                top = top - vPad,
                right = right + hPad,
                bottom = bottom + vPad
            )
            val radiusPx = min(cornerRadius.toPx(), rect.height / 2f)
            val path = Path().apply {
                addRoundRect(RoundRect(rect, CornerRadius(radiusPx, radiusPx)))
            }
            drawPath(path, color = background, style = Fill)
        }
    }
}
