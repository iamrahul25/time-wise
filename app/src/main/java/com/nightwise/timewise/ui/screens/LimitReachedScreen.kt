package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.friction.Awareness
import com.nightwise.timewise.data.friction.AwarenessItem
import com.nightwise.timewise.data.friction.AwarenessKind
import com.nightwise.timewise.data.friction.Dream
import com.nightwise.timewise.data.friction.FrictionContent
import com.nightwise.timewise.data.friction.FrictionLogic
import androidx.compose.material.icons.rounded.VolunteerActivism
import com.nightwise.timewise.data.friction.FrictionStep
import com.nightwise.timewise.data.friction.FrictionTodo
import com.nightwise.timewise.data.friction.LovedPhoto
import com.nightwise.timewise.data.friction.MotivationTrack
import com.nightwise.timewise.data.limits.LimitLogic
import com.nightwise.timewise.ui.components.AppIcon
import com.nightwise.timewise.ui.components.FocusTextField
import com.nightwise.timewise.ui.components.IconTile
import com.nightwise.timewise.ui.components.LockedPhotoCover
import com.nightwise.timewise.ui.components.OutlineButton
import com.nightwise.timewise.ui.components.PinDialog
import com.nightwise.timewise.ui.components.PrimaryButton
import com.nightwise.timewise.ui.components.TrackArt
import com.nightwise.timewise.ui.components.TrackPlayer
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.rememberAppInfo
import com.nightwise.timewise.ui.components.rememberStoredImage
import com.nightwise.timewise.ui.components.rememberTrackPlayer
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.formatLimitMinutes
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.Blue
import com.nightwise.timewise.ui.theme.BlueTint
import com.nightwise.timewise.ui.theme.CardBorder
import com.nightwise.timewise.ui.theme.Cream
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Purple
import com.nightwise.timewise.ui.theme.PurpleTint
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.RedTint
import com.nightwise.timewise.ui.theme.Slate
import com.nightwise.timewise.ui.theme.StepperBg
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.random.Random

private const val INTRO = -1

/** Why the steps are showing: extra time for a blocked app, or someone trying to loosen a limit they set. */
enum class FlowPurpose { EXTRA_TIME, TURN_OFF, REMOVE, RAISE, BEDTIME_EXTRA, BEDTIME_OFF, BEDTIME_EDIT }

private data class FlowTexts(
    val title: String,
    val body: String,
    val keepLabel: String,
    val continueLabel: String,
    val header: String,
    val finishLabel: String,
    val grantsTime: Boolean
)

