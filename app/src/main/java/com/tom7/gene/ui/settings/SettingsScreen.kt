package com.tom7.gene.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom7.gene.data.GeneDatabase
import com.tom7.gene.data.ImportSummary
import com.tom7.gene.data.LlmSettings
import com.tom7.gene.service.FloatingCaptureService
import com.tom7.gene.ui.common.GeneGlassIconButton
import com.tom7.gene.ui.common.geneHazeSource
import com.tom7.gene.ui.common.rememberGeneHazeState
import com.tom7.gene.ui.theme.GeneColors
import com.tom7.gene.ui.theme.GeneFontFamily
import com.tom7.gene.ui.theme.GeneRadius
import com.tom7.gene.ui.theme.GeneSpace
import com.tom7.gene.ui.theme.rememberGeneColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GroupShape = RoundedCornerShape(GeneRadius.md)
private val IconTileShape = RoundedCornerShape(GeneRadius.xs)

@Composable
fun SettingsScreen(
    dark: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    context: Context,
    db: GeneDatabase,
    onDataChanged: () -> Unit
) {
    val colors = rememberGeneColors(dark)
    val hazeState = rememberGeneHazeState()
    val prefs = remember { context.getSharedPreferences(LlmSettings.PREFS, Context.MODE_PRIVATE) }
    var bubble by remember { mutableStateOf(prefs.getBoolean("bubble_enabled", false)) }
    var llmMode by remember { mutableStateOf(LlmSettings.mode(prefs)) }
    var endpoint by remember { mutableStateOf(prefs.getString("llm_endpoint", "").orEmpty()) }
    var apiKey by remember { mutableStateOf(prefs.getString("llm_api_key", "").orEmpty()) }
    var model by remember { mutableStateOf(prefs.getString("llm_model", LlmSettings.DEFAULT_BYOK_MODEL).orEmpty()) }
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
                .padding(top = 56.dp)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Text(
                    "Settings",
                    color = colors.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GeneFontFamily,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
            }

            item {
                SettingsSectionTitle("Appearance", "Keep Gene quiet and focused", colors)
                SettingsGroup(colors) {
                    SettingRow(
                        title = "Dark interface",
                        subtitle = "Pure black background",
                        checked = dark,
                        onChecked = onToggleTheme,
                        colors = colors,
                        icon = Icons.Outlined.DarkMode,
                        tileColor = colors.pastel(0)
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                SettingsSectionTitle("Quick capture", "Save a thought without leaving another app", colors)
                SettingsGroup(colors) {
                    SettingRow(
                        title = "Floating bubble",
                        subtitle = "Capture a note from any screen",
                        checked = bubble,
                        colors = colors,
                        icon = Icons.Outlined.BubbleChart,
                        tileColor = colors.pastel(1),
                        onChecked = {
                            val next = !bubble
                            bubble = next
                            prefs.edit().putBoolean("bubble_enabled", next).apply()
                            if (next && Settings.canDrawOverlays(context)) {
                                context.startService(Intent(context, FloatingCaptureService::class.java))
                            } else if (!next) {
                                context.stopService(Intent(context, FloatingCaptureService::class.java))
                            }
                        }
                    )
                    if (bubble && !Settings.canDrawOverlays(context)) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)
                        ) {
                            Text("Allow overlay access")
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                SettingsSectionTitle("Intelligence", "Choose how Gene thinks for you", colors)
                SettingsGroup(colors) {
                    Text(
                        "Gene AI uses Gene's hosted gateway. BYOK keeps your own OpenAI-compatible endpoint and key on device.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = GeneFontFamily,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IntelligenceModeChip(
                            title = "Gene AI",
                            subtitle = "Hosted",
                            selected = llmMode == LlmSettings.MODE_GENE_AI,
                            colors = colors,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                llmMode = LlmSettings.MODE_GENE_AI
                                saved = false
                            }
                        )
                        IntelligenceModeChip(
                            title = "BYOK",
                            subtitle = "Your key",
                            selected = llmMode == LlmSettings.MODE_BYOK,
                            colors = colors,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                llmMode = LlmSettings.MODE_BYOK
                                saved = false
                            }
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (llmMode == LlmSettings.MODE_GENE_AI) {
                        Text(
                            "No API key needed. Gene routes requests through its hosted gateway.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            fontFamily = GeneFontFamily,
                            lineHeight = 18.sp
                        )
                    } else {
                        Text(
                            "Enter an OpenAI-compatible /v1 base URL or a full /v1/chat/completions URL, plus your key. Leave blank to stay fully offline.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            fontFamily = GeneFontFamily,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        SoftSettingsField(
                            value = endpoint,
                            onValueChange = { endpoint = it; saved = false },
                            label = "Endpoint URL",
                            placeholder = "https://.../v1",
                            colors = colors
                        )
                        Spacer(Modifier.height(10.dp))
                        SoftSettingsField(
                            value = model,
                            onValueChange = { model = it; saved = false },
                            label = "Model",
                            colors = colors
                        )
                        Spacer(Modifier.height(10.dp))
                        SoftSettingsField(
                            value = apiKey,
                            onValueChange = { apiKey = it; saved = false },
                            label = "API key",
                            colors = colors,
                            password = true
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            prefs.edit()
                                .putString("llm_mode", llmMode)
                                .putString("llm_endpoint", endpoint.trim())
                                .putString("llm_model", model.trim())
                                .putString("llm_api_key", apiKey.trim())
                                .apply()
                            saved = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onPillActive),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (saved) "Saved" else "Save intelligence settings")
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                SettingsSectionTitle("Data & Backup", "Export your memories or restore from a file", colors)
                SettingsGroup(colors) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(IconTileShape)
                                .background(colors.pastel(3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Memory, null, tint = colors.textPrimary.copy(alpha = 0.65f), modifier = Modifier.size(15.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Export creates a complete JSON backup. Import restores or merges records back into Gene.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            fontFamily = GeneFontFamily,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                exportLauncher.launch("gene_backup_$timestamp.json")
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isExporting && !isImporting,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isExporting) "Exporting..." else "Export", fontSize = 13.sp, fontFamily = GeneFontFamily)
                        }
                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isExporting && !isImporting,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onPillActive),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isImporting) "Reading..." else "Import", fontSize = 13.sp, fontFamily = GeneFontFamily)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                SettingsSectionTitle("Privacy", "Your context stays yours", colors)
                SettingsGroup(colors) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(IconTileShape)
                                .background(colors.pastel(4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Lock, null, tint = colors.textPrimary.copy(alpha = 0.65f), modifier = Modifier.size(15.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Gene saves people, notes, transcripts, sessions, and message references locally on this device. It does not record continuously. Audio is used only to create a transcript when you choose Audio memory.",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                fontFamily = GeneFontFamily,
                                lineHeight = 18.sp
                            )
                            Text(
                                "Gene AI sends only locally selected context to Gene's gateway. BYOK sends that same context to the endpoint you configure. Persona replies are working hypotheses, not certainty about another person's private thoughts.",
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                                fontFamily = GeneFontFamily,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
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

    if (showImportDialog && pendingImportSummary != null) {
        val summary = pendingImportSummary!!
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportContent = null
                pendingImportSummary = null
            },
            containerColor = colors.surface,
            title = {
                Text("Import data", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeneFontFamily)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This backup contains:", color = colors.textPrimary, fontSize = 14.sp, fontFamily = GeneFontFamily)
                    Text(
                        "• ${summary.peopleCount} ${if (summary.peopleCount == 1) "person" else "people"}",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = GeneFontFamily
                    )
                    Text(
                        "• ${summary.memoryCount} ${if (summary.memoryCount == 1) "memory" else "memories"}",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = GeneFontFamily
                    )
                    Text(
                        "• ${summary.sessionCount} ${if (summary.sessionCount == 1) "conversation" else "conversations"} (${summary.messageCount} messages)",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = GeneFontFamily
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("How would you like to import this backup?", color = colors.textPrimary, fontSize = 14.sp, fontFamily = GeneFontFamily)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onPillActive),
                    shape = RoundedCornerShape(8.dp)
                ) {
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
                        Text("Cancel", color = colors.textSecondary)
                    }
                    OutlinedButton(
                        onClick = {
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
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger)
                    ) {
                        Text("Replace all")
                    }
                }
            }
        )
    }
}

