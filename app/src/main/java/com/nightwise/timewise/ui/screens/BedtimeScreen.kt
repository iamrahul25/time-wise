package com.nightwise.timewise.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nightwise.timewise.data.FocusSettings
import com.nightwise.timewise.data.SleepEntry
import com.nightwise.timewise.data.bedtime.BedtimeInsights
import com.nightwise.timewise.data.bedtime.BedtimeLog
import com.nightwise.timewise.data.bedtime.BedtimeWindows
import com.nightwise.timewise.data.usage.UsageSnapshot
import com.nightwise.timewise.ui.components.BedtimeAppDialog
import com.nightwise.timewise.ui.components.AppIcon
import com.nightwise.timewise.ui.components.FocusTextField
import com.nightwise.timewise.ui.components.FocusToggle
import com.nightwise.timewise.ui.components.IconTile
import com.nightwise.timewise.ui.components.ScreenHeader
import com.nightwise.timewise.ui.components.SegmentedToggle
import com.nightwise.timewise.ui.components.SectionTitle
import com.nightwise.timewise.ui.components.SmallActionButton
import com.nightwise.timewise.ui.components.UsageAccessCard
import com.nightwise.timewise.ui.components.focusCard
import com.nightwise.timewise.ui.components.rememberAppInfo
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.formatMinutesOfDay
import com.nightwise.timewise.ui.formatTime
import com.nightwise.timewise.ui.relativeDayLabel
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.Blue
import com.nightwise.timewise.ui.theme.BlueTint
import com.nightwise.timewise.ui.theme.CardWhite
import com.nightwise.timewise.ui.theme.Cream
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Purple
import com.nightwise.timewise.ui.theme.PurpleTint
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.Slate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.produceState

private val REMINDER_LEADS = listOf(0, 15, 30)

