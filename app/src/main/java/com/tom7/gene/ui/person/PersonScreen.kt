package com.tom7.gene.ui.person

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom7.gene.data.GeneDatabase
import com.tom7.gene.data.Interaction
import com.tom7.gene.data.Person
import com.tom7.gene.data.RemoteInsightEngine
import com.tom7.gene.ui.chat.ChatPreviewCard
import com.tom7.gene.ui.chat.ChatPreviewMoreCard
import com.tom7.gene.ui.common.GeneGlassDropdownMenu
import com.tom7.gene.ui.common.GeneGlassIconButton
import com.tom7.gene.ui.common.MemojiAvatar
import com.tom7.gene.ui.common.MemojiConfig
import com.tom7.gene.ui.common.QuickActionChip
import com.tom7.gene.ui.common.asDate
import com.tom7.gene.ui.common.geneGlassMenuItemColors
import com.tom7.gene.ui.common.geneHazeSource
import com.tom7.gene.ui.common.rememberGeneHazeState
import com.tom7.gene.ui.memory.MasonryMemoryGallery
import com.tom7.gene.ui.memory.MemoryCard
import com.tom7.gene.ui.navigation.CHAT_TALK
import com.tom7.gene.ui.theme.GeneColors
import com.tom7.gene.ui.theme.GeneFontFamily
import com.tom7.gene.ui.theme.GeneRadius
import com.tom7.gene.ui.theme.GeneSpace
import com.tom7.gene.ui.theme.rememberGeneColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SheetShape = RoundedCornerShape(topStart = GeneRadius.lg, topEnd = GeneRadius.lg)
private val CardShape = RoundedCornerShape(GeneRadius.lg)
private val TraitShape = RoundedCornerShape(GeneRadius.sm)
private val FieldShape = RoundedCornerShape(GeneRadius.sm)
private val ButtonShape = RoundedCornerShape(GeneRadius.sm)

