package com.tom7.gene.ui.search

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom7.gene.data.GeneDatabase
import com.tom7.gene.data.LocalRelevanceSearch
import com.tom7.gene.data.Person
import com.tom7.gene.ui.common.GeneBarAction
import com.tom7.gene.ui.common.GeneEmptyHint
import com.tom7.gene.ui.common.GeneGlassIconButton
import com.tom7.gene.ui.common.GeneGlassInputBar
import com.tom7.gene.ui.common.NotionSectionHeader
import com.tom7.gene.ui.common.geneHazeSource
import com.tom7.gene.ui.common.rememberGeneHazeState
import com.tom7.gene.ui.memory.MemoryCard
import com.tom7.gene.ui.theme.GeneFontFamily
import com.tom7.gene.ui.theme.GeneRadius
import com.tom7.gene.ui.theme.GeneSpace
import com.tom7.gene.ui.theme.rememberGeneColors

private val IconTileShape = RoundedCornerShape(GeneRadius.xs)

@Composable
fun SearchScreen(
    person: Person,
    db: GeneDatabase,
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit,
    onOpenMemory: (Long) -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = rememberGeneColors(dark)
    val hazeState = rememberGeneHazeState()
    var query by remember { mutableStateOf("") }
    val memories = remember(person.id) { db.interactions(person.id) }
    val sessions = db.sessions(person.id)
    val memoryHits = if (query.isBlank()) memories.take(20) else LocalRelevanceSearch.relevant(memories, query, 20)
    val sessionHits = if (query.isBlank()) sessions else sessions.filter { session ->
        session.title.contains(query, true) || db.messages(session.id).any { it.body.contains(query, true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .geneHazeSource(hazeState)
                .statusBarsPadding()
                .padding(top = 168.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                NotionSectionHeader(title = "Chats", colors = colors)
            }
            if (sessionHits.isEmpty()) {
                item {
                    GeneEmptyHint(colors = colors, message = "No chats match this search.")
                }
            }
            items(sessionHits, key = { "session-${it.id}" }) { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenChat(session.id) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(IconTileShape)
                            .background(colors.pastel(session.id.toInt())),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Chat",
                            tint = colors.textPrimary.copy(alpha = 0.65f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        session.title,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = GeneFontFamily,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            item {
                Spacer(Modifier.height(14.dp))
                NotionSectionHeader(title = "Memories", colors = colors)
            }
            if (memoryHits.isEmpty()) {
                item {
                    GeneEmptyHint(colors = colors, message = "No memories match this search.")
                }
            }
            items(memoryHits, key = { "memory-${it.id}" }) { memory ->
                Box(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    MemoryCard(memory, onClick = { onOpenMemory(memory.id) })
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = GeneSpace.sm, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GeneGlassIconButton(
                    colors = colors,
                    hazeState = hazeState,
                    onClick = onBack,
                    contentDescription = "Back"
                ) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Search",
                        color = colors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GeneFontFamily
                    )
                    Text(
                        person.name,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = GeneFontFamily
                    )
                }
            }
            Spacer(Modifier.height(GeneSpace.sm))
            GeneGlassInputBar(
                colors = colors,
                hazeState = hazeState,
                value = query,
                onValueChange = { query = it },
                placeholder = "Search memories and chats",
                singleLine = true,
                maxLines = 1,
                leading = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(18.dp)
                    )
                },
                trailing = {
                    if (query.isNotBlank()) {
                        GeneBarAction(
                            onClick = { query = "" },
                            muted = true,
                            colors = colors,
                            contentDescription = "Clear search"
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}
