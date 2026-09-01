package com.gene.app.ui.home

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gene.app.data.Person
import com.gene.app.ui.theme.GeneGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    people: List<Person>,
    onPerson: (Long) -> Unit,
    onSettings: () -> Unit,
    onPersonCreated: (String, String) -> Unit,
    onRefresh: () -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("gene", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.PersonAdd, "Add person")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(18.dp))
                Text("People", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("A private memory for understanding patterns over time.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
            }
            if (people.isEmpty()) item { EmptyState(onAdd = { showAdd = true }) }
            items(people, key = { it.id }) { person -> PersonCard(person, onClick = { onPerson(person.id) }) }
            item { Spacer(Modifier.height(86.dp)) }
        }
    }
    if (showAdd) AddPersonSheet(onDismiss = { showAdd = false }, onCreate = { n, note -> showAdd = false; onPersonCreated(n, note) })
}

@Composable
fun EmptyState(onAdd: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Start with one person", style = MaterialTheme.typography.titleMedium)
            Text("Add someone you want to understand better. Capture only what you choose.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add person")
            }
        }
    }
}

@Composable
fun PersonCard(person: Person, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Text(person.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 21.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(person.name, style = MaterialTheme.typography.titleMedium)
                Text(if (person.interactionCount == 0) "No memories yet" else " memor", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Outlined.MoreHoriz, "Open")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonSheet(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Add a person", style = MaterialTheme.typography.titleLarge)
            Text("Start a private thread of context. You can add details later.", color = GeneGray, style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("First impression, optional") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Button(onClick = { if (name.isNotBlank()) onCreate(name, note) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Begin thread") }
        }
    }
}