private fun flowTexts(
    purpose: FlowPurpose,
    appName: String,
    usedMillis: Long,
    limitMinutes: Int?,
    extraMinutes: Int,
    cooldownMinutes: Int
): FlowTexts = when (purpose) {
    FlowPurpose.EXTRA_TIME -> FlowTexts(
        title = "Time's up",
        body = buildString {
            append("You've spent ${formatDuration(usedMillis)} on $appName today")
            if (limitMinutes != null) {
                append(". Your limit is ${formatLimitMinutes(limitMinutes)}")
                if (extraMinutes > 0) append(", plus ${extraMinutes}m extra you already earned")
            }
            append(".")
        },
        keepLabel = "Close $appName",
        continueLabel = "I need ${LimitLogic.EXTRA_MINUTES} more minutes",
        header = "$appName · ${LimitLogic.EXTRA_MINUTES} more minutes",
        finishLabel = "Unlock ${LimitLogic.EXTRA_MINUTES} more minutes",
        grantsTime = true
    )
    FlowPurpose.TURN_OFF -> FlowTexts(
        title = "Turn off daily limits?",
        body = "You set these limits as a promise to yourself. Turning them all off takes the same steps as extra time, with a $cooldownMinutes min wait.",
        keepLabel = "Keep my limits",
        continueLabel = "I still want to turn them off",
        header = "Turning off daily limits",
        finishLabel = "Turn off limits",
        grantsTime = false
    )
    FlowPurpose.REMOVE -> FlowTexts(
        title = "Remove the $appName limit?",
        body = buildString {
            append("You promised yourself")
            if (limitMinutes != null) append(" ${formatLimitMinutes(limitMinutes)} a day on $appName") else append(" a limit on $appName")
            append(". Removing it takes the steps and a $cooldownMinutes min wait.")
        },
        keepLabel = "Keep the limit",
        continueLabel = "I still want to remove it",
        header = "Removing the $appName limit",
        finishLabel = "Remove limit",
        grantsTime = false
    )
    FlowPurpose.RAISE -> FlowTexts(
        title = "Raise the $appName limit?",
        body = buildString {
            append("You promised yourself")
            if (limitMinutes != null) append(" ${formatLimitMinutes(limitMinutes)} a day on $appName") else append(" a limit on $appName")
            append(". Raising it takes the steps and a $cooldownMinutes min wait.")
        },
        keepLabel = "Keep my limit",
        continueLabel = "I still want to raise it",
        header = "Raising the $appName limit",
        finishLabel = "Raise limit",
        grantsTime = false
    )
    FlowPurpose.BEDTIME_EXTRA -> FlowTexts(
        title = "Bedtime focus",
        body = "$appName is blocked while you're meant to be asleep. Finish the steps to get ${LimitLogic.EXTRA_MINUTES} more minutes with it.",
        keepLabel = "Close $appName",
        continueLabel = "I need ${LimitLogic.EXTRA_MINUTES} more minutes",
        header = "$appName · ${LimitLogic.EXTRA_MINUTES} more minutes",
        finishLabel = "Unlock ${LimitLogic.EXTRA_MINUTES} more minutes",
        grantsTime = true
    )
    FlowPurpose.BEDTIME_OFF -> FlowTexts(
        title = "Turn off bedtime focus?",
        body = "You chose these apps to stay closed during your sleep. Turning bedtime focus off takes the same steps as extra time, with a $cooldownMinutes min wait.",
        keepLabel = "Keep bedtime focus",
        continueLabel = "I still want to turn it off",
        header = "Turning off bedtime focus",
        finishLabel = "Turn off bedtime focus",
        grantsTime = false
    )
    FlowPurpose.BEDTIME_EDIT -> FlowTexts(
        title = "Change bedtime focus?",
        body = "Changing your bedtime, wake-up time or blocked apps takes the same steps as extra time, with a $cooldownMinutes min wait.",
        keepLabel = "Keep it as it is",
        continueLabel = "I still want to change it",
        header = "Changing bedtime focus",
        finishLabel = "Save changes",
        grantsTime = false
    )
}

private fun clock(totalSeconds: Int): String =
    String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)

/**
 * The steps that stand between the user and loosening a limit: extra time for a blocked app, or
 * turning off, removing or raising a limit. A cooldown runs from the first tap and shows on every
 * step; the last step waits for it to end. [packageName] is null when the change covers every app.
 */
