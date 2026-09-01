package com.gene.app.ui.memory

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.gene.app.ui.common.asDate
import com.gene.app.ui.theme.GeneGray

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

@Composable
fun MemoryCard(
    memory: Interaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = memoryTypeLabel(memory.type)
    val icon = when (memory.type) {
        TYPE_AUDIO -> Icons.Outlined.GraphicEq
        TYPE_QUOTE -> Icons.Outlined.BookmarkBorder
        "recommendation" -> Icons.Outlined.Lightbulb
        "favorite" -> Icons.Outlined.FavoriteBorder
        else -> Icons.Outlined.TextSnippet
    }
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, label, tint = GeneGray, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(9.dp))
                Text(label, color = GeneGray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(memory.createdAt.asDate(), color = GeneGray, style = MaterialTheme.typography.bodyMedium)
            }
            if (memory.type == TYPE_AUDIO) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(18, 28, 14, 34, 22, 40, 26, 16, 31, 20, 36, 24, 13, 29).forEach { height ->
                        Box(Modifier.width(4.dp).height(height.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                    }
                }
                Text(
                    text = when {
                        memory.transcriptionStatus == TRANSCRIPTION_COMPLETE && !memory.transcript.isNullOrBlank() -> memory.transcript.take(120)
                        memory.transcriptionStatus == TRANSCRIPTION_PENDING -> "Audio saved · transcription queued"
                        memory.transcriptionStatus == TRANSCRIPTION_UNAVAILABLE -> "Audio saved · tap to open"
                        else -> "Audio saved · tap to open"
                    },
                    color = if (memory.transcriptionStatus == TRANSCRIPTION_COMPLETE) MaterialTheme.colorScheme.onSurface else GeneGray,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(memory.body.removePrefix("$label · ").trim(), style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
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
            if (leftHeight <= rightHeight) { left += memory; leftHeight += estimate } else { right += memory; rightHeight += estimate }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllMemoriesScreen(
    person: Person,
    db: GeneDatabase,
    onBack: () -> Unit,
    onOpenMemory: (Long) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("gene_settings", Context.MODE_PRIVATE) }
    val memoryLayoutKey = "memory_grid_${person.id}"
    var grid by remember(person.id) { mutableStateOf(prefs.getBoolean(memoryLayoutKey, true)) }
    val memories = db.interactions(person.id)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memories · ${person.name}", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = {
                        grid = !grid
                        prefs.edit().putBoolean(memoryLayoutKey, grid).apply()
                    }) {
                        Icon(if (grid) Icons.Outlined.ViewList else Icons.Outlined.ViewModule, if (grid) "List view" else "Grid view")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("All memories", style = MaterialTheme.typography.titleLarge)
                Text("${memories.size} saved moments", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
            }
            if (!grid) {
                items(memories, key = { it.id }) { memory -> MemoryCard(memory, onClick = { onOpenMemory(memory.id) }) }
            } else {
                items(memories.chunked(2), key = { it.firstOrNull()?.id ?: 0L }) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { memory -> MemoryCard(memory, onClick = { onOpenMemory(memory.id) }, modifier = Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
