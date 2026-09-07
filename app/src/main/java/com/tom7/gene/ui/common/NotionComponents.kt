package com.tom7.gene.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom7.gene.ui.chat.drawMentionChips
import com.tom7.gene.ui.theme.GeneColors
import com.tom7.gene.ui.theme.GeneFontFamily
import com.tom7.gene.ui.theme.GeneRadius
import com.tom7.gene.ui.theme.GeneSpace
import dev.chrisbanes.haze.HazeState

private val PillShape = RoundedCornerShape(GeneRadius.pill)
private val CardShape = RoundedCornerShape(GeneRadius.lg)
private val IconTileShape = RoundedCornerShape(GeneRadius.sm)
private val DockShape = RoundedCornerShape(26.dp)
private val GlassBarShape = RoundedCornerShape(22.dp)
private val ControlSize = 36.dp
private val DockControlSize = 40.dp
private val DockCaptureHeight = 40.dp

@Composable
fun NotionTopChrome(
    colors: GeneColors,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSettings: () -> Unit,
    onNewPerson: () -> Unit,
    onSelf: () -> Unit,
    selfName: String = "You",
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GeneSpace.sm, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GeneGlassIconButton(
            colors = colors,
            hazeState = hazeState,
            onClick = onSettings,
            contentDescription = "Settings",
            size = ControlSize
        ) {
            Icon(Icons.Outlined.Settings, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        }

        NotionTabPill(
            label = "People",
            icon = Icons.Outlined.People,
            selected = selectedTab == 0,
            colors = colors,
            onClick = { onTabSelected(0) },
            hazeState = hazeState
        )
        NotionTabPill(
            label = "Chats",
            icon = Icons.Outlined.ChatBubbleOutline,
            selected = selectedTab == 1,
            colors = colors,
            onClick = { onTabSelected(1) },
            hazeState = hazeState
        )

        GeneGlassIconButton(
            colors = colors,
            hazeState = hazeState,
            onClick = onNewPerson,
            contentDescription = "New person",
            size = ControlSize
        ) {
            Icon(Icons.Outlined.Add, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.weight(1f))

        GeneGlassIconButton(
            colors = colors,
            hazeState = hazeState,
            onClick = onSelf,
            contentDescription = "Your profile",
            size = ControlSize
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colors.pastel(0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selfName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Y",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneFontFamily
                )
            }
        }
    }
}

@Composable
fun NotionTabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    colors: GeneColors,
    onClick: () -> Unit,
    hazeState: HazeState? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val base = if (selected) {
        Modifier
            .clip(PillShape)
            .background(colors.pillActive)
    } else if (hazeState != null) {
        Modifier.geneGlass(hazeState, colors, GlassThickness.Thin, PillShape)
    } else {
        Modifier.geneFrostedSurface(colors, PillShape, elevation = 2.dp)
    }
    Row(
        modifier = base
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) colors.onPillActive else colors.textSecondary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = if (selected) colors.onPillActive else colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneFontFamily
        )
    }
}

/** @deprecated Prefer [GeneGlassIconButton] */
@Composable
fun NotionCircleButton(
    colors: GeneColors,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    GeneGlassIconButton(
        colors = colors,
        onClick = onClick,
        contentDescription = "",
        hazeState = hazeState,
        size = size,
        content = content
    )
}

@Composable
fun GeneGlassIconButton(
    colors: GeneColors,
    onClick: () -> Unit,
    contentDescription: String,
    hazeState: HazeState? = null,
    size: Dp = ControlSize,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = CircleShape
    val glassMod = if (hazeState != null) {
        Modifier.geneGlass(hazeState, colors, GlassThickness.Thin, shape)
    } else {
        Modifier.geneFrostedSurface(colors, shape, elevation = 2.dp)
    }
    Box(
        modifier = Modifier
            .size(size)
            .then(glassMod)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Liquid-glass input chrome: equal-height trailing/leading controls + BasicTextField.
 * Avoids Material OutlinedTextField / IconButton padding that misaligns mic & send.
 */
@Composable
fun GeneGlassInputBar(
    colors: GeneColors,
    hazeState: HazeState?,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = 5,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    // Same pattern as foundation BasicTextField(String): keep selection in state so
    // rebuilding from a String each frame does not snap the caret to index 0.
    var fieldState by remember { mutableStateOf(TextFieldValue(text = value)) }
    val field = fieldState.copy(text = value)
    SideEffect {
        if (field.selection != fieldState.selection ||
            field.composition != fieldState.composition
        ) {
            fieldState = field
        }
    }
    var lastText by remember(value) { mutableStateOf(value) }
    GeneGlassInputBar(
        colors = colors,
        hazeState = hazeState,
        value = field,
        onValueChange = { next ->
            fieldState = next
            val changed = lastText != next.text
            lastText = next.text
            if (changed) onValueChange(next.text)
        },
        placeholder = placeholder,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        leading = leading,
        trailing = trailing
    )
}

@Composable
fun GeneGlassInputBar(
    colors: GeneColors,
    hazeState: HazeState?,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = 5,
    mentionChipBackground: Color? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val glassMod = if (hazeState != null) {
        Modifier.geneGlass(hazeState, colors, GlassThickness.Regular, GlassBarShape)
    } else {
        Modifier.geneFrostedSurface(colors, GlassBarShape, elevation = 10.dp)
    }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val chipBg = mentionChipBackground
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(glassMod)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .defaultMinSize(minHeight = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (leading != null) leading()
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.text.isEmpty()) {
                Text(
                    text = placeholder,
                    color = colors.textTertiary,
                    fontSize = 15.sp,
                    fontFamily = GeneFontFamily,
                    maxLines = if (singleLine) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                maxLines = maxLines,
                cursorBrush = SolidColor(colors.accent),
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontFamily = GeneFontFamily,
                    lineHeight = 20.sp
                ),
                onTextLayout = { textLayout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (chipBg != null) {
                            Modifier.drawMentionChips(
                                annotated = value.annotatedString,
                                layoutResult = textLayout,
                                background = chipBg
                            )
                        } else Modifier
                    )
            )
        }
        if (trailing != null) trailing()
    }
}