@Composable
fun LimitReachedScreen(
    packageName: String?,
    usedMillis: Long,
    limitMinutes: Int?,
    extraMinutes: Int,
    cooldownMinutes: Int,
    content: FrictionContent,
    flowKey: Int,
    photoFile: (LovedPhoto) -> File,
    artFile: (MotivationTrack) -> File?,
    onCheckPin: (String) -> Boolean,
    onCloseApp: () -> Unit,
    onGranted: () -> Unit,
    purpose: FlowPurpose = FlowPurpose.EXTRA_TIME
) {
    val info = packageName?.let { rememberAppInfo(it) }
    val appName = info?.label ?: "this app"
    val texts = flowTexts(purpose, appName, usedMillis, limitMinutes, extraMinutes, cooldownMinutes)
    val bedtime = purpose.isBedtime()
    val steps = remember(flowKey) { FrictionLogic.steps(content, all = bedtime) }
    var stage by rememberSaveable(flowKey) { mutableIntStateOf(INTRO) }
    var ready by remember(flowKey, stage) { mutableStateOf(false) }
    val phrase = rememberSaveable(flowKey) { FrictionLogic.randomPhrase() }
    val code = rememberSaveable(flowKey, cooldownMinutes) { FrictionLogic.randomCode(FrictionLogic.codeLength(cooldownMinutes)) }
    val awareness = remember(flowKey) { Awareness.pickOneOfEach() }
    val onReady: (Boolean) -> Unit = { ready = it }

    // The wait starts when the user asks to go on and is measured on the wall clock, so it can't be skipped.
    var startedAt by rememberSaveable(flowKey) { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt) {
        while (startedAt != 0L) {
            now = System.currentTimeMillis()
            delay(500L)
        }
    }
    val waitSeconds = if (startedAt == 0L) cooldownMinutes * 60 else {
        val leftMs = (cooldownMinutes * 60_000L - (now - startedAt)).coerceAtLeast(0L)
        ((leftMs + 999L) / 1000L).toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        if (stage == INTRO) {
            Intro(
                appIcon = {
                    if (packageName != null) AppIcon(info, size = 64.dp)
                    else IconTile(icon = Icons.Rounded.Shield, tint = Forest, background = Mint, size = 64.dp, iconSize = 34.dp)
                },
                texts = texts,
                showHourglass = purpose == FlowPurpose.EXTRA_TIME || purpose == FlowPurpose.BEDTIME_EXTRA,
                cooldownMinutes = cooldownMinutes,
                stepCount = steps.size,
                onCloseApp = onCloseApp,
                onWantMore = {
                    startedAt = System.currentTimeMillis()
                    now = startedAt
                    stage = 0
                }
            )
            return@Column
        }

        val step = steps[stage]
        val last = stage == steps.lastIndex
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (packageName != null) AppIcon(info, size = 28.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = texts.header, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "Step ${stage + 1} of ${steps.size}", color = Slate, fontSize = 12.sp)
            }
            Text(
                text = if (waitSeconds > 0) "Wait ${clock(waitSeconds)}" else "Wait over",
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (waitSeconds > 0) AmberTint else Mint)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                color = if (waitSeconds > 0) Amber else Forest,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        StepProgress(total = steps.size, current = stage, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            key(stage) {
                if (!FrictionLogic.isReady(step, content)) {
                    PromptStep(step, onReady)
                } else when (step) {
                    FrictionStep.PHRASE -> PhraseStep(phrase, code, onReady)
                    FrictionStep.PEOPLE -> PeopleStep(content.photos, photoFile, onCheckPin, onReady)
                    FrictionStep.FAITH -> FaithStep(content.faithPhotos, photoFile, onReady)
                    FrictionStep.DREAM -> DreamStep(content.dreams, onReady)
                    FrictionStep.AWARENESS -> AwarenessStep(awareness, onReady)
                    FrictionStep.TODOS -> TodosStep(content.openTodos, texts.grantsTime, onReady)
                    FrictionStep.MUSIC -> MusicStep(content.tracks, artFile, onReady)
                }
            }
        }

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (last && ready && waitSeconds > 0) {
                Text(
                    text = "All steps done. Stay here until the wait ends. It grows by ${LimitLogic.COOLDOWN_STEP_MINUTES} min each time, so the urge has time to pass.",
                    color = Amber,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            PrimaryButton(
                text = when {
                    !last -> "Next"
                    waitSeconds > 0 -> "Wait ${clock(waitSeconds)}"
                    else -> texts.finishLabel
                },
                onClick = { if (last) onGranted() else stage++ },
                enabled = ready && (!last || waitSeconds == 0),
                showArrow = !last
            )
            Text(
                text = texts.keepLabel,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onCloseApp)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = Forest,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun Intro(
    appIcon: @Composable () -> Unit,
    texts: FlowTexts,
    showHourglass: Boolean,
    cooldownMinutes: Int,
    stepCount: Int,
    onCloseApp: () -> Unit,
    onWantMore: () -> Unit
) {
    val quote = remember { Awareness.byKind(AwarenessKind.QUOTE).random() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(contentAlignment = Alignment.BottomEnd) {
            appIcon()
            if (showHourglass) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Red),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.HourglassBottom, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        Text(text = texts.title, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            text = texts.body,
            color = Slate,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AmberTint)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "Why the pause?", color = Amber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "You set this limit to protect your time. Most urges fade within a few minutes, so a short wait is often all it takes to change your mind. Each time you ask today the wait grows by ${LimitLogic.COOLDOWN_STEP_MINUTES} minutes.",
                color = Amber,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusCard()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "“${quote.text}”", color = Ink, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
            Text(text = "— ${quote.source}", color = Mist, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        PrimaryButton(text = texts.keepLabel, onClick = onCloseApp)
        OutlineButton(onClick = onWantMore) {
            Text(text = texts.continueLabel, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = "$stepCount short steps and a $cooldownMinutes min wait come first.",
            color = Mist,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepProgress(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index <= current) Forest else StepperBg)
            )
        }
    }
}

@Composable
private fun StepIntro(icon: ImageVector, tint: Color, background: Color, title: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon = icon, tint = tint, background = background, size = 44.dp, iconSize = 24.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(text = text, color = Slate, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun StatusText(text: String, done: Boolean) {
    Text(
        text = text,
        color = if (done) Forest else Slate,
        fontSize = 13.sp,
        fontWeight = if (done) FontWeight.SemiBold else FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Seconds left of [seconds], counting down only while [running]. */
@Composable
private fun rememberCountdown(seconds: Int, running: Boolean = true): Int {
    var remaining by rememberSaveable { mutableIntStateOf(seconds) }
    LaunchedEffect(running) {
        while (running && remaining > 0) {
            delay(1_000L)
            remaining--
        }
    }
    return remaining
}

@Composable
private fun PhraseStep(phrase: String, code: String, onReady: (Boolean) -> Unit) {
    var typed by rememberSaveable { mutableStateOf("") }
    var typedCode by rememberSaveable { mutableStateOf("") }
    val matches = FrictionLogic.matchesPhrase(typed, phrase)
    val typo = FrictionLogic.hasTypo(typed, phrase)
    val correct = FrictionLogic.correctPrefixLength(typed, phrase)
    val hint = FrictionLogic.nextWordHint(typed, phrase)
    val codeMatches = FrictionLogic.matchesCode(typedCode, code)
    val codeTypo = typedCode.isNotEmpty() && !code.startsWith(typedCode)
    LaunchedEffect(matches, codeMatches) { onReady(matches && codeMatches) }

    StepIntro(
        Icons.Rounded.Keyboard, Red, RedTint, "Type it out",
        "Type this sentence, then the code below. Tap the suggested word if you want a hand."
    )
    Text(
        text = "“$phrase”",
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        color = Ink,
        fontSize = 19.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.SemiBold
    )
    FocusTextField(
        value = typed,
        onValueChange = { typed = it },
        placeholder = "Type the sentence here",
        singleLine = false,
        isError = typo,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done
        )
    )
    if (hint != null) {
        Text(
            text = "Next word: ${hint.word}  ›",
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Mint)
                .clickable { typed = hint.typedAfter }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            color = Forest,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    StatusText(
        text = when {
            matches -> "Done. Was it worth saying?"
            typo -> "Something doesn't match. Check the words above."
            else -> "$correct of ${phrase.length} characters"
        },
        done = matches
    )

    Text(text = "Now this code, ${code.length} characters", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Text(
        text = code,
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        color = Ink,
        fontSize = 20.sp,
        lineHeight = 30.sp,
        letterSpacing = 1.5.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold
    )
    FocusTextField(
        value = typedCode,
        onValueChange = { typedCode = it },
        placeholder = "Type the code exactly",
        singleLine = false,
        isError = codeTypo,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done
        )
    )
    StatusText(
        text = when {
            codeMatches -> "Code matches."
            codeTypo -> "Not quite. Letters are case sensitive, so check each character."
            else -> "${typedCode.length} of ${code.length} characters"
        },
        done = codeMatches
    )
}

@Composable
private fun FaithStep(
    photos: List<LovedPhoto>,
    photoFile: (LovedPhoto) -> File,
    onReady: (Boolean) -> Unit
) {
    val pagerState = rememberPagerState { photos.size }
    var visited by remember { mutableStateOf(setOf(0)) }
    val seconds = rememberCountdown(FrictionLogic.FAITH_SECONDS)
    LaunchedEffect(pagerState.currentPage) { visited = visited + pagerState.currentPage }
    val allSeen = visited.size >= photos.size
    LaunchedEffect(allSeen, seconds) { onReady(allSeen && seconds == 0) }

    StepIntro(Icons.Rounded.VolunteerActivism, Purple, PurpleTint, "Your faith", "Take a moment with what you believe in. Look at every one of them.")
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f),
        pageSpacing = 12.dp
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(NeutralChip)
        ) {
            rememberStoredImage(photoFile(photos[page]), maxPx = 1200)?.let { image ->
                Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
    if (photos.size > 1) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            photos.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (index == pagerState.currentPage) Forest else CardBorder)
                )
            }
        }
    }
    StatusText(
        text = when {
            !allSeen -> "Swipe to see all ${photos.size}"
            seconds > 0 -> "Stay with them for ${seconds}s"
            else -> "Hold on to what you believe in."
        },
        done = allSeen && seconds == 0
    )
}

@Composable
private fun PeopleStep(
    photos: List<LovedPhoto>,
    photoFile: (LovedPhoto) -> File,
    onCheckPin: (String) -> Boolean,
    onReady: (Boolean) -> Unit
) {
    val pagerState = rememberPagerState { photos.size }
    var visited by remember { mutableStateOf(setOf(0)) }
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var askPin by remember { mutableStateOf(false) }
    val seconds = rememberCountdown(FrictionLogic.PEOPLE_SECONDS)
    LaunchedEffect(pagerState.currentPage) { visited = visited + pagerState.currentPage }
    val allSeen = visited.size >= photos.size
    LaunchedEffect(allSeen, seconds) { onReady(allSeen && seconds == 0) }

    StepIntro(Icons.Rounded.Favorite, Red, RedTint, "Your people", "Who are you doing this for? Look at every one of them.")
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f),
        pageSpacing = 12.dp
    ) { page ->
        val photo = photos[page]
        val hidden = photo.locked && !unlocked
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(NeutralChip)
        ) {
            if (hidden) {
                LockedPhotoCover(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { askPin = true },
                    label = "Tap and enter your PIN to see"
                )
            } else {
                rememberStoredImage(photoFile(photo), maxPx = 1200)?.let { image ->
                    Image(bitmap = image, contentDescription = photo.caption, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            if (photo.caption.isNotBlank() && !hidden) {
                Text(
                    text = photo.caption,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
    if (photos.size > 1) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            photos.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (index == pagerState.currentPage) Forest else CardBorder)
                )
            }
        }
    }
    StatusText(
        text = when {
            !allSeen -> "Swipe to see all ${photos.size}"
            seconds > 0 -> "Stay with them for ${seconds}s"
            else -> "They're the reason. Don't let an app take your time with them."
        },
        done = allSeen && seconds == 0
    )

    if (askPin) {
        PinDialog(
            title = "Locked photo",
            message = "Enter your photo PIN to see your locked photos.",
            onDismiss = { askPin = false },
            onSubmit = { pin -> onCheckPin(pin).also { if (it) unlocked = true } }
        )
    }
}

@Composable
private fun FlowPurpose.isBedtime(): Boolean =
    this == FlowPurpose.BEDTIME_EXTRA || this == FlowPurpose.BEDTIME_OFF || this == FlowPurpose.BEDTIME_EDIT

private val PROMPT_TEXT = mapOf(
    FrictionStep.PEOPLE to "Write the name of one person you want to be present for.",
    FrictionStep.FAITH to "Write one thing you believe in or are grateful for.",
    FrictionStep.DREAM to "Write one thing you want to achieve in your life.",
    FrictionStep.TODOS to "Write the one thing you'll do once this time is over.",
    FrictionStep.MUSIC to "Write the name of a song that gets you moving."
)

/**
 * Stands in for a step the user hasn't set up yet, so a bedtime unlock still goes through every step.
 * It asks for one written sentence, which is enough to continue.
 */
@Composable
private fun PromptStep(step: FrictionStep, onReady: (Boolean) -> Unit) {
    var typed by rememberSaveable(step) { mutableStateOf("") }
    val done = typed.isNotBlank()
    LaunchedEffect(done) { onReady(done) }

    StepIntro(Icons.Rounded.Lightbulb, Purple, PurpleTint, step.title, PROMPT_TEXT[step].orEmpty())
    FocusTextField(
        value = typed,
        onValueChange = { typed = it },
        placeholder = "Write it here",
        singleLine = false,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )
    Text(
        text = if (done) "Good. Continue when you're ready." else "Write one sentence to continue.",
        color = if (done) Forest else Slate,
        fontSize = 12.sp
    )
}

@Composable
private fun DreamStep(dreams: List<Dream>, onReady: (Boolean) -> Unit) {
    var typed by rememberSaveable { mutableStateOf("") }
    val matched = FrictionLogic.matchingDream(typed, dreams)
    LaunchedEffect(matched) { onReady(matched != null) }

    StepIntro(Icons.Rounded.Flag, Purple, PurpleTint, "Your dreams", "This is what you said you want from life. Type one of them to continue.")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        dreams.forEach { dream ->
            val isMatch = dream.id == matched?.id
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isMatch) Forest else Purple)
                )
                Text(
                    text = dream.text,
                    color = if (isMatch) Forest else Ink,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
    FocusTextField(
        value = typed,
        onValueChange = { typed = it },
        placeholder = "Type one dream exactly",
        singleLine = false,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done)
    )
    StatusText(
        text = if (matched != null) "Every minute you don't scroll goes to this." else "Type it word for word",
        done = matched != null
    )
}

