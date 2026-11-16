package com.nightwise.timewise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.limits.LimitLogic
import com.nightwise.timewise.ui.formatDuration
import com.nightwise.timewise.ui.theme.Amber
import com.nightwise.timewise.ui.theme.AmberTint
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.NeutralChip
import com.nightwise.timewise.ui.theme.Red
import com.nightwise.timewise.ui.theme.Slate

/**
 * Blocks one app during bedtime focus, or takes it off. Works like [LimitDialog]: each app is saved on its
 * own, and taking one off asks for the steps. [onRemove] is null for an app that isn't blocked yet.
 */
@Composable
fun BedtimeAppDialog(
    packageName: String,
    bedtimeLabel: String,
    wakeLabel: String,
    /** Average use a night in the 2 hours before and after bedtime, if known. */
    averageMillis: Long?,
    /** True when taking it off has to go through the steps, because bedtime focus is on. */
    removalNeedsSteps: Boolean,
    onSave: () -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val info = rememberAppInfo(packageName)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIcon(info, size = 40.dp) },
        title = { Text(info?.label ?: "Bedtime focus", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Closed from $bedtimeLabel until $wakeLabel. To open it at night you go through your steps " +
                        "and get ${LimitLogic.EXTRA_MINUTES} minutes, then it closes again.",
                    color = Slate,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                if (onRemove != null && removalNeedsSteps) {
                    Text(
                        text = "This is your promise to yourself. Taking it off bedtime focus means going through your steps.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberTint)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Amber,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
                if (averageMillis != null && averageMillis > 0) {
                    Text(
                        text = "You average ${formatDuration(averageMillis)} a night on it in the 2 hours before and after bedtime.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeutralChip)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(); onDismiss() }) {
                Text(if (onRemove == null) "Save" else "Done", color = Forest)
            }
        },
        dismissButton = {
            Row {
                if (onRemove != null) {
                    TextButton(onClick = { onRemove(); onDismiss() }) { Text("Remove", color = Red) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", color = Slate) }
            }
        }
    )
}
