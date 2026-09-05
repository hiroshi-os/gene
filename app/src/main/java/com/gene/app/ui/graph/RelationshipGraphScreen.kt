package com.gene.app.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.gene.app.data.GeneDatabase
import com.gene.app.data.Person
import com.gene.app.data.Relationship
import com.gene.app.ui.common.GeneGlassIconButton
import com.gene.app.ui.common.geneHazeSource
import com.gene.app.ui.common.rememberGeneHazeState
import com.gene.app.ui.theme.GeneColors
import com.gene.app.ui.theme.GeneFontFamily
import com.gene.app.ui.theme.GeneRadius
import com.gene.app.ui.theme.GeneSpace
import com.gene.app.ui.theme.rememberGeneColors
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private val SuggestedLabels = listOf(
    "Friend", "Family", "Partner", "Colleague", "Roommate",
    "Mentor", "Neighbor", "Acquaintance", "Sibling", "Parent"
)

private data class GraphNode(
    val person: Person,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipGraphScreen(
    db: GeneDatabase,
    onBack: () -> Unit,
    onOpenPerson: (Long) -> Unit
) {
    val colors = rememberGeneColors(MaterialTheme.colorScheme.background.luminance() < 0.5f)
    val hazeState = rememberGeneHazeState()
    var tick by remember { mutableIntStateOf(0) }
    var frame by remember { mutableIntStateOf(0) }
    val people = remember(tick) { db.people(includeSelf = true) }
    val links = remember(tick) { db.relationships() }
    val textMeasurer = rememberTextMeasurer()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedPersonId by remember { mutableStateOf<Long?>(null) }
    var selectedLinkId by remember { mutableStateOf<Long?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    var editLink by remember { mutableStateOf<Relationship?>(null) }

    val nodes = remember { mutableStateMapOf<Long, GraphNode>() }

    LaunchedEffect(people.map { it.id }) {
        val cx = 0f
        val cy = 0f
        people.forEachIndexed { index, person ->
            if (nodes[person.id] == null) {
                val angle = (index.toFloat() / people.size.coerceAtLeast(1)) * Math.PI.toFloat() * 2f
                val radius = 180f + (index % 5) * 28f
                nodes[person.id] = GraphNode(
                    person = person,
                    x = cx + cos(angle) * radius + Random.nextFloat() * 20f,
                    y = cy + sin(angle) * radius + Random.nextFloat() * 20f
                )
            } else {
                nodes[person.id] = nodes[person.id]!!.copy(person = person)
            }
        }
        val alive = people.map { it.id }.toSet()
        nodes.keys.filter { it !in alive }.forEach { nodes.remove(it) }
    }

    // Soft force layout (Obsidian-like drift)
    LaunchedEffect(people, links, tick) {
        repeat(64) {
            val list = nodes.values.toList()
            for (n in list) {
                n.vx *= 0.86f
                n.vy *= 0.86f
            }
            for (i in list.indices) {
                for (j in i + 1 until list.size) {
                    val a = list[i]
                    val b = list[j]
                    var dx = b.x - a.x
                    var dy = b.y - a.y
                    var dist = hypot(dx, dy).coerceAtLeast(1f)
                    val push = 4200f / (dist * dist)
                    dx /= dist
                    dy /= dist
                    a.vx -= dx * push
                    a.vy -= dy * push
                    b.vx += dx * push
                    b.vy += dy * push
                }
            }
            for (link in links) {
                val a = nodes[link.personAId] ?: continue
                val b = nodes[link.personBId] ?: continue
                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                val pull = (dist - 160f) * 0.018f
                val ux = dx / dist
                val uy = dy / dist
                a.vx += ux * pull
                a.vy += uy * pull
                b.vx -= ux * pull
                b.vy -= uy * pull
            }
            for (n in list) {
                n.vx -= n.x * 0.0025f
                n.vy -= n.y * 0.0025f
                n.x += n.vx
                n.y += n.vy
            }
            frame++
            delay(16)
        }
    }

    fun screenToWorld(p: Offset, canvasSize: Size, pan: Offset, zoom: Float): Offset {
        val cx = canvasSize.width / 2f
        val cy = canvasSize.height / 2f
        return Offset(
            (p.x - cx - pan.x) / zoom,
            (p.y - cy - pan.y) / zoom
        )
    }

    val latestLinks by rememberUpdatedState(links)
    val latestOnOpenPerson by rememberUpdatedState(onOpenPerson)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .geneHazeSource(hazeState)
                // Keys must stay stable during pan/zoom — including scale/offset here
                // was cancelling the gesture after every tiny update (one-step feel).
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (oldScale * zoom).coerceIn(0.35f, 2.8f)
                        val center = Offset(size.width / 2f, size.height / 2f)
                        // Keep the world point under the pinch centroid stable while zooming.
                        val worldUnder = (centroid - center - offset) / oldScale
                        scale = newScale
                        offset = centroid - center - worldUnder * newScale + pan
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tap ->
                            val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                            val zoom = scale
                            val pan = offset
                            val world = screenToWorld(tap, canvasSize, pan, zoom)
                            val hitRadius = 36f / zoom.coerceAtLeast(0.5f)
                            val hitNode = nodes.values.minByOrNull { hypot(it.x - world.x, it.y - world.y) }
                            if (hitNode != null && hypot(hitNode.x - world.x, hitNode.y - world.y) < hitRadius) {
                                selectedPersonId = hitNode.person.id
                                selectedLinkId = null
                                return@detectTapGestures
                            }
                            val currentLinks = latestLinks
                            val hitLink = currentLinks.minByOrNull { link ->
                                val a = nodes[link.personAId] ?: return@minByOrNull Float.MAX_VALUE
                                val b = nodes[link.personBId] ?: return@minByOrNull Float.MAX_VALUE
                                distToSegment(world, Offset(a.x, a.y), Offset(b.x, b.y))
                            }
                            if (hitLink != null) {
                                val a = nodes[hitLink.personAId]
                                val b = nodes[hitLink.personBId]
                                if (a != null && b != null &&
                                    distToSegment(world, Offset(a.x, a.y), Offset(b.x, b.y)) < 22f / zoom.coerceAtLeast(0.5f)
                                ) {
                                    selectedLinkId = hitLink.id
                                    selectedPersonId = null
                                    editLink = hitLink
                                    return@detectTapGestures
                                }
                            }
                            selectedPersonId = null
                            selectedLinkId = null
                        },
                        onDoubleTap = { tap ->
                            val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                            val world = screenToWorld(tap, canvasSize, offset, scale)
                            val hitNode = nodes.values.minByOrNull { hypot(it.x - world.x, it.y - world.y) }
                            if (hitNode != null && hypot(hitNode.x - world.x, hitNode.y - world.y) < 40f) {
                                latestOnOpenPerson(hitNode.person.id)
                            }
                        }
                    )
                }
        ) {
            // Read frame so force steps invalidate the canvas
            @Suppress("UNUSED_EXPRESSION")
            frame
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Soft dots background
            val dotColor = colors.border.copy(alpha = 0.45f)
            var gy = 0f
            while (gy < size.height) {
                var gx = 0f
                while (gx < size.width) {
                    drawCircle(dotColor, radius = 1.2f, center = Offset(gx, gy))
                    gx += 28f
                }
                gy += 28f
            }

            fun toScreen(nx: Float, ny: Float) = Offset(cx + offset.x + nx * scale, cy + offset.y + ny * scale)

            for (link in links) {
                val a = nodes[link.personAId] ?: continue
                val b = nodes[link.personBId] ?: continue
                val p1 = toScreen(a.x, a.y)
                val p2 = toScreen(b.x, b.y)
                val selected = link.id == selectedLinkId
                drawLine(
                    color = if (selected) colors.accent else colors.textTertiary.copy(alpha = 0.55f),
                    start = p1,
                    end = p2,
                    strokeWidth = if (selected) 3.5f else 2f
                )
                val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                val labelLayout = textMeasurer.measure(
                    text = link.label,
                    style = TextStyle(
                        color = colors.textSecondary,
                        fontSize = (11f * scale.coerceIn(0.7f, 1.4f)).sp,
                        fontFamily = GeneFontFamily,
                        fontWeight = FontWeight.Medium,
                        background = colors.bg.copy(alpha = 0.82f)
                    )
                )
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(mid.x - labelLayout.size.width / 2f, mid.y - labelLayout.size.height / 2f)
                )
            }

            for (node in nodes.values) {
                val p = toScreen(node.x, node.y)
                val selected = node.person.id == selectedPersonId
                val fill = if (node.person.isSelf) colors.pillActive else colors.pastel(node.person.id.toInt())
                val r = (if (node.person.isSelf) 28f else 22f) * scale.coerceIn(0.6f, 1.6f)
                drawCircle(color = fill, radius = r, center = p)
                if (selected) {
                    drawCircle(
                        color = colors.accent,
                        radius = r + 5f,
                        center = p,
                        style = Stroke(width = 2.5f)
                    )
                } else {
                    drawCircle(
                        color = colors.border.copy(alpha = 0.7f),
                        radius = r,
                        center = p,
                        style = Stroke(width = 1.2f)
                    )
                }
                val nameLayout = textMeasurer.measure(
                    text = node.person.name.take(14),
                    style = TextStyle(
                        color = colors.textPrimary,
                        fontSize = (12f * scale.coerceIn(0.7f, 1.3f)).sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = GeneFontFamily,
                        background = colors.bg.copy(alpha = 0.75f)
                    )
                )
                drawText(
                    textLayoutResult = nameLayout,
                    topLeft = Offset(p.x - nameLayout.size.width / 2f, p.y + r + 6f)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = GeneSpace.sm, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GeneGlassIconButton(
                    colors = colors,
                    hazeState = hazeState,
                    onClick = onBack,
                    contentDescription = "Back"
                ) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Relationships",
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = GeneFontFamily
                    )
                    Text(
                        "${people.size} people · ${links.size} links",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontFamily = GeneFontFamily
                    )
                }
                GeneGlassIconButton(
                    colors = colors,
                    hazeState = hazeState,
                    onClick = { addOpen = true },
                    contentDescription = "Add relationship"
                ) {
                    Icon(Icons.Outlined.Add, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedPersonId?.let { id ->
                val person = people.find { it.id == id } ?: return@let
                val degree = links.count { it.personAId == id || it.personBId == id }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GeneRadius.md))
                        .background(colors.surface)
                        .border(0.5.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(GeneRadius.md))
                        .clickable { onOpenPerson(person.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (person.isSelf) colors.pillActive else colors.pastel(person.id.toInt())),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            person.name.take(1).uppercase(),
                            color = if (person.isSelf) colors.onPillActive else colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GeneFontFamily
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(person.name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontFamily = GeneFontFamily)
                        Text(
                            if (degree == 0) "No links yet · tap + to connect"
                            else "$degree ${if (degree == 1) "link" else "links"} · double-tap node to open",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontFamily = GeneFontFamily
                        )
                    }
                    Icon(Icons.Outlined.Hub, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                }
            }
            if (people.size < 2) {
                Text(
                    "Add at least two people to draw connections.",
                    color = colors.textTertiary,
                    fontSize = 13.sp,
                    fontFamily = GeneFontFamily,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    if (addOpen) {
        RelationshipEditorSheet(
            colors = colors,
            people = people,
            initial = null,
            onDismiss = { addOpen = false },
            onSave = { a, b, label ->
                db.upsertRelationship(a, b, label)
                tick++
                addOpen = false
            },
            onDelete = null
        )
    }

    editLink?.let { link ->
        RelationshipEditorSheet(
            colors = colors,
            people = people,
            initial = link,
            onDismiss = { editLink = null; selectedLinkId = null },
            onSave = { a, b, label ->
                if (link.personAId == minOf(a, b) && link.personBId == maxOf(a, b)) {
                    db.updateRelationshipLabel(link.id, label)
                } else {
                    db.deleteRelationship(link.id)
                    db.upsertRelationship(a, b, label)
                }
                tick++
                editLink = null
                selectedLinkId = null
            },
            onDelete = {
                db.deleteRelationship(link.id)
                tick++
                editLink = null
                selectedLinkId = null
            }
        )
    }
}

private fun distToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val apx = p.x - a.x
    val apy = p.y - a.y
    val abLen2 = abx * abx + aby * aby
    if (abLen2 < 1e-4f) return hypot(apx, apy)
    val t = ((apx * abx + apy * aby) / abLen2).coerceIn(0f, 1f)
    val cx = a.x + abx * t
    val cy = a.y + aby * t
    return hypot(p.x - cx, p.y - cy)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelationshipEditorSheet(
    colors: GeneColors,
    people: List<Person>,
    initial: Relationship?,
    onDismiss: () -> Unit,
    onSave: (personA: Long, personB: Long, label: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var personA by remember(initial) {
        mutableStateOf(initial?.personAId ?: people.firstOrNull()?.id)
    }
    var personB by remember(initial) {
        mutableStateOf(
            initial?.personBId
                ?: people.firstOrNull { it.id != personA }?.id
        )
    }
    var label by remember(initial) { mutableStateOf(initial?.label ?: "Friend") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (initial == null) "New connection" else "Edit connection",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneFontFamily
            )
            Text("Person A", color = colors.textSecondary, fontSize = 12.sp, fontFamily = GeneFontFamily)
            PersonChipRow(people = people, selectedId = personA, colors = colors) { personA = it }
            Text("Person B", color = colors.textSecondary, fontSize = 12.sp, fontFamily = GeneFontFamily)
            PersonChipRow(
                people = people.filter { it.id != personA },
                selectedId = personB,
                colors = colors
            ) { personB = it }

            Text("Relationship", color = colors.textSecondary, fontSize = 12.sp, fontFamily = GeneFontFamily)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SuggestedLabels) { suggestion ->
                    val selected = label.equals(suggestion, ignoreCase = true)
                    Text(
                        suggestion,
                        color = if (selected) colors.onPillActive else colors.textPrimary,
                        fontSize = 13.sp,
                        fontFamily = GeneFontFamily,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) colors.pillActive else colors.pill)
                            .clickable { label = suggestion }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                singleLine = true,
                label = { Text("Custom label") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GeneRadius.sm),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.border,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.accent,
                    focusedContainerColor = colors.pill,
                    unfocusedContainerColor = colors.pill,
                    focusedLabelColor = colors.textSecondary,
                    unfocusedLabelColor = colors.textTertiary
                )
            )

            val canSave = personA != null && personB != null && personA != personB && label.isNotBlank()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = colors.danger, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remove", color = colors.danger, fontFamily = GeneFontFamily)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                TextButton(
                    onClick = {
                        val a = personA ?: return@TextButton
                        val b = personB ?: return@TextButton
                        onSave(a, b, label.trim())
                    },
                    enabled = canSave
                ) {
                    Text(
                        "Save",
                        color = if (canSave) colors.accent else colors.textTertiary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GeneFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonChipRow(
    people: List<Person>,
    selectedId: Long?,
    colors: GeneColors,
    onSelect: (Long) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(people, key = { it.id }) { person ->
            val selected = person.id == selectedId
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) colors.pillActive else colors.pill)
                    .clickable { onSelect(person.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) colors.onPillActive.copy(alpha = 0.2f)
                            else colors.pastel(person.id.toInt())
                        )
                )
                Text(
                    person.name,
                    color = if (selected) colors.onPillActive else colors.textPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = GeneFontFamily
                )
            }
        }
    }
}
