package com.tom7.gene.ui.calendar

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom7.gene.data.GeneDatabase
import com.tom7.gene.data.Person
import com.tom7.gene.ui.common.GeneGlassIconButton
import com.tom7.gene.ui.common.geneHazeSource
import com.tom7.gene.ui.common.rememberGeneHazeState
import com.tom7.gene.ui.memory.MemoryCard
import com.tom7.gene.ui.theme.GeneFontFamily
import com.tom7.gene.ui.theme.GeneRadius
import com.tom7.gene.ui.theme.GeneSpace
import com.tom7.gene.ui.theme.rememberGeneColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    person: Person,
    db: GeneDatabase,
    onBack: () -> Unit,
    onOpenMemory: (Long) -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = rememberGeneColors(dark)
    val hazeState = rememberGeneHazeState()

    var monthCursor by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }
    var selectedDay by remember { mutableStateOf<String?>(null) }
    val memories = db.interactions(person.id)
    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val titleFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayLabelFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val byDay = remember(memories) { memories.groupBy { dayFormat.format(Date(it.createdAt)) } }

    val daysInMonth = monthCursor.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = monthCursor.get(Calendar.DAY_OF_WEEK)
    val leadingBlanks = firstDayOfWeek - 1
    val cells = buildList {
        repeat(leadingBlanks) { add(null) }
        for (day in 1..daysInMonth) add(day)
    }

    val todayKey = remember { dayFormat.format(Date()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .geneHazeSource(hazeState)
                .statusBarsPadding()
                .padding(top = 56.dp)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    "Calendar",
                    color = colors.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GeneFontFamily
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    person.name,
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontFamily = GeneFontFamily
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GeneRadius.md))
                    .background(colors.surface)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(GeneRadius.xs))
                        .clickable {
                            monthCursor = (monthCursor.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ChevronLeft, "Previous month", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                }
                Text(
                    titleFormat.format(monthCursor.time),
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneFontFamily,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(GeneRadius.xs))
                        .clickable {
                            monthCursor = (monthCursor.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ChevronRight, "Next month", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GeneRadius.lg))
                    .background(colors.surface)
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                        Text(
                            it,
                            color = colors.textTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = GeneFontFamily,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        week.forEach { day ->
                            val date = if (day == null) null else (monthCursor.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, day)
                            }
                            val key = date?.let { dayFormat.format(it.time) }
                            val count = key?.let { byDay[it]?.size ?: 0 } ?: 0
                            val isToday = key == todayKey
                            val hasMemories = count > 0

                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(GeneRadius.xs))
                                    .background(if (isToday) colors.pill else Color.Transparent)
                                    .clickable(enabled = day != null && hasMemories) { selectedDay = key },
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (day != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        Text(
                                            day.toString(),
                                            color = when {
                                                isToday -> colors.accent
                                                hasMemories -> colors.textPrimary
                                                else -> colors.textSecondary
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = if (isToday || hasMemories) FontWeight.SemiBold else FontWeight.Normal,
                                            fontFamily = GeneFontFamily
                                        )
                                        if (hasMemories) {
                                            Box(
                                                Modifier
                                                    .size(5.dp)
                                                    .background(colors.accent, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                if (byDay.isEmpty()) {
                    "Saved memories will appear on their days."
                } else {
                    "Tap a marked day to see its memories."
                },
                color = colors.textTertiary,
                fontSize = 13.sp,
                fontFamily = GeneFontFamily
            )
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
        }
    }

    selectedDay?.let { key ->
        val dayMemories = byDay[key].orEmpty()
        ModalBottomSheet(
            onDismissRequest = { selectedDay = null },
            containerColor = colors.surface
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    dayMemories.firstOrNull()?.let { dayLabelFormat.format(Date(it.createdAt)) } ?: "Memories",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeneFontFamily
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 430.dp)
                ) {
                    items(dayMemories, key = { it.id }) { memory ->
                        MemoryCard(memory, onClick = {
                            selectedDay = null
                            onOpenMemory(memory.id)
                        })
                    }
                }
            }
        }
    }
}
