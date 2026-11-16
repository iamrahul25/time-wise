package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SectionTitle
import com.nightwise.timewise.ui.theme.CardBorder
import com.nightwise.timewise.ui.theme.Cream
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nightwise.timewise.data.bedtime.SleepStatus
import com.nightwise.timewise.data.bedtime.SleepStatusLogic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.bedtime.BedtimeInsights
import com.nightwise.timewise.data.bedtime.BedtimeWindows
import com.nightwise.timewise.data.bedtime.NightAppUsage
import com.nightwise.timewise.data.bedtime.PhoneDownSummary
import com.nightwise.timewise.ui.components.AppIcon
import com.nightwise.timewise.ui.components.IconTile
import com.nightwise.timewise.ui.components.SegmentedToggle
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.rememberAppInfo
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.formatMinutesOfDay
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.Purple
import com.nightwise.timewise.ui.theme.PurpleTint
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.Slate
import com.nightwise.timewise.ui.theme.StepperBg

private const val DAY_MINUTES = 24 * 60
private const val ON_TIME_MINUTES = 15
private const val LISTED_APPS = 5

/**
 * The sleep status (Early Bird to Ghost) from when the phone usually goes down, as a slim card.
 * Tapping it opens every status with its icon, color and range. Before [SleepStatusLogic.MIN_NIGHTS]
 * nights are recorded it only shows the average, so one bad night doesn't earn a label.
 */
@Composable
fun TypicalBedtimeCard(summary: PhoneDownSummary?, bedtimeMinutes: Int) {
    if (summary == null) {
        BedtimeEmptyCard()
        return
    }
    val status = SleepStatusLogic.statusFor(summary)
    if (status == null) {
        BedtimeLearningCard(summary, bedtimeMinutes)
    } else {
        var showAll by rememberSaveable { mutableStateOf(false) }
        SleepStatusCompactCard(status, summary) { showAll = true }
        if (showAll) SleepStatusDialog(status, summary, bedtimeMinutes) { showAll = false }
    }
}

@Composable
private fun BedtimeEmptyCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "Your sleep status", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = "Shows up once ${SleepStatusLogic.MIN_NIGHTS} nights are recorded. Check back tomorrow morning.",
            color = Slate,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun BedtimeLearningCard(summary: PhoneDownSummary, bedtimeMinutes: Int) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.Bedtime, tint = Purple, background = PurpleTint, size = 36.dp, iconSize = 20.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "Phone usually goes down", color = Slate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = formatMinutesOfDay(context, summary.averageMinutes),
                    color = Ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            OffsetPill(BedtimeInsights.offsetFromBedtime(summary.averageMinutes, bedtimeMinutes))
        }
        Text(
            text = "Still learning · ${summary.nights} of ${SleepStatusLogic.MIN_NIGHTS} nights before your sleep status shows.",
            color = Slate,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

/** The status at a glance. Tap for every level. */
@Composable
private fun SleepStatusCompactCard(status: SleepStatus, summary: PhoneDownSummary, onClick: () -> Unit) {
    val context = LocalContext.current
    val (light, dark) = sleepStatusColors(status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(light, dark)))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SleepStatusAvatar(status, size = 44.dp, onGradient = true)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "SLEEP STATUS",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(text = status.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Phone goes down around ${formatMinutesOfDay(context, summary.averageMinutes)} · tap for all levels",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = "See all sleep statuses",
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp)
        )
    }
}