@Composable
private fun AwarenessStep(items: List<AwarenessItem>, onReady: (Boolean) -> Unit) {
    val seconds = rememberCountdown(FrictionLogic.AWARENESS_SECONDS)
    LaunchedEffect(seconds) { onReady(seconds == 0) }

    StepIntro(Icons.Rounded.Lightbulb, Amber, AmberTint, "The real cost", "Read these before you go back.")
    items.forEach { AwarenessCard(it) }
    StatusText(text = if (seconds > 0) "Keep reading: ${seconds}s" else "Now you know what it costs.", done = seconds == 0)
}

@Composable
private fun TodosStep(todos: List<FrictionTodo>, grantsTime: Boolean, onReady: (Boolean) -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = todos.firstOrNull { it.id == selectedId }
    LaunchedEffect(selected) { onReady(selected != null) }

    StepIntro(
        Icons.Rounded.Checklist, Blue, BlueTint, "Your to-dos",
        if (grantsTime) "These are still waiting. Pick the one you'll do when these ${LimitLogic.EXTRA_MINUTES} minutes are up."
        else "These are still waiting. Pick the one you'll do with your time instead."
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(vertical = 6.dp)
    ) {
        todos.forEach { todo ->
            val isSelected = todo.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedId = todo.id }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (isSelected) Forest else CardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Forest))
                }
                Text(text = todo.text, color = Ink, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
            }
        }
    }
    StatusText(
        text = selected?.let {
            if (grantsTime) "Next up, in ${LimitLogic.EXTRA_MINUTES} minutes: ${it.text}" else "Next up: ${it.text}"
        } ?: "Choose one",
        done = selected != null
    )
}

