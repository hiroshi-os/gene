package com.tom7.gene.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom7.gene.data.GeneDatabase
import com.tom7.gene.data.TYPE_TEXT
import com.tom7.gene.ui.blob.GeneBlob
import com.tom7.gene.ui.blob.GeneBlobMood
import com.tom7.gene.ui.theme.BlobCream
import com.tom7.gene.ui.theme.BlobDeep
import com.tom7.gene.ui.theme.BlobStage
import com.tom7.gene.ui.theme.BlobStageMuted
import com.tom7.gene.ui.theme.GeneFontFamily
import com.tom7.gene.ui.theme.NotionDarkBg
import com.tom7.gene.ui.theme.NotionLightBg
import kotlinx.coroutines.delay

const val ONBOARDING_COMPLETE_KEY = "onboarding_complete"

private const val STEP_COUNT = 8
private val StageText = Color(0xFFF4EBD8)
private val StageDim = Color(0xFF9A9488)
private val FieldShape = RoundedCornerShape(18.dp)
private val CardShape = RoundedCornerShape(18.dp)
private val CtaShape = RoundedCornerShape(28.dp)

private data class MemoryKind(
    val type: String,
    val label: String,
    val hint: String,
    val icon: ImageVector
)

private val MemoryKinds = listOf(
    MemoryKind(TYPE_TEXT, "Note", "A moment you don’t want to lose", Icons.Outlined.TextSnippet),
    MemoryKind("quote", "Quote", "Something they actually said", Icons.Outlined.BookmarkBorder),
    MemoryKind("signal", "Signal", "A cue — plus what you think it means", Icons.Outlined.Lightbulb),
    MemoryKind("pattern", "Pattern", "Something that keeps coming up", Icons.Outlined.History),
    MemoryKind("feeling", "Feeling", "How being with them lands", Icons.Outlined.WbSunny),
    MemoryKind("question", "Question", "Something you still want to understand", Icons.Outlined.HelpOutline)
)

