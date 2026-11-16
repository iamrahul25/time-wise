package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightwise.timewise.data.friction.FrictionContent
import com.nightwise.timewise.data.friction.FrictionLogic
import com.nightwise.timewise.data.friction.FrictionStep
import com.nightwise.timewise.ui.components.SmallActionButton
import com.nightwise.timewise.ui.theme.Forest
import com.nightwise.timewise.ui.theme.Ink
import com.nightwise.timewise.ui.theme.Mint
import com.nightwise.timewise.ui.theme.Mist
import com.nightwise.timewise.ui.theme.Slate
import com.nightwise.timewise.ui.theme.StepperBg

/**
 * Bedtime focus and daily app limits both put every step in front of [unlock], so neither can be turned
 * on until all of them are set up. This walks the user through what is left: progress, a checklist, and
 * a way straight into the setup. [unlock] reads as the start of a sentence, e.g. "Opening a blocked app at bedtime".
 */
@Composable
fun StepsGuideCard(content: FrictionContent, unlock: String, onOpenSetup: () -> Unit) {
    val steps = FrictionStep.entries
    val ready = steps.count { FrictionLogic.isReady(it, content) }
    val next = steps.firstOrNull { !FrictionLogic.isReady(it, content) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Mint)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Set up your ${steps.size} steps to turn this on · $ready of ${steps.size}",
            color = Forest,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "$unlock goes through every step, so each one needs to be set up first.",
            color = Slate,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            steps.forEach { step ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (FrictionLogic.isReady(step, content)) Forest else StepperBg)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            steps.forEach { step ->
                val done = FrictionLogic.isReady(step, content)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .then(if (done) Modifier else Modifier.clickable(onClick = onOpenSetup))
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (done) Forest else Mist,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = step.title,
                        modifier = Modifier.weight(1f),
                        color = if (done) Slate else Ink,
                        fontSize = 13.sp,
                        fontWeight = if (step == next) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (!done) {
                        Text(text = "Set up", color = Forest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        SmallActionButton(text = if (ready == 0) "Start setup" else "Continue setup", onClick = onOpenSetup)
    }
}

/** Shown when a toggle is tapped before every step is set up: says what is missing and offers to set it up now. */
@Composable
fun StepsGuideDialog(
    content: FrictionContent,
    /** What can't be turned on yet, e.g. "Bedtime focus". */
    feature: String,
    unlock: String,
    onSetUp: () -> Unit,
    onDismiss: () -> Unit
) {
    val steps = FrictionStep.entries
    val missing = steps.filter { !FrictionLogic.isReady(it, content) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set up your steps first") },
        text = {
            Text(
                text = "Bedtime focus can't be turned on until all ${steps.size} steps are set up, because opening a " +
                    "blocked app at night goes through every one of them.\n\n" +
                    "Still to do (${missing.size} of ${steps.size}): ${missing.joinToString { it.title }}.",
                color = Slate,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            TextButton(onClick = { onSetUp(); onDismiss() }) { Text("Set up now", color = Forest) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later", color = Slate) }
        }
    )
}