/** Round status avatar: gradient with the status icon. [onGradient] is for use on the status's own gradient, [dimmed] for levels that aren't yours. */
@Composable
private fun SleepStatusAvatar(status: SleepStatus, size: Dp, onGradient: Boolean = false, dimmed: Boolean = false) {
    val (light, dark) = sleepStatusColors(status)
    val brush = when {
        onGradient -> SolidColor(Color.White.copy(alpha = 0.18f))
        dimmed -> Brush.linearGradient(listOf(light.copy(alpha = 0.5f), dark.copy(alpha = 0.5f)))
        else -> Brush.linearGradient(listOf(light, dark))
    }
    Box(
        modifier = Modifier
            .size(size)
            .background(brush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(sleepStatusIcon(status), contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

/** Your status in full, then every level with its icon, color and range, like the levels on the Profile screen. */
@Composable
private fun SleepStatusDialog(status: SleepStatus, summary: PhoneDownSummary, bedtimeMinutes: Int, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenHeader(title = "Sleep status", onBack = onDismiss)
            SleepStatusHero(status, summary, bedtimeMinutes)
            SectionTitle("All sleep statuses")
            SleepStatusLadder(current = status)
            Text(
                text = "From the average time your phone goes down over your recorded nights. " +
                    "A status shows once ${SleepStatusLogic.MIN_NIGHTS} nights are recorded.",
                color = Mist,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SleepStatusLadder(current: SleepStatus) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        SleepStatus.entries.forEachIndexed { index, level ->
            val isCurrent = level == current
            val (light, dark) = sleepStatusColors(level)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCurrent) light.copy(alpha = 0.14f) else Color.Transparent)
                    .then(if (isCurrent) Modifier.border(1.5.dp, light, RoundedCornerShape(12.dp)) else Modifier)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SleepStatusAvatar(level, size = 40.dp, dimmed = !isCurrent)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = level.title, color = if (isCurrent) dark else Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = rangeLabel(context, level), color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text(text = level.tagline, color = Mist, fontSize = 11.sp, lineHeight = 15.sp)
                }
                if (isCurrent) {
                    Text(
                        text = "YOU",
                        modifier = Modifier
                            .background(dark, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (index < SleepStatus.entries.lastIndex && !isCurrent && SleepStatus.entries[index + 1] != current) {
                Box(
                    modifier = Modifier
                        .padding(start = 60.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CardBorder)
                )
            }
        }
    }
}

/** When the phone goes down for this level, e.g. "11:30 PM – 12:30 AM". */
private fun rangeLabel(context: android.content.Context, status: SleepStatus): String {
    fun clock(nightMinutes: Int) = formatMinutesOfDay(context, nightMinutes % DAY_MINUTES)
    return when {
        status.ordinal == 0 -> "Before ${clock(status.maxNightMinutes)}"
        status.maxNightMinutes == Int.MAX_VALUE -> "After ${clock(status.minNightMinutes)}"
        else -> "${clock(status.minNightMinutes)} – ${clock(status.maxNightMinutes)}"
    }
}

@Composable
private fun SleepStatusHero(status: SleepStatus, summary: PhoneDownSummary, bedtimeMinutes: Int) {
    val context = LocalContext.current
    val (light, dark) = sleepStatusColors(status)
    val offset = BedtimeInsights.offsetFromBedtime(summary.averageMinutes, bedtimeMinutes)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(light, dark)))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "YOUR SLEEP STATUS",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(sleepStatusIcon(status), contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Text(text = status.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = status.tagline, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            StatusChip("Phone goes down", formatMinutesOfDay(context, summary.averageMinutes), Modifier.weight(1f))
            StatusChip("Vs your bedtime", offsetText(offset), Modifier.weight(1f))
        }
        Text(
            text = "Usually between ${formatMinutesOfDay(context, summary.earliestMinutes)} and " +
                "${formatMinutesOfDay(context, summary.latestMinutes)} · last ${summary.nights} nights",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StatusChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Text(text = value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun offsetText(offsetMinutes: Int): String = when {
    offsetMinutes > ON_TIME_MINUTES -> "${formatDuration(offsetMinutes * 60_000L)} later"
    offsetMinutes < -ON_TIME_MINUTES -> "${formatDuration(-offsetMinutes * 60_000L)} earlier"
    else -> "On time"
}

/** Gradient from lighter to darker, always readable under white text. */
private fun sleepStatusColors(status: SleepStatus): Pair<Color, Color> = when (status) {
    SleepStatus.EARLY_BIRD -> Color(0xFF2F7D57) to Color(0xFF1F4E3A)
    SleepStatus.SANDMAN -> Color(0xFF3B82F6) to Color(0xFF1E3A8A)
    SleepStatus.NIGHT_OWL -> Color(0xFFD99A06) to Color(0xFF8A5A04)
    SleepStatus.BAT -> Color(0xFFEA580C) to Color(0xFF9A3412)
    SleepStatus.VAMPIRE -> Color(0xFFDC2626) to Color(0xFF7F1D1D)
    SleepStatus.GHOST -> Color(0xFF475569) to Color(0xFF0F172A)
}

private fun sleepStatusIcon(status: SleepStatus): ImageVector = when (status) {
    SleepStatus.EARLY_BIRD -> Icons.Rounded.WbTwilight
    SleepStatus.SANDMAN -> Icons.Rounded.Bedtime
    SleepStatus.NIGHT_OWL -> Icons.Rounded.NightsStay
    SleepStatus.BAT -> Icons.Rounded.DarkMode
    SleepStatus.VAMPIRE -> Icons.Rounded.Bloodtype
    SleepStatus.GHOST -> Icons.Rounded.VisibilityOff
}

@Composable
private fun OffsetPill(offsetMinutes: Int) {
    val late = offsetMinutes > ON_TIME_MINUTES
    val text = when {
        offsetMinutes > ON_TIME_MINUTES -> "${formatDuration(offsetMinutes * 60_000L)} after bedtime"
        offsetMinutes < -ON_TIME_MINUTES -> "${formatDuration(-offsetMinutes * 60_000L)} before bedtime"
        else -> "On time"
    }
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (late) AmberTint else Mint)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = if (late) Amber else Forest,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

/**
 * The apps that took the most time in the 2 hours before or after the bedtime that was set,
 * averaged over the last nights. Tapping one blocks it (or opens it, if it's already blocked).
 */
@Composable
fun AroundBedtimeCard(
    windows: BedtimeWindows,
    bedtimeMinutes: Int,
    blocked: Set<String>,
    onAppClick: (String) -> Unit
) {
    val context = LocalContext.current
    var after by rememberSaveable { mutableStateOf(false) }
    val apps = if (after) windows.after else windows.before
    val startMinutes = Math.floorMod(bedtimeMinutes - if (after) 0 else BedtimeInsights.SPAN_MINUTES, DAY_MINUTES)
    val endMinutes = Math.floorMod(startMinutes + BedtimeInsights.SPAN_MINUTES, DAY_MINUTES)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SegmentedToggle(
            options = listOf("2h before bed", "2h after bed"),
            selectedIndex = if (after) 1 else 0,
            onSelect = { after = it == 1 }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${formatMinutesOfDay(context, startMinutes)} – ${formatMinutesOfDay(context, endMinutes)}",
                modifier = Modifier.weight(1f),
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (windows.nights > 0) {
                Text(
                    text = "average of ${windows.nights} ${if (windows.nights == 1) "night" else "nights"}",
                    color = Slate,
                    fontSize = 12.sp
                )
            }
        }

        when {
            windows.nights == 0 -> Text(
                text = "Needs a few finished nights of usage. Android only keeps about a week of detail, so this fills in as you go.",
                color = Slate,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            apps.isEmpty() -> Text(
                text = "No app use in this window. That's a good sign.",
                color = Slate,
                fontSize = 12.sp
            )
            else -> {
                TopAppLine(apps.first(), after)
                val max = apps.first().averageMillis
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    apps.take(LISTED_APPS).forEachIndexed { index, app ->
                        NightAppRow(
                            app = app,
                            maxMillis = max,
                            nights = windows.nights,
                            top = index == 0,
                            blocked = app.packageName in blocked,
                            onClick = { onAppClick(app.packageName) }
                        )
                    }
                }
                Text(
                    text = "Tap an app to block it during bedtime.",
                    color = Mist,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TopAppLine(top: NightAppUsage, after: Boolean) {
    val info = rememberAppInfo(top.packageName)
    Text(
        text = "${info?.label ?: "An app"} takes the most ${if (after) "after" else "before"} bedtime: " +
            "about ${formatDuration(top.averageMillis)} a night.",
        color = Slate,
        fontSize = 12.sp,
        lineHeight = 17.sp
    )
}

@Composable
private fun NightAppRow(app: NightAppUsage, maxMillis: Long, nights: Int, top: Boolean, blocked: Boolean, onClick: () -> Unit) {
    val info = rememberAppInfo(app.packageName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(info, size = 34.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = info?.label ?: "",
                    modifier = Modifier.weight(1f),
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = "${formatDuration(app.averageMillis)} a night", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(StepperBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((app.averageMillis.toFloat() / maxMillis.coerceAtLeast(1L)).coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (top) Red else Forest)
                )
            }
            Text(
                text = "Used ${app.nightsUsed} of $nights nights" + if (blocked) " · Blocked at bedtime" else "",
                color = if (blocked) Forest else Mist,
                fontSize = 11.sp,
                fontWeight = if (blocked) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
