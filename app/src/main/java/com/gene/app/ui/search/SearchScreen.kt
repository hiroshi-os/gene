package com.gene.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gene.app.data.GeneDatabase
import com.gene.app.data.LocalRelevanceSearch
import com.gene.app.data.Person
import com.gene.app.ui.memory.MemoryCard
import com.gene.app.ui.theme.GeneGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    person: Person,
    db: GeneDatabase,
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit,
    onOpenMemory: (Long) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val memories = remember(person.id) { db.interactions(person.id) }
    val sessions = db.sessions(person.id)
    val memoryHits = if (query.isBlank()) memories.take(20) else LocalRelevanceSearch.relevant(memories, query, 20)
    val sessionHits = if (query.isBlank()) sessions else sessions.filter { session -> session.title.contains(query, true) || db.messages(session.id).any { it.body.contains(query, true) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search ${person.name}", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }
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
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search memories and chats") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear search") }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(18.dp))
                Text("Chats", style = MaterialTheme.typography.titleMedium)
            }
            if (sessionHits.isEmpty()) item { Text("No chats match this search.", color = GeneGray, style = MaterialTheme.typography.bodyLarge) }
            items(sessionHits, key = { "session-${it.id}" }) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenChat(session.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.History, "Chat", tint = GeneGray)
                        Spacer(Modifier.width(12.dp))
                        Text(session.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                Text("Memories", style = MaterialTheme.typography.titleMedium)
            }
            if (memoryHits.isEmpty()) item { Text("No memories match this search.", color = GeneGray, style = MaterialTheme.typography.bodyLarge) }
            items(memoryHits, key = { "memory-${it.id}" }) { memory ->
                MemoryCard(memory, onClick = { onOpenMemory(memory.id) })
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
