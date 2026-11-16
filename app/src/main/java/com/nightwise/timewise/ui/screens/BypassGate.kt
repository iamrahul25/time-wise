package com.nightwise.timewise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.friction.FrictionRepository
import com.nightwise.timewise.data.limits.LimitLogic
import com.nightwise.timewise.data.limits.LimitsState
import com.nightwise.timewise.ui.theme.Cream

/** A change that would loosen a limit, held until the user has been through the steps. */
class PendingBypass(
    val purpose: FlowPurpose,
    val packageName: String?,
    val limitMinutes: Int?,
    val cooldownMinutes: Int,
    val onApproved: () -> Unit
)

/**
 * Stops the quiet ways around a daily limit (turning limits off, removing one, raising one) and
 * puts the same steps and cooldown in front of them as extra time has.
 */
@Stable
class BypassGate {
    var pending by mutableStateOf<PendingBypass?>(null)
        private set

    /** Bumped for every request so a half-finished run never carries over. */
    var flowKey by mutableIntStateOf(0)
        private set

    fun ask(request: PendingBypass) {
        flowKey++
        pending = request
    }

    fun clear() {
        pending = null
    }

    /** Turning everything off waits as long as the longest cooldown of any limited app. */
    fun turnOff(state: LimitsState, apply: () -> Unit) {
        if (!LimitLogic.needsStepsToDisable(state)) return apply()
        val today = FocusRepository.currentEpochDay()
        ask(PendingBypass(FlowPurpose.TURN_OFF, null, null, state.maxCooldownMinutes(today), apply))
    }

    fun remove(state: LimitsState, packageName: String, apply: () -> Unit) {
        val today = FocusRepository.currentEpochDay()
        val current = state.limitFor(packageName)?.minutes
        ask(PendingBypass(FlowPurpose.REMOVE, packageName, current, state.cooldownMinutes(packageName, today), apply))
    }

    /** Loosening bedtime focus (turning it off, changing its times or removing an app) takes the steps. */
    fun bedtime(purpose: FlowPurpose, apply: () -> Unit) {
        ask(PendingBypass(purpose, null, null, LimitLogic.COOLDOWN_STEP_MINUTES, apply))
    }

    /** Only a higher limit needs the steps; the same or a lower one is saved straight away. */
    fun save(state: LimitsState, packageName: String, minutes: Int, apply: () -> Unit) {
        val current = state.limitFor(packageName)?.minutes
        if (!LimitLogic.isRaise(current, minutes)) return apply()
        val today = FocusRepository.currentEpochDay()
        ask(PendingBypass(FlowPurpose.RAISE, packageName, current, state.cooldownMinutes(packageName, today), apply))
    }
}

@Composable
fun rememberBypassGate(): BypassGate = remember { BypassGate() }

/** Shows the steps full screen while a change is waiting on them. Back or "keep" cancels the change. */
@Composable
fun BypassFlowHost(gate: BypassGate) {
    val request = gate.pending ?: return
    val context = LocalContext.current
    val friction = remember { FrictionRepository.get(context) }
    val content by friction.content.collectAsState()
    Dialog(
        onDismissRequest = gate::clear,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Cream)) {
            LimitReachedScreen(
                packageName = request.packageName,
                usedMillis = 0L,
                limitMinutes = request.limitMinutes,
                extraMinutes = 0,
                cooldownMinutes = request.cooldownMinutes,
                content = content,
                flowKey = gate.flowKey,
                photoFile = friction::photoFile,
                artFile = friction::artFile,
                onCheckPin = friction::checkPin,
                onCloseApp = gate::clear,
                onGranted = {
                    request.onApproved()
                    gate.clear()
                },
                purpose = request.purpose
            )
        }
    }
}
