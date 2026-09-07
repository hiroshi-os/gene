package com.tom7.gene.ui.memory

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom7.gene.data.GeneDatabase
import com.tom7.gene.data.Interaction
import com.tom7.gene.data.Person
import com.tom7.gene.data.TRANSCRIPTION_PENDING
import com.tom7.gene.data.TRANSCRIPTION_UNAVAILABLE
import com.tom7.gene.data.TYPE_AUDIO
import com.tom7.gene.ui.common.GeneGlassIconButton
import com.tom7.gene.ui.common.asDate
import com.tom7.gene.ui.common.geneCardSurface
import com.tom7.gene.ui.common.geneHazeSource
import com.tom7.gene.ui.common.rememberGeneHazeState
import com.tom7.gene.ui.theme.GeneFontFamily
import com.tom7.gene.ui.theme.GeneRadius
import com.tom7.gene.ui.theme.GeneSpace
import com.tom7.gene.ui.theme.NotionDarkBg
import com.tom7.gene.ui.theme.rememberGeneColors

private val ButtonShape = RoundedCornerShape(GeneRadius.sm)

@Composable
fun MemoryDetailScreen(
    person: Person,
    memory: Interaction,
    db: GeneDatabase,
    onBack: () -> Unit
) {
    val dark = MaterialTheme.colorScheme.background == NotionDarkBg
    val colors = rememberGeneColors(dark)
    val hazeState = rememberGeneHazeState()
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    val playInteraction = remember { MutableInteractionSource() }

    DisposableEffect(memory.id) {
        onDispose {
            player?.release()
            player = null
            playing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .geneHazeSource(hazeState)
                .statusBarsPadding()
                .padding(top = 56.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${person.name} · ${memory.createdAt.asDate()}",
                color = colors.textTertiary,
                fontSize = 13.sp,
                fontFamily = GeneFontFamily
            )

            if (memory.type == TYPE_AUDIO && memory.audioUri != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .geneCardSurface(colors, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(20, 34, 14, 28, 40, 18, 32, 24, 12, 36, 20, 28).forEach { bar ->
                            Box(
                                Modifier
                                    .width(4.dp)
                                    .height(bar.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.accent.copy(alpha = 0.5f))
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(ButtonShape)
                            .background(if (playing) colors.pill else colors.pillActive)
                            .clickable(interactionSource = playInteraction, indication = null) {
                                if (playing) {
                                    player?.pause()
                                    playing = false
                                } else {
                                    player?.release()
                                    player = MediaPlayer.create(context, Uri.parse(memory.audioUri))
                                    player?.setOnCompletionListener { playing = false }
                                    player?.start()
                                    playing = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (playing) "Pause recording" else "Play recording",
                            color = if (playing) colors.textPrimary else colors.onPillActive,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = GeneFontFamily
                        )
                    }
                }
            }

            Text(
                text = if (memory.type == TYPE_AUDIO && memory.transcriptionStatus == TRANSCRIPTION_PENDING) {
                    "Transcription is still processing in the background."
                } else {
                    memory.transcript?.takeIf { it.isNotBlank() } ?: memory.body
                },
                color = colors.textPrimary,
                fontSize = 17.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = GeneFontFamily
            )

            if (memory.type == TYPE_AUDIO && memory.transcriptionStatus == TRANSCRIPTION_UNAVAILABLE) {
                Text(
                    text = "The audio is saved on this device. Add a compatible transcription provider in Settings if you want a text transcript.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = GeneFontFamily
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = GeneSpace.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GeneSpace.xs)
        ) {
            GeneGlassIconButton(
                colors = colors,
                hazeState = hazeState,
                onClick = onBack,
                contentDescription = "Back"
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
            }
            Text(
                text = memoryTypeLabel(memory.type),
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GeneFontFamily,
                modifier = Modifier.weight(1f)
            )
            GeneGlassIconButton(
                colors = colors,
                hazeState = hazeState,
                onClick = {
                    db.deleteInteraction(memory.id)
                    onBack()
                },
                contentDescription = "Delete memory"
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = colors.danger, modifier = Modifier.size(18.dp))
            }
        }
    }
}