@Composable
private fun MusicStep(tracks: List<MotivationTrack>, artFile: (MotivationTrack) -> File?, onReady: (Boolean) -> Unit) {
    val player = rememberTrackPlayer()
    var index by rememberSaveable { mutableIntStateOf(Random.nextInt(tracks.size)) }
    SideEffect {
        player.onCompletion = {
            if (tracks.size == 1) player.play(tracks[0]) else index = (index + 1) % tracks.size
        }
    }
    LaunchedEffect(index) { player.play(tracks[index]) }
    val seconds = rememberCountdown(FrictionLogic.MUSIC_SECONDS, running = player.isPlaying || player.failed)
    LaunchedEffect(seconds) { onReady(seconds == 0) }
    val track = tracks[index]

    StepIntro(Icons.Rounded.MusicNote, Forest, Mint, "Your music", "Listen to what gets you going.")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TrackArt(artFile(track), size = 200.dp)
        Text(text = track.title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        track.artist?.let { Text(text = it, color = Slate, fontSize = 13.sp) }
        SeekBar(player)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (tracks.size > 1) {
                PlayerButton(Icons.Rounded.SkipPrevious, "Previous") { index = (index - 1 + tracks.size) % tracks.size }
            }
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Forest)
                    .clickable(onClick = player::toggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (player.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            if (tracks.size > 1) {
                PlayerButton(Icons.Rounded.SkipNext, "Next track") { index = (index + 1) % tracks.size }
            }
        }
    }
    StatusText(
        text = when {
            player.failed && seconds > 0 -> "This file can't be played any more. Remove it in setup. ${seconds}s"
            seconds > 0 -> "Listen for ${seconds}s"
            else -> "Feel that? Use it for something real."
        },
        done = seconds == 0
    )
}

/** Drag to jump forward or backward in the playing track. */
@Composable
private fun SeekBar(player: TrackPlayer) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(player.isPlaying) {
        while (player.isPlaying) {
            player.refreshPosition()
            delay(500L)
        }
    }
    val duration = player.durationMs.coerceAtLeast(1)
    val shown = (dragging ?: player.positionMs.toFloat()).coerceIn(0f, duration.toFloat())
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = shown,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { player.seekTo(it.toInt()) }
                dragging = null
            },
            valueRange = 0f..duration.toFloat(),
            enabled = player.durationMs > 0 && !player.failed,
            colors = SliderDefaults.colors(
                thumbColor = Forest,
                activeTrackColor = Forest,
                inactiveTrackColor = StepperBg,
                disabledThumbColor = CardBorder,
                disabledActiveTrackColor = CardBorder,
                disabledInactiveTrackColor = StepperBg
            )
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = clock(shown.toInt() / 1000), color = Slate, fontSize = 12.sp)
            Text(text = clock(player.durationMs / 1000), color = Slate, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlayerButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(NeutralChip)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = Ink, modifier = Modifier.size(24.dp))
    }
}