@Composable
fun BedtimeScreen(
    settings: FocusSettings,
    usage: UsageSnapshot?,
    notificationsGranted: Boolean,
    usageAccessGranted: Boolean,
    accessibilityEnabled: Boolean,
    onSettingsChange: (FocusSettings) -> Unit,
    onAllowNotifications: () -> Unit,
    onAllowUsageAccess: () -> Unit,
    onEnableAccessibility: () -> Unit
) {
    val context = LocalContext.current
    val bypassGate = rememberBypassGate()
    var showAppPicker by remember { mutableStateOf(false) }
    /** Set when the picker was opened by the toggle, so saving apps also turns bedtime focus on. */
    var pickerTurnsOn by remember { mutableStateOf(false) }
    /** The app whose block dialog is open, from the picker or from the insights. */
    var dialogApp by remember { mutableStateOf<String?>(null) }

    val sleepLog = usage?.sleepLog
    val windows = remember(usage, settings.bedtimeMinutes) {
        usage?.let {
            BedtimeInsights.windows(
                sessions = it.sessions,
                bedtimeMinutes = settings.bedtimeMinutes,
                today = it.today.epochDay,
                now = System.currentTimeMillis(),
                exclude = setOf(context.packageName)
            )
        } ?: BedtimeWindows.EMPTY
    }
    val phoneDown = remember(sleepLog) { BedtimeInsights.phoneDown(sleepLog.orEmpty().map { it.phoneDownAt }) }

    // While bedtime focus is on, anything that loosens it (or changes its times) waits for the steps.
    fun changeBedtime(purpose: FlowPurpose, apply: () -> Unit) {
        if (settings.bedtimeFocusEnabled) bypassGate.bedtime(purpose, apply) else apply()
    }

    fun pickTime(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            initial / 60,
            initial % 60,
            DateFormat.is24HourFormat(context)
        ).show()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenHeader(title = "Bedtime")

            SectionTitle("Schedule")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusCard()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                TimeRow(
                    icon = Icons.Rounded.Bedtime,
                    tint = Purple,
                    tileColor = PurpleTint,
                    title = "Bedtime",
                    value = formatMinutesOfDay(context, settings.bedtimeMinutes),
                    onClick = {
                        pickTime(settings.bedtimeMinutes) { picked ->
                            changeBedtime(FlowPurpose.BEDTIME_EDIT) { onSettingsChange(settings.copy(bedtimeMinutes = picked)) }
                        }
                    }
                )
                TimeRow(
                    icon = Icons.Rounded.WbSunny,
                    tint = Amber,
                    tileColor = AmberTint,
                    title = "Wake up",
                    value = formatMinutesOfDay(context, settings.wakeMinutes),
                    onClick = {
                        pickTime(settings.wakeMinutes) { picked ->
                            changeBedtime(FlowPurpose.BEDTIME_EDIT) { onSettingsChange(settings.copy(wakeMinutes = picked)) }
                        }
                    }
                )
                Text(
                    text = "Sleep goal ${formatDuration(settings.sleepGoalMinutes * 60_000L)}",
                    color = Slate,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }

            SectionTitle("Bedtime focus")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusCard()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconTile(Icons.Rounded.DoNotDisturbOn, tint = Red, background = AmberTint, size = 36.dp, iconSize = 20.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(text = "Bedtime focus", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Blocks your chosen apps from bedtime until wake-up",
                            color = Slate,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    FocusToggle(
                        checked = settings.bedtimeFocusEnabled,
                        onCheckedChange = { on ->
                            when {
                                !on -> bypassGate.bedtime(FlowPurpose.BEDTIME_OFF) {
                                    onSettingsChange(settings.copy(bedtimeFocusEnabled = false))
                                    BedtimeLog.get(context).recordFocusOff()
                                }
                                // Nothing to block yet: pick the apps first, and the toggle turns on with them.
                                settings.bedtimeFocusApps.isEmpty() -> {
                                    pickerTurnsOn = true
                                    showAppPicker = true
                                }
                                else -> onSettingsChange(settings.copy(bedtimeFocusEnabled = true))
                            }
                        }
                    )
                }

                if (!accessibilityEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberTint)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Accessibility is off, so blocked apps can't be stopped.",
                            modifier = Modifier.weight(1f),
                            color = Ink,
                            fontSize = 12.sp
                        )
                        SmallActionButton(text = "Allow", onClick = onEnableAccessibility)
                    }
                }

                val blocked = settings.bedtimeFocusApps.sorted()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (blocked.isEmpty()) "Blocked apps" else "Blocked apps · ${blocked.size}",
                        modifier = Modifier.weight(1f),
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    SmallActionButton(
                        text = if (blocked.isEmpty()) "Choose apps" else "Edit",
                        onClick = {
                            pickerTurnsOn = false
                            showAppPicker = true
                        }
                    )
                }
                if (blocked.isEmpty()) {
                    Text(
                        text = "No blocked apps yet. Choose the ones that pull you in late at night.",
                        color = Slate,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        blocked.take(BLOCKED_PREVIEW).forEach { pkg -> BlockedAppRow(packageName = pkg) }
                        if (blocked.size > BLOCKED_PREVIEW) {
                            Text(
                                text = "+${blocked.size - BLOCKED_PREVIEW} more",
                                color = Slate,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Opening a blocked app at bedtime asks for the steps, then gives you 15 minutes with it. After that it blocks again.",
                    color = Mist,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            if (usageAccessGranted) {
                SectionTitle("Around bedtime")
                TypicalBedtimeCard(summary = phoneDown, bedtimeMinutes = settings.bedtimeMinutes)
                AroundBedtimeCard(
                    windows = windows,
                    bedtimeMinutes = settings.bedtimeMinutes,
                    blocked = settings.bedtimeFocusApps,
                    onAppClick = { dialogApp = it }
                )
            }

            SectionTitle("Reminder")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusCard()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconTile(Icons.Rounded.NotificationsActive, tint = Blue, background = BlueTint, size = 36.dp, iconSize = 20.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(text = "Bedtime reminder", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "A nudge to put the phone down", color = Slate, fontSize = 12.sp)
                    }
                    FocusToggle(
                        checked = settings.bedtimeEnabled,
                        onCheckedChange = {
                            onSettingsChange(settings.copy(bedtimeEnabled = it))
                            if (it && !notificationsGranted) onAllowNotifications()
                        }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    REMINDER_LEADS.forEach { lead ->
                        LeadChip(
                            text = if (lead == 0) "At bedtime" else "$lead min before",
                            selected = settings.reminderLeadMinutes == lead,
                            enabled = settings.bedtimeEnabled,
                            onClick = { onSettingsChange(settings.copy(reminderLeadMinutes = lead)) }
                        )
                    }
                }
                if (settings.bedtimeEnabled && !notificationsGranted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberTint)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Notifications are off, so the reminder can't show.",
                            modifier = Modifier.weight(1f),
                            color = Ink,
                            fontSize = 12.sp
                        )
                        SmallActionButton(text = "Allow", onClick = onAllowNotifications)
                    }
                }
            }

            SectionTitle("Sleep Log")
            SleepLogCard(
                sleepLog = sleepLog?.take(SLEEP_LOG_NIGHTS),
                goalMinutes = settings.sleepGoalMinutes,
                usageAccessGranted = usageAccessGranted,
                onAllowUsageAccess = onAllowUsageAccess
            )
        }

        if (showAppPicker) {
            BlockedAppsPicker(
                blocked = settings.bedtimeFocusApps,
                lastNight = windows.lastNight?.before.orEmpty().associate { it.packageName to it.totalMillis },
                weekAverage = windows.before.associate { it.packageName to it.averageMillis },
                windowLabel = run {
                    val start = Math.floorMod(settings.bedtimeMinutes - BedtimeInsights.SPAN_MINUTES, 24 * 60)
                    "${formatMinutesOfDay(context, start)} – ${formatMinutesOfDay(context, settings.bedtimeMinutes)}"
                },
                onPick = { dialogApp = it },
                onDone = {
                    showAppPicker = false
                    pickerTurnsOn = false
                }
            )
        }
        dialogApp?.let { pkg ->
            val isBlocked = pkg in settings.bedtimeFocusApps
            BedtimeAppDialog(
                packageName = pkg,
                bedtimeLabel = formatMinutesOfDay(context, settings.bedtimeMinutes),
                wakeLabel = formatMinutesOfDay(context, settings.wakeMinutes),
                averageMillis = windows.around.firstOrNull { it.packageName == pkg }?.averageMillis,
                removalNeedsSteps = settings.bedtimeFocusEnabled,
                // Each app is saved on its own. Adding is free; taking one off asks for the steps.
                onSave = {
                    if (!isBlocked) {
                        onSettingsChange(withApps(settings, settings.bedtimeFocusApps + pkg, turnOn = pickerTurnsOn))
                        pickerTurnsOn = false
                    }
                },
                onRemove = if (isBlocked) {
                    {
                        changeBedtime(FlowPurpose.BEDTIME_EDIT) {
                            onSettingsChange(withApps(settings, settings.bedtimeFocusApps - pkg))
                        }
                    }
                } else {
                    null
                },
                onDismiss = { dialogApp = null }
            )
        }
        BypassFlowHost(bypassGate)
    }
}

