package com.gene.app.ui.memory

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gene.app.data.GeneDatabase
import com.gene.app.data.Interaction
import com.gene.app.data.Person
import com.gene.app.data.TRANSCRIPTION_COMPLETE
import com.gene.app.data.TRANSCRIPTION_PENDING
import com.gene.app.data.TRANSCRIPTION_UNAVAILABLE
import com.gene.app.data.TYPE_AUDIO
import com.gene.app.data.TYPE_PATTERN
import com.gene.app.data.TYPE_QUOTE
import com.gene.app.data.TYPE_SIGNAL
import com.gene.app.ui.common.GeneGlassIconButton
import com.gene.app.ui.common.asDate
import com.gene.app.ui.common.geneCardSurface
import com.gene.app.ui.common.geneHazeSource
import com.gene.app.ui.common.rememberGeneHazeState
import com.gene.app.ui.theme.GeneFontFamily
import com.gene.app.ui.theme.GeneRadius
import com.gene.app.ui.theme.GeneSpace
import com.gene.app.ui.theme.NotionDarkBg
import com.gene.app.ui.theme.rememberGeneColors

private val CardShape = RoundedCornerShape(GeneRadius.sm)
private val PillShape = RoundedCornerShape(GeneRadius.xs)

fun memoryTypeLabel(type: String): String = when (type) {
    TYPE_AUDIO -> "Voice"
    TYPE_QUOTE -> "Quote"
    TYPE_SIGNAL -> "Signal"
    TYPE_PATTERN -> "Pattern"
    "recommendation" -> "Recommendation"
    "favorite" -> "Favorite"
    "feeling" -> "Feeling"
    "question" -> "Open question"
    else -> "Note"
}

private fun memoryTypePastelIndex(type: String): Int = when (type) {
    TYPE_AUDIO -> 3
    TYPE_QUOTE -> 0
    TYPE_SIGNAL -> 5
    TYPE_PATTERN -> 2
    "recommendation" -> 4
    "favorite" -> 1
    "feeling" -> 5
    "question" -> 2
    else -> 6
}

@Composable
fun MemoryCard(
    memory: Interaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = MaterialTheme.colorScheme.background == NotionDarkBg
    val colors = rememberGeneColors(dark)
    val label = memoryTypeLabel(memory.type)
    val icon = when (memory.type) {
        TYPE_AUDIO -> Icons.Outlined.GraphicEq
        TYPE_QUOTE -> Icons.Outlined.BookmarkBorder
        "recommendation" -> Icons.Outlined.Lightbulb
        "favorite" -> Icons.Outlined.FavoriteBorder
        else -> Icons.Outlined.TextSnippet
    }
    val pastel = colors.pastel(memoryTypePastelIndex(memory.type))
    val interaction = remember { MutableInteractionSource() }
    val bodyText = when {
        memory.type == TYPE_AUDIO && memory.transcriptionStatus == TRANSCRIPTION_COMPLETE && !memory.transcript.isNullOrBlank() ->
            memory.transcript.take(120)
        memory.type == TYPE_AUDIO && memory.transcriptionStatus == TRANSCRIPTION_PENDING ->
            "Audio saved · transcription queued"
        memory.type == TYPE_AUDIO && memory.transcriptionStatus == TRANSCRIPTION_UNAVAILABLE ->
            "Audio saved · tap to open"
        memory.type == TYPE_AUDIO ->
            "Audio saved · tap to open"
        else -> memory.body.removePrefix("$label · ").trim()
    }
    val bodyMuted = memory.type == TYPE_AUDIO && memory.transcriptionStatus != TRANSCRIPTION_COMPLETE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .geneCardSurface(colors, CardShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(pastel)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(14.dp))
                Text(
                    text = label,
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GeneFontFamily
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = memory.createdAt.asDate(),
                color = colors.textTertiary,
                fontSize = 12.sp,
                fontFamily = GeneFontFamily
            )
        }

        if (memory.type == TYPE_AUDIO) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(14, 22, 12, 28, 18, 32, 20, 14, 26, 16, 30, 18, 12, 24).forEach { bar ->
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(bar.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.accent.copy(alpha = 0.45f))
                    )
                }
            }
        }

        Text(
            text = bodyText,
            color = if (bodyMuted) colors.textSecondary else colors.textPrimary,
            fontSize = 15.sp,
            fontFamily = GeneFontFamily,
            lineHeight = 21.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MasonryMemoryGallery(memories: List<Interaction>, onMemory: (Long) -> Unit) {
    val columns = remember(memories) {
        val left = mutableListOf<Interaction>()
        val right = mutableListOf<Interaction>()
        var leftHeight = 0
        var rightHeight = 0
        memories.forEach { memory ->
            val estimate = 90 + (memory.body.length / 34).coerceIn(0, 5) * 24 + if (memory.type == TYPE_AUDIO) 50 else 0
            if (leftHeight <= rightHeight) {
                left += memory
                leftHeight += estimate
            } else {
                right += memory
                rightHeight += estimate
            }
        }
        left to right
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            columns.first.forEach { memory -> MemoryCard(memory, onClick = { onMemory(memory.id) }) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            columns.second.forEach { memory -> MemoryCard(memory, onClick = { onMemory(memory.id) }) }
        }
    }
}

@Composable
fun AllMemoriesScreen(
    person: Person,
    db: GeneDatabase,
    onBack: () -> Unit,
    onOpenMemory: (Long) -> Unit
) {
    val dark = MaterialTheme.colorScheme.background == NotionDarkBg
    val colors = rememberGeneColors(dark)
    val hazeState = rememberGeneHazeState()
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("gene_settings", Context.MODE_PRIVATE) }
    val memoryLayoutKey = "memory_grid_${person.id}"
    var grid by remember(person.id) { mutableStateOf(prefs.getBoolean(memoryLayoutKey, true)) }
    val memories = db.interactions(person.id)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .geneHazeSource(hazeState)
                .statusBarsPadding()
                .padding(top = 56.dp)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "All memories",
                    color = colors.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GeneFontFamily,
                    letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${memories.size} saved moments",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = GeneFontFamily
                )
                Spacer(Modifier.height(12.dp))
            }
            if (memories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No memories yet",
                            color = colors.textTertiary,
                            fontSize = 15.sp,
                            fontFamily = GeneFontFamily
                        )
                    }
                }
            } else if (!grid) {
                items(memories, key = { it.id }) { memory ->
                    MemoryCard(memory, onClick = { onOpenMemory(memory.id) })
                }
            } else {
                items(memories.chunked(2), key = { it.firstOrNull()?.id ?: 0L }) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { memory ->
                            MemoryCard(memory, onClick = { onOpenMemory(memory.id) }, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            item { Spacer(Modifier.height(28.dp)) }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = GeneSpace.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GeneSpace.xs)
        ) {
            GeneGlassIconButton(
                colors = colors,
                hazeState = hazeState,
                onClick = onBack,
                contentDescription = "Back"
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Memories",
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    fontFamily = GeneFontFamily
                )
            }
            GeneGlassIconButton(
                colors = colors,
                hazeState = hazeState,
                onClick = {
                    grid = !grid
                    prefs.edit().putBoolean(memoryLayoutKey, grid).apply()
                },
                contentDescription = if (grid) "List view" else "Grid view"
            ) {
                Icon(
                    if (grid) Icons.Outlined.ViewList else Icons.Outlined.ViewModule,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