@Composable
private fun PersonSectionLabel(title: String, colors: GeneColors) {
    Text(
        text = title,
        color = colors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = GeneFontFamily
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    person: Person,
    memories: List<Interaction>,
    dark: Boolean,
    context: Context,
    openCapture: Boolean = false,
    onOpenChat: (Long?, String) -> Unit,
    onAsk: () -> Unit,
    onSearch: () -> Unit,
    onCalendar: () -> Unit,
    onOpenGraph: () -> Unit,
    onMemory: (Long) -> Unit,
    onAllMemories: () -> Unit,
    onAllChats: () -> Unit,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    db: GeneDatabase
) {
    val colors = rememberGeneColors(dark)
    val hazeState = rememberGeneHazeState()
    val prefs = remember { context.getSharedPreferences("gene_settings", Context.MODE_PRIVATE) }
    val memoryLayoutKey = "memory_grid_${person.id}"
    var memoryGrid by remember(person.id) { mutableStateOf(prefs.getBoolean(memoryLayoutKey, true)) }
    var showDelete by remember { mutableStateOf(false) }
    var personaMenuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(person.id, person.name) { mutableStateOf(person.name) }
    var personFavorite by remember(person.id, person.favorite) { mutableStateOf(person.favorite) }
    var summaryRefreshing by remember { mutableStateOf(false) }
    var displayedSummary by remember(person.id, person.summary) { mutableStateOf(person.summary) }
    val sessions = db.sessions(person.id)
    val selfRelationLabel = remember(person.id) {
        if (person.isSelf) null
        else {
            val self = db.selfPerson() ?: return@remember null
            db.relationshipBetween(self.id, person.id)?.label
        }
    }
    val quoteSet = remember(memories) { memories.filter { it.type == "quote" }.shuffled().take(3) }
    var quoteIndex by remember(person.id) { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val regenInteraction = remember { MutableInteractionSource() }
    val layoutInteraction = remember { MutableInteractionSource() }
    val seeAllChatsInteraction = remember { MutableInteractionSource() }
    val seeAllMemoriesInteraction = remember { MutableInteractionSource() }

    LaunchedEffect(quoteSet.size) {
        if (quoteSet.size > 1) {
            while (true) {
                delay(5500)
                quoteIndex = (quoteIndex + 1) % quoteSet.size
            }
        }
    }

    fun regenerateSummary() {
        if (memories.size < 10 || summaryRefreshing) return
        summaryRefreshing = true
        scope.launch {
            val generated = RemoteInsightEngine.generatedSummary(context, person, memories)
            db.saveSummary(person.id, generated, memories.size)
            displayedSummary = generated
            summaryRefreshing = false
            onChanged()
        }
    }

    LaunchedEffect(person.id, memories.size, person.summaryMemoryCount) {
        if (memories.size >= 10 && (person.summary == null || memories.size >= person.summaryMemoryCount + 10)) {
            regenerateSummary()
        }
    }

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
                .padding(top = 56.dp),
            contentPadding = PaddingValues(
                start = GeneSpace.lg,
                end = GeneSpace.lg,
                bottom = 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(GeneSpace.md)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = GeneSpace.sm, bottom = GeneSpace.xs),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!person.avatar.isNullOrBlank()) {
                        MemojiAvatar(
                            config = MemojiConfig.deserialize(person.avatar),
                            size = 72.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.pastel(person.id.toInt())),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = person.name.take(1).uppercase(),
                                color = colors.textPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = GeneFontFamily
                            )
                        }
                    }
                    Spacer(Modifier.height(GeneSpace.sm))
                    Text(
                        text = person.name,
                        color = colors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GeneFontFamily,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            person.isSelf -> "Your profile · notes about yourself"
                            person.note.isNotBlank() -> person.note
                            else -> ""
                        },
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = GeneFontFamily,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!selfRelationLabel.isNullOrBlank()) {
                        Spacer(Modifier.height(GeneSpace.sm))
                        Text(
                            text = selfRelationLabel,
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = GeneFontFamily,
                            modifier = Modifier
                                .clip(TraitShape)
                                .background(colors.pastel(person.id.toInt()))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickActionChip(
                            label = "Chat",
                            icon = Icons.Outlined.ChatBubbleOutline,
                            colors = colors,
                            onClick = { onOpenChat(null, CHAT_TALK) },
                            filled = true
                        )
                        QuickActionChip(
                            label = "Relations",
                            icon = Icons.Outlined.Hub,
                            colors = colors,
                            onClick = onOpenGraph
                        )
                        QuickActionChip(
                            label = "Calendar",
                            icon = Icons.Outlined.Event,
                            colors = colors,
                            onClick = onCalendar
                        )
                    }
                }
            }

            item {
                PersonSectionLabel("Summary", colors)
                Spacer(Modifier.height(GeneSpace.xs))
                val summaryReady = memories.size >= 10
                val summaryClick = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(colors.pastel(0).copy(alpha = if (dark) 0.55f else 0.95f))
                        .border(
                            width = if (summaryReady) 1.dp else 0.5.dp,
                            color = if (summaryReady) colors.accent.copy(alpha = 0.35f) else colors.border.copy(alpha = 0.6f),
                            shape = CardShape
                        )
                        .then(
                            if (summaryReady) {
                                Modifier.clickable(
                                    interactionSource = summaryClick,
                                    indication = null,
                                    onClick = onAsk
                                )
                            } else Modifier
                        )
                        .padding(GeneSpace.md),
                    verticalArrangement = Arrangement.spacedBy(GeneSpace.sm)
                ) {
                    if (summaryReady) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (summaryRefreshing) "Refreshing…" else if (person.isSelf) "About you" else "About ${person.name}",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = GeneFontFamily,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.pill)
                                    .clickable(
                                        interactionSource = regenInteraction,
                                        indication = null,
                                        enabled = !summaryRefreshing,
                                        onClick = { regenerateSummary() }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = "Regenerate summary",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = displayedSummary ?: "Building a clearer picture…",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontFamily = GeneFontFamily,
                            lineHeight = 21.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (person.isSelf) "Ask about yourself" else "Ask about ${person.name}",
                                color = colors.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = GeneFontFamily
                            )
                            Icon(
                                Icons.Outlined.ExpandMore,
                                contentDescription = "Expand summary",
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Add ${10 - memories.size} more memor${if (10 - memories.size == 1) "y" else "ies"} for a generated summary.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            fontFamily = GeneFontFamily
                        )
                    }
                }
            }

            if (quoteSet.isNotEmpty()) {
                item {
                    PersonSectionLabel("Quotes", colors)
                    Spacer(Modifier.height(GeneSpace.xs))
                    QuoteCarousel(quoteSet, quoteIndex, colors)
                }
            }

            if (sessions.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chats",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = GeneFontFamily,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(GeneRadius.xs))
                                .clickable(
                                    interactionSource = seeAllChatsInteraction,
                                    indication = null,
                                    onClick = onAllChats
                                )
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "See all",
                                color = colors.textTertiary,
                                fontSize = 13.sp,
                                fontFamily = GeneFontFamily
                            )
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = "See all chats",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(GeneSpace.xs))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(GeneSpace.sm),
                        contentPadding = PaddingValues(end = GeneSpace.xs)
                    ) {
                        items(sessions.take(5), key = { "preview-${it.id}" }) { session ->
                            ChatPreviewCard(session, onClick = { onOpenChat(session.id, CHAT_TALK) })
                        }
                        if (sessions.size > 5) {
                            item { ChatPreviewMoreCard(onClick = onAllChats) }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Memories",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = GeneFontFamily,
                        modifier = Modifier.weight(1f)
                    )
                    if (memories.size > 10) {
                        Text(
                            text = "Show all",
                            color = colors.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = GeneFontFamily,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = seeAllMemoriesInteraction,
                                    indication = null,
                                    onClick = onAllMemories
                                )
                                .padding(end = GeneSpace.xs)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.pill)
                            .clickable(
                                interactionSource = layoutInteraction,
                                indication = null,
                                onClick = {
                                    memoryGrid = !memoryGrid
                                    prefs.edit().putBoolean(memoryLayoutKey, memoryGrid).apply()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (memoryGrid) Icons.Outlined.ViewList else Icons.Outlined.ViewModule,
                            contentDescription = if (memoryGrid) "List view" else "Grid view",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (memories.isEmpty()) {
                item {
                    Text(
                        text = "Capture something below — notes show up here.",
                        color = colors.textTertiary,
                        fontSize = 14.sp,
                        fontFamily = GeneFontFamily,
                        modifier = Modifier.padding(top = GeneSpace.xs, bottom = GeneSpace.sm)
                    )
                }
            }
            if (memoryGrid && memories.isNotEmpty()) {
                item { MasonryMemoryGallery(memories.take(10), onMemory) }
            } else if (!memoryGrid) {
                items(memories.take(10), key = { it.id }) { memory ->
                    MemoryCard(memory, onClick = { onMemory(memory.id) })
                }
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
                Spacer(Modifier.weight(1f))
                GeneGlassIconButton(
                    colors = colors,
                    hazeState = hazeState,
                    onClick = onSearch,
                    contentDescription = "Search"
                ) {
                    Icon(Icons.Outlined.Search, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Box {
                    GeneGlassIconButton(
                        colors = colors,
                        hazeState = hazeState,
                        onClick = { personaMenuOpen = true },
                        contentDescription = "Person actions"
                    ) {
                        Icon(Icons.Outlined.MoreHoriz, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                    }
                    GeneGlassDropdownMenu(
                        expanded = personaMenuOpen,
                        onDismissRequest = { personaMenuOpen = false },
                        colors = colors,
                        hazeState = hazeState
                    ) {
                        val itemColors = geneGlassMenuItemColors(colors)
                        if (!person.isSelf) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (personFavorite) "Unfavorite" else "Favorite",
                                        fontFamily = GeneFontFamily
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (personFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                        null,
                                        tint = colors.textSecondary
                                    )
                                },
                                colors = itemColors,
                                onClick = {
                                    personFavorite = !personFavorite
                                    db.setPersonFavorite(person.id, personFavorite)
                                    personaMenuOpen = false
                                    onChanged()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (person.isSelf) "Rename yourself" else "Rename", fontFamily = GeneFontFamily) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null, tint = colors.textSecondary) },
                            colors = itemColors,
                            onClick = { personaMenuOpen = false; renameOpen = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Calendar", fontFamily = GeneFontFamily) },
                            leadingIcon = { Icon(Icons.Outlined.Event, null, tint = colors.textSecondary) },
                            colors = itemColors,
                            onClick = { personaMenuOpen = false; onCalendar() }
                        )
                        if (!person.isSelf) {
                            DropdownMenuItem(
                                text = { Text("Delete", fontFamily = GeneFontFamily) },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = colors.danger) },
                                colors = itemColors,
                                onClick = { personaMenuOpen = false; showDelete = true }
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = GeneSpace.xs)
        ) {
            MemoryComposer(
                person = person,
                db = db,
                onSaved = onChanged,
                hazeState = hazeState,
                expandTypes = openCapture
            )
        }
    }

    if (renameOpen) {
        ModalBottomSheet(
            onDismissRequest = { renameOpen = false },
            containerColor = colors.bg,
            shape = SheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = GeneSpace.lg)
                    .padding(bottom = GeneSpace.xl),
                verticalArrangement = Arrangement.spacedBy(GeneSpace.md)
            ) {
                Text(
                    if (person.isSelf) "Your name" else "Rename person",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneFontFamily
                )
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(FieldShape),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.border,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.accent,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(ButtonShape)
                        .background(if (renameText.isNotBlank()) colors.pillActive else colors.pill)
                        .clickable(enabled = renameText.isNotBlank()) {
                            if (renameText.isNotBlank()) {
                                db.updatePersonName(person.id, renameText)
                                renameOpen = false
                                onChanged()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Save name",
                        color = if (renameText.isNotBlank()) colors.onPillActive else colors.textTertiary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = GeneFontFamily
                    )
                }
            }
        }
    }
    if (showDelete) {
        DeletePersonSheet(
            person = person,
            db = db,
            onDismiss = { showDelete = false },
            onDeleted = {
                showDelete = false
                onChanged()
                onBack()
            },
            colors = colors
        )
    }
}

