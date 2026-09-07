package com.tom7.gene.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom7.gene.data.ChatSession
import com.tom7.gene.data.GeneDatabase
import com.tom7.gene.data.Person
import com.tom7.gene.ui.common.GeneBarAction
import com.tom7.gene.ui.common.GeneEmptyHint
import com.tom7.gene.ui.common.GeneGlassDropdownMenu
import com.tom7.gene.ui.common.GeneGlassIconButton
import com.tom7.gene.ui.common.NotionSectionHeader
import com.tom7.gene.ui.common.geneGlassMenuItemColors
import com.tom7.gene.ui.common.geneHazeSource
import com.tom7.gene.ui.common.rememberGeneHazeState
import com.tom7.gene.ui.theme.GeneColors
import com.tom7.gene.ui.theme.GeneFontFamily
import com.tom7.gene.ui.theme.GeneRadius
import com.tom7.gene.ui.theme.GeneSpace
import com.tom7.gene.ui.theme.rememberGeneColors
import dev.chrisbanes.haze.HazeState

private val PreviewCardShape = RoundedCornerShape(GeneRadius.md)
private val IconTileShape = RoundedCornerShape(GeneRadius.xs)

@Composable
fun ChatPreviewCard(session: ChatSession, onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = rememberGeneColors(dark)
    val cover = colors.pastel(session.id.toInt())

    Column(
        modifier = Modifier
            .width(128.dp)
            .height(148.dp)
            .shadow(3.dp, PreviewCardShape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(PreviewCardShape)
            .background(colors.surface)
            .border(0.5.dp, colors.border.copy(alpha = 0.7f), PreviewCardShape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(cover),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (session.favorite) Icons.Outlined.Star else Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Chat",
                tint = colors.textPrimary.copy(alpha = 0.55f),
                modifier = Modifier.size(28.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                session.title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            Text(
                if (session.messageCount == 0) "New chat" else "${session.messageCount} messages",
                color = colors.textTertiary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ChatPreviewMoreCard(onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = rememberGeneColors(dark)

    Column(
        modifier = Modifier
            .width(128.dp)
            .height(148.dp)
            .shadow(3.dp, PreviewCardShape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(PreviewCardShape)
            .background(colors.accent)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = "See all chats",
            tint = colors.onPillActive,
            modifier = Modifier.size(22.dp)
        )
        Text(
            "Load more",
            color = colors.onPillActive,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneFontFamily
        )
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
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = rememberGeneColors(dark)
    val hazeState = rememberGeneHazeState()
    var refresh by remember { mutableStateOf(0) }
    val sessions = remember(refresh) { db.sessions(person.id) }

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
                .padding(top = 56.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 18.dp).padding(top = 8.dp, bottom = 6.dp)) {
                    Text(
                        "Chats",
                        color = colors.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Saved conversations with ${person.name}",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                NotionSectionHeader(title = "All chats", colors = colors)
            }
            if (sessions.isEmpty()) {
                item {
                    GeneEmptyHint(colors = colors, message = "No saved chats yet.")
                }
            }
            items(sessions, key = { it.id }) { session ->
                SessionRow(
                    session = session,
                    db = db,
                    colors = colors,
                    hazeState = hazeState,
                    onClick = { onOpenChat(session.id) },
                    onChanged = { refresh++ }
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = GeneSpace.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GeneGlassIconButton(
                colors = colors,
                hazeState = hazeState,
                onClick = onBack,
                contentDescription = "Back"
            ) {
                Icon(Icons.Outlined.ArrowBack, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
            }
            Text(
                person.name,
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneFontFamily,
                modifier = Modifier.padding(start = GeneSpace.sm),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionRow(
    session: ChatSession,
    db: GeneDatabase,
    onClick: () -> Unit,
    onChanged: () -> Unit,
    colors: GeneColors = rememberGeneColors(MaterialTheme.colorScheme.background.luminance() < 0.5f),
    hazeState: HazeState? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(session.id) { mutableStateOf(session.title) }
    val tile = colors.pastel(session.id.toInt())

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
                .background(tile),
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    session.title,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (session.favorite) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Outlined.Star, "Favorite", tint = colors.accent, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                if (session.messageCount == 0) "New conversation" else "${session.messageCount} message${if (session.messageCount == 1) "" else "s"}",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
        Box {
            GeneBarAction(
                onClick = { menuOpen = true },
                muted = true,
                colors = colors,
                contentDescription = "Session actions"
            ) {
                Icon(Icons.Outlined.MoreHoriz, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
            }
            GeneGlassDropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                colors = colors,
                hazeState = hazeState
            ) {
                val itemColors = geneGlassMenuItemColors(colors)
                DropdownMenuItem(
                    text = { Text(if (session.favorite) "Unfavorite" else "Favorite") },
                    leadingIcon = { Icon(if (session.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null) },
                    colors = itemColors,
                    onClick = { db.setSessionFavorite(session.id, !session.favorite); menuOpen = false; onChanged() }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    colors = itemColors,
                    onClick = { menuOpen = false; renameOpen = true }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                    colors = itemColors,
                    onClick = { db.deleteSession(session.id); menuOpen = false; onChanged() }
                )
            }
        }
    }

    if (renameOpen) {
        ModalBottomSheet(
            onDismissRequest = { renameOpen = false },
            containerColor = colors.surface
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Rename chat", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = colors.pill,
                        unfocusedContainerColor = colors.pill,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.accent
                    )
                )
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            db.updateSessionTitle(session.id, renameText)
                            renameOpen = false
                            onChanged()
                        }
                    },
                    enabled = renameText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onPillActive),
                    shape = RoundedCornerShape(GeneRadius.sm)
                ) {
                    Text("Save name")
                }
            }
        }
    }
}
