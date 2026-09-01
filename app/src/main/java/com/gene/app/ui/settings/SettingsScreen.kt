package com.gene.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.gene.app.data.GeneDatabase
import com.gene.app.data.ImportSummary
import com.gene.app.service.FloatingCaptureService
import com.gene.app.ui.theme.GeneGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    context: Context,
    db: GeneDatabase,
    onDataChanged: () -> Unit
) {
    val prefs = remember { context.getSharedPreferences("gene_settings", Context.MODE_PRIVATE) }
    var bubble by remember { mutableStateOf(prefs.getBoolean("bubble_enabled", false)) }
    var endpoint by remember { mutableStateOf(prefs.getString("llm_endpoint", "").orEmpty()) }
    var apiKey by remember { mutableStateOf(prefs.getString("llm_api_key", "").orEmpty()) }
    var model by remember { mutableStateOf(prefs.getString("llm_model", "gpt-5-mini").orEmpty()) }
    var saved by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    var pendingImportSummary by remember { mutableStateOf<ImportSummary?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            scope.launch(Dispatchers.IO) {
                try {
                    val json = db.exportToJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Could not open file for writing")
                    launch(Dispatchers.Main) {
                        isExporting = false
                        Toast.makeText(context, "Data exported successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        isExporting = false
                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            scope.launch(Dispatchers.IO) {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: throw IllegalStateException("Could not read selected file")
                    val summary = db.parseBackupSummary(content)
                    launch(Dispatchers.Main) {
                        isImporting = false
                        if (summary == null) {
                            Toast.makeText(context, "Invalid Gene backup file", Toast.LENGTH_SHORT).show()
                        } else {
                            pendingImportContent = content
                            pendingImportSummary = summary
                            showImportDialog = true
                        }
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        isImporting = false
                        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Spacer(Modifier.height(18.dp))
                SettingsSectionTitle("Appearance", "Keep Gene quiet and focused")
            }
            item { SettingsCard { SettingRow("Dark interface", "Pure black background", dark, onToggleTheme) } }
            item {
                Spacer(Modifier.height(28.dp))
                SettingsSectionTitle("Quick capture", "Save a thought without leaving another app")
            }
            item {
                SettingsCard {
                    SettingRow("Floating bubble", "Capture a note from any screen", bubble) {
                        val next = !bubble
                        bubble = next
                        prefs.edit().putBoolean("bubble_enabled", next).apply()
                        if (next && Settings.canDrawOverlays(context)) {
                            context.startService(Intent(context, FloatingCaptureService::class.java))
                        } else if (!next) {
                            context.stopService(Intent(context, FloatingCaptureService::class.java))
                        }
                    }
                    if (bubble && !Settings.canDrawOverlays(context)) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        }) {
                            Text("Allow overlay access")
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(28.dp))
                SettingsSectionTitle("Intelligence", "Local by default, remote only when you choose it")
            }
            item {
                SettingsCard {
                    Text(
                        "Leave the endpoint and key blank to use Gene's offline reflection engine. Otherwise enter either an OpenAI-compatible /v1 base URL or a full /v1/chat/completions URL.",
                        color = GeneGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(value = endpoint, onValueChange = { endpoint = it; saved = false }, label = { Text("Endpoint URL") }, placeholder = { Text("https://.../v1") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = model, onValueChange = { model = it; saved = false }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it; saved = false }, label = { Text("API key") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = {
                        prefs.edit().putString("llm_endpoint", endpoint.trim()).putString("llm_model", model.trim()).putString("llm_api_key", apiKey.trim()).apply()
                        saved = true
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (saved) "Saved" else "Save intelligence settings")
                    }
                }
            }
            item {
                Spacer(Modifier.height(28.dp))
                SettingsSectionTitle("Data & Backup", "Export your memories or restore from a file")
            }
            item {
                SettingsCard {
                    Text(
                        "Export creates a complete JSON backup containing all people, memories, and conversations. Import restores or merges records back into Gene.",
                        color = GeneGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                exportLauncher.launch("gene_backup_$timestamp.json")
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isExporting && !isImporting
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isExporting) "Exporting..." else "Export data")
                        }
                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isExporting && !isImporting
                        ) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isImporting) "Reading..." else "Import data")
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(28.dp))
                SettingsSectionTitle("Privacy", "Your context stays yours")
            }
            item {
                SettingsCard {
                    Text(
                        "Gene saves people, notes, transcripts, sessions, and message references locally on this device. It does not record continuously. Audio is used only to create a transcript when you choose Audio memory.",
                        color = GeneGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Remote analysis is opt-in. When enabled, only locally selected context is sent to the endpoint you entered. Persona replies are working hypotheses, not certainty about another person's private thoughts.",
                        color = GeneGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            item { Spacer(Modifier.height(34.dp)) }
        }
    }

    if (showImportDialog && pendingImportSummary != null) {
        val summary = pendingImportSummary!!
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportContent = null
                pendingImportSummary = null
            },
            title = { Text("Import data", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This backup contains:", style = MaterialTheme.typography.bodyMedium)
                    Text("• ${summary.peopleCount} ${if (summary.peopleCount == 1) "person" else "people"}", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                    Text("• ${summary.memoryCount} ${if (summary.memoryCount == 1) "memory" else "memories"}", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                    Text("• ${summary.sessionCount} ${if (summary.sessionCount == 1) "conversation" else "conversations"} (${summary.messageCount} messages)", color = GeneGray, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("How would you like to import this backup?", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val content = pendingImportContent ?: return@Button
                    showImportDialog = false
                    scope.launch(Dispatchers.IO) {
                        try {
                            val result = db.importFromJson(content, replaceExisting = false)
                            launch(Dispatchers.Main) {
                                onDataChanged()
                                Toast.makeText(context, "Merged ${result.peopleCount} people and ${result.memoryCount} memories", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            pendingImportContent = null
                            pendingImportSummary = null
                        }
                    }
                }) {
                    Text("Merge")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showImportDialog = false
                        pendingImportContent = null
                        pendingImportSummary = null
                    }) {
                        Text("Cancel")
                    }
                    OutlinedButton(onClick = {
                        val content = pendingImportContent ?: return@OutlinedButton
                        showImportDialog = false
                        scope.launch(Dispatchers.IO) {
                            try {
                                val result = db.importFromJson(content, replaceExisting = true)
                                launch(Dispatchers.Main) {
                                    onDataChanged()
                                    Toast.makeText(context, "Restored ${result.peopleCount} people and ${result.memoryCount} memories", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                pendingImportContent = null
                                pendingImportSummary = null
                            }
                        }
                    }) {
                        Text("Replace all")
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = GeneGray, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
fun SettingRow(title: String, subtitle: String, checked: Boolean, onChecked: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = GeneGray, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = { onChecked() })
    }
}
