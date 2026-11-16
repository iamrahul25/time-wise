package com.nightwise.timewise.limits

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nightwise.timewise.data.FocusRepository
import com.nightwise.timewise.data.friction.FrictionRepository
import com.nightwise.timewise.data.limits.AppLimitRepository
import com.nightwise.timewise.data.limits.LimitLogic
import com.nightwise.timewise.ui.screens.FlowPurpose
import com.nightwise.timewise.ui.screens.LimitReachedScreen
import com.nightwise.timewise.ui.theme.TimeWiseTheme

/** Covers an app whose limit ran out. Extra time is earned by going through every friction step. */
class LimitReachedActivity : ComponentActivity() {

    private var blockedPackage by mutableStateOf("")
    private var usedMillis by mutableLongStateOf(0L)
    /** True when bedtime focus blocks the app rather than its daily limit running out. */
    private var bedtime by mutableStateOf(false)
    /** Bumped for every new block so a half-finished run starts over. */
    private var flowKey by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        readIntent(intent)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        val limits = AppLimitRepository.get(this)
        val friction = FrictionRepository.get(this)
        setContent {
            TimeWiseTheme {
                val limitsState by limits.state.collectAsState()
                val content by friction.content.collectAsState()
                val today = FocusRepository.currentEpochDay()
                LimitReachedScreen(
                    packageName = blockedPackage,
                    usedMillis = usedMillis,
                    limitMinutes = limitsState.limitFor(blockedPackage)?.minutes,
                    extraMinutes = limitsState.extraMinutes(blockedPackage, today),
                    cooldownMinutes = if (bedtime) LimitLogic.COOLDOWN_STEP_MINUTES else limitsState.cooldownMinutes(blockedPackage, today),
                    content = content,
                    flowKey = flowKey,
                    photoFile = friction::photoFile,
                    artFile = friction::artFile,
                    onCheckPin = friction::checkPin,
                    onCloseApp = ::goHome,
                    onGranted = ::grantExtra,
                    purpose = if (bedtime) FlowPurpose.BEDTIME_EXTRA else FlowPurpose.EXTRA_TIME
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
        flowKey++
    }

    private fun readIntent(intent: Intent) {
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        usedMillis = intent.getLongExtra(EXTRA_USED_MILLIS, 0L)
        bedtime = intent.getBooleanExtra(EXTRA_BEDTIME, false)
    }

    private fun grantExtra() {
        val pkg = blockedPackage
        if (bedtime) {
            val until = System.currentTimeMillis() + LimitLogic.EXTRA_MINUTES * 60_000L
            FocusRepository.get(this).grantBedtimeTime(pkg, until)
        } else {
            AppLimitRepository.get(this).grantExtra(pkg, FocusRepository.currentEpochDay())
        }
        AppLimitService.onExtraGranted()
        packageManager.getLaunchIntentForPackage(pkg)?.let { launch ->
            runCatching { startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        finish()
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(home) }
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_USED_MILLIS = "used_millis"
        private const val EXTRA_BEDTIME = "bedtime"

        fun intent(context: Context, packageName: String, usedMillis: Long): Intent =
            Intent(context, LimitReachedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_USED_MILLIS, usedMillis)

        /** Covers an app that bedtime focus blocks. */
        fun bedtimeIntent(context: Context, packageName: String): Intent =
            intent(context, packageName, 0L).putExtra(EXTRA_BEDTIME, true)
    }
}
