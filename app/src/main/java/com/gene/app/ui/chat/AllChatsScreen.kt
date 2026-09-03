package com.gene.app.ui.chat

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.gene.app.data.ChatSession
import com.gene.app.data.GeneDatabase
import com.gene.app.data.Person
import com.gene.app.ui.theme.GeneGray
import com.gene.app.ui.theme.IosBlue

@Composable
fun ChatPreviewCard(session: ChatSession, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(146.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (session.favorite) Icons.Outlined.Star else Icons.Outlined.History, "Chat", tint = IosBlue, modifier = Modifier.size(22.dp))
                Text(session.title, style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            Text(if (session.messageCount == 0) "New chat" else "${session.messageCount} messages", color = GeneGray, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

@Composable
fun ChatPreviewMoreCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(146.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = IosBlue),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Outlined.ChevronRight, "See all chats", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp))
            Text("Load more", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllChatsScreen(
    person: Person,
    db: GeneDatabase,
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit
) {
    var refresh by remember { mutableStateOf(0) }
    val sessions = remember(refresh) { db.sessions(person.id) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats · ${person.name}", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = IosBlue) } }
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
                Text("All chats", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text("Your saved conversations with ${person.name}.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(14.dp))
            }
            if (sessions.isEmpty()) item { Text("No saved chats yet.", color = GeneGray, style = MaterialTheme.typography.bodyLarge) }
            items(sessions, key = { it.id }) { session -> SessionRow(session, db, onClick = { onOpenChat(session.id) }, onChanged = { refresh++ }) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionRow(
    session: ChatSession,
    db: GeneDatabase,
    onClick: () -> Unit,
    onChanged: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(session.id) { mutableStateOf(session.title) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 17.dp, top = 15.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.History, "Chat session", tint = GeneGray, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (session.favorite) Icon(Icons.Outlined.Star, "Favorite", modifier = Modifier.size(18.dp))
                }
                Text(if (session.messageCount == 0) "New conversation" else "${session.messageCount} message${if (session.messageCount == 1) "" else "s"}", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Session actions") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(if (session.favorite) "Unfavorite" else "Favorite") }, leadingIcon = { Icon(if (session.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null) }, onClick = { db.setSessionFavorite(session.id, !session.favorite); menuOpen = false; onChanged() })
                    DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { menuOpen = false; renameOpen = true })
                    DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }, onClick = { db.deleteSession(session.id); menuOpen = false; onChanged() })
                }
            }
        }
    }
    if (renameOpen) {
        ModalBottomSheet(onDismissRequest = { renameOpen = false }, containerColor = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text("Rename chat", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { if (renameText.isNotBlank()) { db.updateSessionTitle(session.id, renameText); renameOpen = false; onChanged() } }, enabled = renameText.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save name") }
            }
        }
    }
}