@Composable
fun QuoteCarousel(
    quotes: List<Interaction>,
    index: Int,
    colors: GeneColors = rememberGeneColors(false)
) {
    val quote = quotes[index.coerceIn(0, quotes.lastIndex)]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.pastel(quote.id.toInt()).copy(alpha = if (colors.dark) 0.55f else 0.95f))
            .border(0.5.dp, colors.border.copy(alpha = 0.55f), CardShape)
            .padding(GeneSpace.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GeneSpace.sm)
    ) {
        Icon(
            Icons.Outlined.FormatQuote,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "“${quote.body.removePrefix("Quote · ").trim().trim('"')}”",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            fontFamily = GeneFontFamily,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 22.sp
        )
        Text(
            text = "${index + 1} / ${quotes.size}  ·  ${quote.createdAt.asDate()}",
            color = colors.textTertiary,
            fontSize = 12.sp,
            fontFamily = GeneFontFamily
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletePersonSheet(
    person: Person,
    db: GeneDatabase,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    colors: GeneColors = rememberGeneColors(false)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        shape = SheetShape
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = GeneSpace.lg)
                .padding(bottom = GeneSpace.xl),
            verticalArrangement = Arrangement.spacedBy(GeneSpace.md)
        ) {
            Text(
                "Remove ${person.name}?",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneFontFamily
            )
            Text(
                "This removes the person and every saved memory from this device. This cannot be undone.",
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontFamily = GeneFontFamily,
                lineHeight = 20.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(ButtonShape)
                    .background(colors.danger)
                    .clickable {
                        if (db.deletePerson(person.id)) onDeleted()
                        else onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Remove person",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneFontFamily
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(ButtonShape)
                    .border(0.5.dp, colors.border, ButtonShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Keep person",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GeneFontFamily
                )
            }
        }
    }
}