/** Fixed circular control — same metrics for mic, send, and secondary actions. */
@Composable
fun GeneBarAction(
    onClick: () -> Unit,
    enabled: Boolean = true,
    filled: Boolean = false,
    muted: Boolean = false,
    danger: Boolean = false,
    colors: GeneColors,
    contentDescription: String,
    size: Dp = ControlSize,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                when {
                    danger -> colors.danger.copy(alpha = 0.16f)
                    filled && enabled -> colors.accent
                    filled || muted -> colors.pill
                    else -> Color.Transparent
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun NotionSectionHeader(
    title: String,
    colors: GeneColors,
    expanded: Boolean = true,
    showMenu: Boolean = false,
    onToggle: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onToggle != null) { onToggle?.invoke() }
            .padding(horizontal = GeneSpace.lg, vertical = GeneSpace.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = GeneFontFamily,
            modifier = Modifier.weight(1f)
        )
        if (onToggle != null) {
            Icon(Icons.Outlined.ExpandMore, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        }
        if (showMenu && onMenu != null) {
            Spacer(Modifier.width(4.dp))
            GeneBarAction(
                onClick = onMenu,
                colors = colors,
                contentDescription = "More"
            ) {
                Icon(
                    Icons.Outlined.MoreHoriz,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun GeneEmptyHint(
    colors: GeneColors,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = colors.textSecondary,
            fontSize = 15.sp,
            fontFamily = GeneFontFamily,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NotionListRow(
    title: String,
    subtitle: String? = null,
    colors: GeneColors,
    tileColor: Color,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(IconTileShape)
                .background(tileColor),
            contentAlignment = Alignment.Center
        ) { leading() }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = GeneFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun NotionRecentCard(
    title: String,
    coverColor: Color,
    colors: GeneColors,
    onClick: () -> Unit,
    coverContent: @Composable () -> Unit,
    footerIcon: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(132.dp)
            .height(152.dp)
            .shadow(4.dp, CardShape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(CardShape)
            .background(colors.surface)
            .border(0.5.dp, colors.border.copy(alpha = 0.7f), CardShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(coverColor),
            contentAlignment = Alignment.Center
        ) { coverContent() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            footerIcon()
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneFontFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Liquid-glass dock: Search · Capture · Chat.
 * Capture is the primary action — pick person → write a memory.
 * New person lives in top chrome next to People/Chats.
 */
@Composable
fun NotionAcrylicDock(
    colors: GeneColors,
    hazeState: HazeState?,
    onSearch: () -> Unit,
    onCapture: () -> Unit,
    onChat: () -> Unit,
    captureLabel: String = "Capture memory",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = GeneSpace.md, end = GeneSpace.md, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val dockMod = if (hazeState != null) {
            Modifier.geneGlass(hazeState, colors, GlassThickness.Regular, DockShape)
        } else {
            Modifier.geneFrostedSurface(colors, DockShape, elevation = 12.dp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(dockMod)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GeneGlassIconButton(colors, onSearch, "Search", null, DockControlSize) {
                Icon(Icons.Outlined.Search, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(DockCaptureHeight)
                    .clip(PillShape)
                    .background(colors.pillActive)
                    .clickable(onClick = onCapture)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.EditNote, null, tint = colors.onPillActive, modifier = Modifier.size(18.dp))
                Text(
                    text = captureLabel,
                    color = colors.onPillActive,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            GeneGlassIconButton(colors, onChat, "Chat", null, DockControlSize) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    icon: ImageVector,
    colors: GeneColors,
    onClick: () -> Unit,
    filled: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(if (filled) colors.pillActive else colors.pill)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            icon,
            null,
            tint = if (filled) colors.onPillActive else colors.textSecondary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            label,
            color = if (filled) colors.onPillActive else colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = GeneFontFamily
        )
    }
}

/**
 * Popup menus: prefer real Haze blur when [hazeState] is provided (same materials as chrome).
 * Falls back to frosted acrylic when no state is available.
 */
@Composable
fun GeneGlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    colors: GeneColors,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(GeneRadius.md)
    val surface = if (hazeState != null) {
        modifier.geneGlass(hazeState, colors, GlassThickness.Thick, shape)
    } else {
        modifier.geneFrostedSurface(colors, shape, elevation = 14.dp)
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = surface,
        shape = shape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(0.5.dp, colors.border.copy(alpha = 0.45f)),
        content = content
    )
}

@Composable
fun geneGlassMenuItemColors(colors: GeneColors) = MenuDefaults.itemColors(
    textColor = colors.textPrimary,
    leadingIconColor = colors.textSecondary,
    trailingIconColor = colors.textSecondary,
    disabledTextColor = colors.textTertiary,
    disabledLeadingIconColor = colors.textTertiary,
    disabledTrailingIconColor = colors.textTertiary
)