@Composable
fun OnboardingScreen(
    dark: Boolean,
    db: GeneDatabase,
    onToggleTheme: () -> Unit,
    onFinished: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var woke by remember { mutableStateOf(false) }
    val ideaTaps = remember { mutableStateListOf<Int>() }
    var personName by remember { mutableStateOf("") }
    var personRelation by remember { mutableStateOf("Friend") }
    var createdPersonId by remember { mutableStateOf<Long?>(null) }
    var memoryKind by remember { mutableStateOf<MemoryKind?>(null) }
    var memoryText by remember { mutableStateOf("") }
    var memorySaved by remember { mutableStateOf(false) }
    var asked by remember { mutableStateOf(false) }
    var lastPrompt by remember { mutableStateOf<String?>(null) }
    var reply by remember { mutableStateOf<String?>(null) }
    var thinking by remember { mutableStateOf(false) }
    val privacy = remember { mutableStateListOf<Int>() }
    var preferDark by remember { mutableStateOf(dark) }
    var goingForward by remember { mutableStateOf(true) }

    val blobMood = when {
        thinking -> GeneBlobMood.Thinking
        step == 0 && !woke -> GeneBlobMood.Idle
        step == 0 -> GeneBlobMood.Happy
        step == 1 -> GeneBlobMood.Curious
        step == 2 && personName.isNotBlank() -> GeneBlobMood.Listening
        step == 3 && memoryKind != null -> GeneBlobMood.Curious
        step == 4 && asked -> GeneBlobMood.Happy
        step == 4 -> GeneBlobMood.Listening
        step == 5 -> GeneBlobMood.Idle
        step == 6 -> if (preferDark) GeneBlobMood.Idle else GeneBlobMood.Happy
        else -> GeneBlobMood.Happy
    }

    fun savePersonIfNeeded() {
        if (createdPersonId != null) return
        if (personRelation == "Me") {
            val self = db.getOrCreateSelf()
            val name = personName.trim()
            if (name.isNotBlank() && !name.equals(self.name, ignoreCase = true)) {
                db.updatePersonName(self.id, name)
            }
            createdPersonId = self.id
            return
        }
        val name = personName.trim()
        if (name.isBlank()) return
        val existing = db.people().firstOrNull { it.name.equals(name, ignoreCase = true) }
        createdPersonId = existing?.id ?: db.addPerson(name, personRelation)
    }

    fun finish() {
        savePersonIfNeeded()
        val body = memoryText.trim()
        val personId = createdPersonId ?: db.getOrCreateSelf().id
        if (body.isNotBlank() && !memorySaved) {
            db.addInteraction(personId, memoryKind?.type ?: TYPE_TEXT, body)
            memorySaved = true
        }
        if (preferDark != dark) onToggleTheme()
        onFinished()
    }

    BackHandler(enabled = step > 0) {
        goingForward = false
        step--
    }

    val canContinue = when (step) {
        0 -> woke
        1 -> ideaTaps.size >= 3
        5 -> privacy.size >= 3
        else -> true
    }
    val continueLabel = when (step) {
        0 -> "Continue"
        1 -> if (canContinue) "Continue" else "Tap each idea"
        2 -> if (personName.isBlank()) "Continue without a name" else "Remember ${personName.trim().substringBefore(' ')}"
        3 -> if (memoryText.isBlank()) "Continue without a memory" else "Save this memory"
        4 -> "Continue"
        5 -> if (canContinue) "I understand" else "Tap each one"
        6 -> "Use this look"
        else -> "Open Gene"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlobStage)
    ) {
        StageGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    "Skip",
                    color = BlobStageMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = GeneFontFamily,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { finish() }
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val dir = if (goingForward) 1 else -1
                    (slideInHorizontally { it * dir / 5 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it * dir / 5 } + fadeOut())
                },
                label = "onboard-step"
            ) { page ->
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (page) {
                        0 -> MeetStep(mood = blobMood, woke = woke, onWake = { woke = true })
                        1 -> IdeasStep(taps = ideaTaps, mood = blobMood, onTap = { id ->
                            if (id !in ideaTaps) ideaTaps.add(id)
                        })
                        2 -> PersonStep(
                            mood = blobMood,
                            name = personName,
                            relation = personRelation,
                            onName = { personName = it },
                            onRelation = { personRelation = it }
                        )
                        3 -> MemoryStep(
                            mood = blobMood,
                            personName = personName.trim().ifBlank { "them" },
                            selected = memoryKind,
                            text = memoryText,
                            onSelect = { memoryKind = it },
                            onText = { memoryText = it }
                        )
                        4 -> AskStep(
                            mood = blobMood,
                            personName = personName.trim().ifBlank { "someone" },
                            memoryText = memoryText.trim(),
                            asked = asked,
                            thinking = thinking,
                            reply = reply,
                            onAsk = { prompt ->
                                asked = true
                                lastPrompt = prompt
                                thinking = true
                                reply = null
                            }
                        )
                        5 -> PrivacyStep(selected = privacy, mood = blobMood, onToggle = { id ->
                            if (id in privacy) privacy.remove(id) else privacy.add(id)
                        })
                        6 -> LookStep(mood = blobMood, preferDark = preferDark, onPick = { preferDark = it })
                        else -> ReadyStep(
                            mood = blobMood,
                            personName = personName.trim(),
                            savedMemory = memoryText.isNotBlank(),
                            asked = asked,
                            preferDark = preferDark
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(STEP_COUNT) { i ->
                        Box(
                            Modifier
                                .size(if (i == step) 7.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (i == step) BlobCream else Color.White.copy(alpha = 0.18f))
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                val enabled = canContinue || step == STEP_COUNT - 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(CtaShape)
                        .background(if (enabled) BlobCream else BlobCream.copy(alpha = 0.28f))
                        .clickable(enabled = enabled) {
                            if (step == 2) savePersonIfNeeded()
                            if (step == 3 && memoryText.isNotBlank() && !memorySaved) {
                                val personId = createdPersonId ?: db.getOrCreateSelf().id
                                db.addInteraction(personId, memoryKind?.type ?: TYPE_TEXT, memoryText.trim())
                                memorySaved = true
                            }
                            if (step >= STEP_COUNT - 1) {
                                finish()
                            } else {
                                goingForward = true
                                step++
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        continueLabel,
                        color = if (enabled) BlobDeep else BlobDeep.copy(alpha = 0.45f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = GeneFontFamily
                    )
                }
            }
        }
    }

    LaunchedEffect(thinking, lastPrompt, personName, memoryText) {
        if (!thinking) return@LaunchedEffect
        delay(900)
        val who = personName.trim().ifBlank { "them" }
        val note = memoryText.trim()
        reply = when {
            lastPrompt?.contains("pattern", ignoreCase = true) == true && note.isNotBlank() ->
                "A pattern I can already see: $who shows up in “$note”. Gene would stack later notes next to this and tell you when it repeats."
            lastPrompt?.contains("show up", ignoreCase = true) == true && note.isNotBlank() ->
                "Next time you see $who, you already have a handle: “$note”. Ask from their profile — Gene only uses what you captured."
            note.isNotBlank() ->
                "From what you saved: $who — “$note”. Gene keeps that with their other notes and uses it the next time you ask."
            who != "them" ->
                "I don’t have a memory for $who yet. Capture one quote or feeling, then ask again — replies stay grounded in what you wrote."
            else ->
                "Ask after you capture something. Gene searches your notes locally and answers from those, not from invented private thoughts."
        }
        thinking = false
    }
}

@Composable
private fun StageGlow() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .size(280.dp)
                .blur(90.dp)
                .background(BlobCream.copy(alpha = 0.12f), CircleShape)
        )
    }
}