/**
 * Saves [apps]. With none left there is nothing to block, so bedtime focus is off; with [turnOn]
 * it comes on together with the first apps.
 */
private fun withApps(settings: FocusSettings, apps: Set<String>, turnOn: Boolean = false): FocusSettings = settings.copy(
    bedtimeFocusApps = apps,
    bedtimeFocusEnabled = (settings.bedtimeFocusEnabled || turnOn) && apps.isNotEmpty()
)

private fun shortDuration(millis: Long): String = if (millis < 60_000L) "Under 1m" else formatDuration(millis)

/** How many blocked apps the Bedtime card lists before "+N more". */
private const val BLOCKED_PREVIEW = 5

/** Nights listed in the sleep log; the averages above use every recorded night. */
private const val SLEEP_LOG_NIGHTS = 7

@Composable
private fun TimeRow(
    icon: ImageVector,
    tint: Color,
    tileColor: Color,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, tint = tint, background = tileColor, size = 36.dp, iconSize = 20.dp)
        Text(text = title, modifier = Modifier.weight(1f), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = Forest, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BlockedAppRow(packageName: String) {
    val info = rememberAppInfo(packageName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(info, size = 30.dp)
        Text(
            text = info?.label ?: "",
            modifier = Modifier.weight(1f),
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Every launchable app, the ones used most around bedtime first, to pick one to block. Like the
 * app-limit picker, tapping an app opens its own dialog where it is saved on its own.
 */
@Composable
private fun BlockedAppsPicker(
    blocked: Set<String>,
    /** Time in the 2 hours before bedtime on the last finished night, by package. */
    lastNight: Map<String, Long>,
    /** Average time a night in the 2 hours before bedtime over the last week, by package. */
    weekAverage: Map<String, Long>,
    /** The 2-hour window as clock times, e.g. "9:00 PM – 11:00 PM". */
    windowLabel: String,
    onPick: (String) -> Unit,
    onDone: () -> Unit
) {
    var showWeek by rememberSaveable { mutableStateOf(true) }
    val shown = if (showWeek) weekAverage else lastNight
    val context = LocalContext.current
    val installed by produceState<List<Pair<String, String>>?>(null) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .filter { it.activityInfo != null && it.activityInfo.packageName != context.packageName }
                .distinctBy { it.activityInfo.packageName }
                .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
        }
    }
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    // Switching view reorders the list; start from the top so the biggest app is visible.
    LaunchedEffect(showWeek) { listState.scrollToItem(0) }
    val apps = remember(installed, shown, query) {
        installed.orEmpty()
            .filter { (_, label) -> query.isBlank() || label.contains(query.trim(), ignoreCase = true) }
            .sortedWith(
                compareByDescending<Pair<String, String>> { shown[it.first] ?: 0L }
                    .thenBy { it.second.lowercase() }
            )
    }

    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp)
        ) {
            ScreenHeader(title = "Add bedtime apps", onBack = onDone)
            FocusTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search apps",
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
            )
            SegmentedToggle(
                options = listOf("Last night", "7-night average"),
                selectedIndex = if (showWeek) 1 else 0,
                onSelect = { showWeek = it == 1 },
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = when {
                    installed == null -> "Loading apps…"
                    showWeek -> "Average a night in the 2 hours before bedtime ($windowLabel), highest first"
                    else -> "Last night in the 2 hours before bedtime ($windowLabel), highest first"
                },
                color = Slate,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp)
                    .focusCard(),
                state = listState,
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(apps, key = { it.first }) { (pkg, _) ->
                    PickerRow(
                        packageName = pkg,
                        lastNightMillis = lastNight[pkg] ?: 0L,
                        weekAverageMillis = weekAverage[pkg] ?: 0L,
                        showWeek = showWeek,
                        blocked = pkg in blocked,
                        onClick = { onPick(pkg) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    packageName: String,
    lastNightMillis: Long,
    weekAverageMillis: Long,
    showWeek: Boolean,
    blocked: Boolean,
    onClick: () -> Unit
) {
    val info = rememberAppInfo(packageName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(info)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = info?.label ?: "",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // The chosen view leads, and the other one sits beside it so the week's trend shows at a glance.
            val week = "${shortDuration(weekAverageMillis)} a night"
            val last = "${shortDuration(lastNightMillis)} last night"
            Text(
                text = if (showWeek) "$week · $last" else "$last · $week",
                color = if (blocked) Forest else Slate,
                fontSize = 12.sp,
                fontWeight = if (blocked) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (blocked) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Forest),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = CardWhite, modifier = Modifier.size(16.dp))
            }
        } else {
            Text(text = "›", color = Mist, fontSize = 20.sp)
        }
    }
}

