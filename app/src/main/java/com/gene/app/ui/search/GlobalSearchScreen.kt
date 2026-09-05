package com.gene.app.ui.search

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
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gene.app.data.GeneDatabase
import com.gene.app.data.Interaction
import com.gene.app.data.LocalRelevanceSearch
import com.gene.app.data.Person
import com.gene.app.data.SessionWithPerson
import com.gene.app.ui.common.GeneBarAction
import com.gene.app.ui.common.GeneEmptyHint
import com.gene.app.ui.common.GeneGlassIconButton
import com.gene.app.ui.common.GeneGlassInputBar
import com.gene.app.ui.common.NotionSectionHeader
import com.gene.app.ui.common.geneHazeSource
import com.gene.app.ui.common.rememberGeneHazeState
import com.gene.app.ui.memory.MemoryCard
import com.gene.app.ui.theme.GeneFontFamily
import com.gene.app.ui.theme.GeneRadius
import com.gene.app.ui.theme.GeneSpace
import com.gene.app.ui.theme.rememberGeneColors

private val IconTileShape = RoundedCornerShape(GeneRadius.xs)

private data class MemoryHit(val memory: Interaction, val person: Person)

@Composable
fun GlobalSearchScreen(
    db: GeneDatabase,
    onBack: () -> Unit,
    onOpenPerson: (Long) -> Unit,
    onOpenChat: (personId: Long, sessionId: Long) -> Unit,
    onOpenMemory: (personId: Long, memoryId: Long) -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = rememberGeneColors(dark)
    val hazeState = rememberGeneHazeState()
    var query by remember { mutableStateOf("") }

    val people = remember { db.people(includeSelf = true) }
    val sessions = remember { db.allSessions() }
    val allMemories = remember(people) {
        people.flatMap { person ->
            db.interactions(person.id).map { MemoryHit(it, person) }
        }
    }

    val personHits = remember(people, query) {
        if (query.isBlank()) people.take(12)
        else people.filter {
            it.name.contains(query, true) || it.note.contains(query, true)
        }
    }
    val sessionHits = remember(sessions, query) {
        if (query.isBlank()) sessions.take(12)
        else sessions.filter {
            it.session.title.contains(query, true) ||
                it.personName.contains(query, true)
        }
    }
    val memoryHits = remember(allMemories, query) {
        if (query.isBlank()) allMemories.take(16)
        else {
            val byPerson = allMemories.groupBy { it.person.id }
            byPerson.values.flatMap { hits ->
                val memories = hits.map { it.memory }
                val person = hits.first().person
                LocalRelevanceSearch.relevant(memories, query, 8).map { MemoryHit(it, person) }
            }.sortedByDescending { it.memory.createdAt }.take(24)
        }
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
            item { NotionSectionHeader(title = "People", colors = colors) }
            if (personHits.isEmpty()) {
                item { GeneEmptyHint(colors = colors, message = "No people match this search.") }
            }
            items(personHits, key = { "person-${it.id}" }) { person ->
                SearchPersonRow(person, colors) { onOpenPerson(person.id) }
            }

            item {
                Spacer(Modifier.height(14.dp))
                NotionSectionHeader(title = "Chats", colors = colors)
            }
            if (sessionHits.isEmpty()) {
                item { GeneEmptyHint(colors = colors, message = "No chats match this search.") }
            }
            items(sessionHits, key = { "session-${it.session.id}" }) { item ->
                SearchSessionRow(item, colors) { onOpenChat(item.session.personId, item.session.id) }
            }

            item {
                Spacer(Modifier.height(14.dp))
                NotionSectionHeader(title = "Memories", colors = colors)
            }
            if (memoryHits.isEmpty()) {
                item { GeneEmptyHint(colors = colors, message = "No memories match this search.") }
            }
            items(memoryHits, key = { "memory-${it.memory.id}" }) { hit ->
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    Text(
                        hit.person.name,
                        color = colors.textTertiary,
                        fontSize = 12.sp,
                        fontFamily = GeneFontFamily,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                    MemoryCard(hit.memory, onClick = { onOpenMemory(hit.person.id, hit.memory.id) })
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
                        "People, chats, and memories",
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
                placeholder = "Search everything",
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

@Composable
private fun SearchPersonRow(person: Person, colors: com.gene.app.ui.theme.GeneColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(IconTileShape)
                .background(colors.pastel(person.id.toInt())),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = colors.textPrimary.copy(alpha = 0.65f),
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (person.isSelf) person.name.ifBlank { "You" } else person.name,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (person.note.isNotBlank()) {
                Text(
                    person.note,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = GeneFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchSessionRow(
    item: SessionWithPerson,
    colors: com.gene.app.ui.theme.GeneColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(IconTileShape)
                .background(colors.pastel(item.session.id.toInt())),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = colors.textPrimary.copy(alpha = 0.65f),
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.session.title.ifBlank { "Untitled chat" },
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.personName,
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontFamily = GeneFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
