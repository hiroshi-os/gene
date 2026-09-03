package com.gene.app.ui.calendar

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gene.app.data.GeneDatabase
import com.gene.app.data.Person
import com.gene.app.ui.memory.MemoryCard
import com.gene.app.ui.theme.GeneGray
import com.gene.app.ui.theme.IosBlue
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar · ${person.name}", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = IosBlue) } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthCursor = (monthCursor.clone() as Calendar).apply { add(Calendar.MONTH, -1) } }) {
                    Icon(Icons.Outlined.ChevronLeft, "Previous month", tint = IosBlue)
                }
                Text(titleFormat.format(monthCursor.time), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { monthCursor = (monthCursor.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }) {
                    Icon(Icons.Outlined.ChevronRight, "Next month", tint = IosBlue)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Text(it, color = GeneGray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        week.forEach { day ->
                            val date = if (day == null) null else (monthCursor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                            val key = date?.let { dayFormat.format(it.time) }
                            val count = key?.let { byDay[it]?.size ?: 0 } ?: 0
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(58.dp)
                                    .clickable(enabled = day != null && count > 0) { selectedDay = key },
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (day != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(day.toString(), style = MaterialTheme.typography.bodyLarge)
                                        if (count > 0) Box(Modifier.size(7.dp).background(IosBlue, CircleShape))
                                        if (count > 0) Text(count.toString(), color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (byDay.isEmpty()) {
                Text("Saved memories will appear on their days.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            } else {
                Text("Tap a marked day to see its memories.", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    selectedDay?.let { key ->
        val dayMemories = byDay[key].orEmpty()
        ModalBottomSheet(onDismissRequest = { selectedDay = null }, containerColor = MaterialTheme.colorScheme.background) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(dayMemories.firstOrNull()?.let { dayLabelFormat.format(Date(it.createdAt)) } ?: "Memories", style = MaterialTheme.typography.titleLarge)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 430.dp)) {
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