@Composable
private fun LeadChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected && enabled) Forest else NeutralChip)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = when {
            selected && enabled -> CardWhite
            enabled -> Ink
            else -> Mist
        },
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SleepLogCard(
    sleepLog: List<SleepEntry>?,
    goalMinutes: Int,
    usageAccessGranted: Boolean,
    onAllowUsageAccess: () -> Unit
) {
    if (!usageAccessGranted) {
        UsageAccessCard(onAllow = onAllowUsageAccess)
        return
    }
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusCard()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P ->
                Text(text = "The sleep log needs Android 9 or newer.", color = Slate, fontSize = 12.sp)
            sleepLog == null -> Text(text = "Loading…", color = Slate, fontSize = 12.sp)
            sleepLog.isEmpty() ->
                Text(text = "No nights recorded yet. Check back tomorrow morning.", color = Slate, fontSize = 12.sp)
            else -> {
                val average = sleepLog.sumOf { it.durationMillis } / sleepLog.size
                Text(
                    text = "Average ${formatDuration(average)} a night, based on when your phone was put down and picked up.",
                    color = Slate,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                sleepLog.forEach { entry ->
                    val metGoal = entry.durationMillis >= goalMinutes * 60_000L
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = relativeDayLabel(entry.wakeEpochDay), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${formatTime(context, entry.phoneDownAt)} → ${formatTime(context, entry.pickedUpAt)}",
                                color = Slate,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = formatDuration(entry.durationMillis),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (metGoal) Mint else AmberTint)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            color = if (metGoal) Forest else Amber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