@Composable
private fun IntelligenceModeChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    colors: GeneColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.accent.copy(alpha = 0.12f) else colors.pill)
            .then(
                if (selected) Modifier.border(1.dp, colors.accent.copy(alpha = 0.45f), shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            title,
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneFontFamily
        )
        Text(
            subtitle,
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontFamily = GeneFontFamily
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String, subtitle: String, colors: GeneColors = rememberGeneColors(false)) {
    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title.uppercase(),
            color = colors.textTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneFontFamily,
            letterSpacing = 0.6.sp
        )
        Text(subtitle, color = colors.textSecondary, fontSize = 13.sp, fontFamily = GeneFontFamily)
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = rememberGeneColors(dark)
    SettingsGroup(colors, content)
}

@Composable
private fun SettingsGroup(colors: GeneColors, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GroupShape)
            .background(colors.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
fun SettingRow(title: String, subtitle: String, checked: Boolean, onChecked: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = rememberGeneColors(dark)
    SettingRow(
        title = title,
        subtitle = subtitle,
        checked = checked,
        onChecked = onChecked,
        colors = colors,
        icon = Icons.Outlined.DarkMode,
        tileColor = colors.pastel(0)
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: () -> Unit,
    colors: GeneColors,
    icon: ImageVector,
    tileColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(IconTileShape)
                .background(tileColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = colors.textPrimary.copy(alpha = 0.65f), modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = GeneFontFamily)
            Text(subtitle, color = colors.textSecondary, fontSize = 12.sp, fontFamily = GeneFontFamily)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onChecked() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.pill,
                uncheckedBorderColor = colors.border
            )
        )
    }
}

@Composable
private fun SoftSettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    colors: GeneColors,
    placeholder: String = "",
    password: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) {{ Text(placeholder, color = colors.textTertiary) }} else null,
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = colors.pill,
            unfocusedContainerColor = colors.pill,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.accent,
            focusedLabelColor = colors.textSecondary,
            unfocusedLabelColor = colors.textTertiary
        )
    )
}