@Composable
private fun MeetStep(mood: GeneBlobMood, woke: Boolean, onWake: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    GeneBlob(
        modifier = Modifier.size(240.dp),
        mood = mood,
        interactive = true,
        onInteract = onWake
    )
    Spacer(Modifier.height(8.dp))
    Text(
        if (woke) "Hi. I’m Gene." else "Meet Gene",
        color = StageText,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = GeneFontFamily,
        letterSpacing = (-0.6).sp,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(10.dp))
    Text(
        if (woke)
            "I keep a private picture of the people in your life — notes, quotes, feelings — then talk with you about them."
        else
            "Tap or drag the blob. It looks back.",
        color = StageDim,
        fontSize = 16.sp,
        fontFamily = GeneFontFamily,
        lineHeight = 23.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun IdeasStep(taps: List<Int>, mood: GeneBlobMood, onTap: (Int) -> Unit) {
    GeneBlob(modifier = Modifier.size(148.dp), mood = mood, interactive = true)
    Text("Gene is built around three things", color = StageText, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = GeneFontFamily, textAlign = TextAlign.Center, lineHeight = 32.sp)
    Spacer(Modifier.height(6.dp))
    Text("Tap each card. This is the whole product.", color = StageDim, fontSize = 14.sp, fontFamily = GeneFontFamily, textAlign = TextAlign.Center)
    Spacer(Modifier.height(18.dp))
    IdeaCard(0, Icons.Outlined.People, "People", "Persistent profiles — not a pile of forgotten chats.", taps, onTap)
    Spacer(Modifier.height(10.dp))
    IdeaCard(1, Icons.Outlined.FavoriteBorder, "Memories", "Quotes, signals, patterns, feelings. You choose the shape.", taps, onTap)
    Spacer(Modifier.height(10.dp))
    IdeaCard(2, Icons.Outlined.ChatBubbleOutline, "Conversations", "Ask about someone. Gene answers from what you captured.", taps, onTap)
}

@Composable
private fun IdeaCard(
    id: Int,
    icon: ImageVector,
    title: String,
    body: String,
    taps: List<Int>,
    onTap: (Int) -> Unit
) {
    val on = id in taps
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (on) BlobCream.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (on) BlobCream.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f), CardShape)
            .clickable { onTap(id) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (on) BlobCream else Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (on) BlobDeep else StageText, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = StageText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeneFontFamily)
            Text(body, color = StageDim, fontSize = 13.sp, fontFamily = GeneFontFamily, lineHeight = 18.sp)
        }
        if (on) {
            Icon(Icons.Outlined.Check, null, tint = BlobCream, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonStep(
    mood: GeneBlobMood,
    name: String,
    relation: String,
    onName: (String) -> Unit,
    onRelation: (String) -> Unit
) {
    GeneBlob(modifier = Modifier.size(140.dp), mood = mood, interactive = true)
    Text("Who should Gene remember first?", color = StageText, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = GeneFontFamily, textAlign = TextAlign.Center, lineHeight = 32.sp)
    Spacer(Modifier.height(6.dp))
    Text("A friend, partner, parent, coworker — anyone you keep thinking about. You can add more later.", color = StageDim, fontSize = 14.sp, fontFamily = GeneFontFamily, textAlign = TextAlign.Center, lineHeight = 20.sp)
    Spacer(Modifier.height(20.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), FieldShape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (name.isEmpty()) {
            Text("Their name", color = StageDim.copy(alpha = 0.7f), fontSize = 17.sp, fontFamily = GeneFontFamily)
        }
        BasicTextField(
            value = name,
            onValueChange = onName,
            singleLine = true,
            textStyle = TextStyle(color = StageText, fontSize = 17.sp, fontFamily = GeneFontFamily, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(BlobCream),
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(14.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Friend", "Partner", "Family", "Colleague", "Me").forEach { label ->
            val on = relation == label
            Text(
                label,
                color = if (on) BlobDeep else StageText,
                fontSize = 13.sp,
                fontFamily = GeneFontFamily,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) BlobCream else Color.White.copy(alpha = 0.07f))
                    .clickable { onRelation(label) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoryStep(
    mood: GeneBlobMood,
    personName: String,
    selected: MemoryKind?,
    text: String,
    onSelect: (MemoryKind) -> Unit,
    onText: (String) -> Unit
) {
    GeneBlob(modifier = Modifier.size(128.dp), mood = mood, interactive = true)
    Text("Memories have a shape", color = StageText, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = GeneFontFamily, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text("Pick a type, then try writing one about $personName. This is the same composer you’ll use later.", color = StageDim, fontSize = 14.sp, fontFamily = GeneFontFamily, textAlign = TextAlign.Center, lineHeight = 20.sp)
    Spacer(Modifier.height(16.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MemoryKinds.forEach { kind ->
            val on = selected?.type == kind.type
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) BlobCream else Color.White.copy(alpha = 0.07f))
                    .clickable { onSelect(kind) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(kind.icon, null, tint = if (on) BlobDeep else StageText, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(kind.label, color = if (on) BlobDeep else StageText, fontSize = 13.sp, fontFamily = GeneFontFamily, fontWeight = FontWeight.Medium)
            }
        }
    }
    AnimatedVisibility(visible = selected != null) {
        Column {
            Spacer(Modifier.height(10.dp))
            Text(selected?.hint.orEmpty(), color = StageDim, fontSize = 13.sp, fontFamily = GeneFontFamily, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(FieldShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), FieldShape)
                    .padding(16.dp)
            ) {
                if (text.isEmpty()) {
                    Text("Write it in your words…", color = StageDim.copy(alpha = 0.7f), fontSize = 15.sp, fontFamily = GeneFontFamily)
                }
                BasicTextField(
                    value = text,
                    onValueChange = onText,
                    textStyle = TextStyle(color = StageText, fontSize = 15.sp, fontFamily = GeneFontFamily, lineHeight = 22.sp),
                    cursorBrush = SolidColor(BlobCream),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun AskStep(
    mood: GeneBlobMood,
    personName: String,
    memoryText: String,
    asked: Boolean,
    thinking: Boolean,
    reply: String?,
    onAsk: (String) -> Unit
) {
    GeneBlob(modifier = Modifier.size(140.dp), mood = mood, interactive = true)
    Text("Then you can ask", color = StageText, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = GeneFontFamily, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text("Gene searches your notes on-device and answers from those — not from guessed inner lives.", color = StageDim, fontSize = 14.sp, fontFamily = GeneFontFamily, textAlign = TextAlign.Center, lineHeight = 20.sp)
    Spacer(Modifier.height(18.dp))
    val prompts = remember(personName, memoryText) {
        listOf(
            "What should I remember about $personName?",
            "What pattern is showing up?",
            "How should I show up next time?"
        )
    }
    prompts.forEach { prompt ->
        Text(
            prompt,
            color = StageText,
            fontSize = 14.sp,
            fontFamily = GeneFontFamily,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .clickable(enabled = !thinking) { onAsk(prompt) }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
    AnimatedVisibility(visible = thinking || reply != null) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                if (thinking) "Looking through what you captured…" else reply.orEmpty(),
                color = if (thinking) BlobCream else StageText,
                fontSize = 15.sp,
                fontFamily = GeneFontFamily,
                lineHeight = 22.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(BlobCream.copy(alpha = 0.1f))
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun PrivacyStep(selected: List<Int>, mood: GeneBlobMood, onToggle: (Int) -> Unit) {
    GeneBlob(modifier = Modifier.size(120.dp), mood = mood, interactive = true)
    Text("A few promises", color = StageText, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = GeneFontFamily, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text("Tap each one so it sticks. Gene is private context — not surveillance.", color = StageDim, fontSize = 14.sp, fontFamily = GeneFontFamily, textAlign = TextAlign.Center, lineHeight = 20.sp)
    Spacer(Modifier.height(18.dp))
    PrivacyCard(0, Icons.Outlined.Lock, "Local first", "People, notes, and chats live on this device. Remote thinking is opt-in.", selected, onToggle)
    Spacer(Modifier.height(10.dp))
    PrivacyCard(1, Icons.Outlined.People, "Ask before capturing", "Get consent before recording or transcribing someone else.", selected, onToggle)
    Spacer(Modifier.height(10.dp))
    PrivacyCard(2, Icons.Outlined.Lightbulb, "Replies are hypotheses", "Treat answers as working notes, not certainty about someone else’s mind.", selected, onToggle)
}

@Composable
private fun PrivacyCard(
    id: Int,
    icon: ImageVector,
    title: String,
    body: String,
    selected: List<Int>,
    onToggle: (Int) -> Unit
) {
    IdeaCard(id, icon, title, body, selected, onToggle)
}

@Composable
private fun LookStep(mood: GeneBlobMood, preferDark: Boolean, onPick: (Boolean) -> Unit) {
    GeneBlob(modifier = Modifier.size(132.dp), mood = mood, interactive = true)
    Text("How should Gene look?", color = StageText, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = GeneFontFamily, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text("You can change this anytime in Settings.", color = StageDim, fontSize = 14.sp, fontFamily = GeneFontFamily, textAlign = TextAlign.Center)
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LookTile("Dark", "Warm charcoal", NotionDarkBg, StageText, preferDark, Modifier.weight(1f)) { onPick(true) }
        LookTile("Light", "Paper cream", NotionLightBg, Color(0xFF37352F), !preferDark, Modifier.weight(1f)) { onPick(false) }
    }
}

@Composable
private fun LookTile(
    title: String,
    subtitle: String,
    bg: Color,
    fg: Color,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(bg)
            .border(2.dp, if (selected) BlobCream else Color.Transparent, CardShape)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .height(120.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(BlobCream))
        Column {
            Text(title, color = fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = GeneFontFamily)
            Text(subtitle, color = fg.copy(alpha = 0.6f), fontSize = 12.sp, fontFamily = GeneFontFamily)
        }
    }
}

@Composable
private fun ReadyStep(
    mood: GeneBlobMood,
    personName: String,
    savedMemory: Boolean,
    asked: Boolean,
    preferDark: Boolean
) {
    GeneBlob(modifier = Modifier.size(168.dp), mood = mood, interactive = true)
    Text("You’re set.", color = StageText, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = GeneFontFamily, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text("Add people, capture in the dock, and ask from a profile. Gene grows as you write.", color = StageDim, fontSize = 15.sp, fontFamily = GeneFontFamily, textAlign = TextAlign.Center, lineHeight = 22.sp)
    Spacer(Modifier.height(20.dp))
    val recap = buildList {
        if (personName.isNotBlank()) add("Remembering $personName")
        if (savedMemory) add("First memory saved")
        if (asked) add("Tried asking")
        add(if (preferDark) "Dark look" else "Light look")
    }
    recap.forEach { line ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Check, null, tint = BlobCream, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(line, color = StageText, fontSize = 14.sp, fontFamily = GeneFontFamily)
        }
    }
}
