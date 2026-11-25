package com.group8.phonecontext.data.datasource

import android.Manifest
import android.annotation.SuppressLint
import android.app.AutomaticZenRule
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.group8.phonecontext.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages Automatic Focus Mode (Do Not Disturb) via Android [NotificationManager].
 *
 * - On Android 15+ (API 35+, including Android 16 / API 36 on Infinix Note 40), creates and
 *   toggles an app-owned [AutomaticZenRule] so the system combines it with any pre-existing
 *   user DND settings without overriding manual user rules.
 * - On API 24-34, falls back cleanly to [NotificationManager.setInterruptionFilter] while
 *   tracking app ownership.
 */
class SystemFocusDataSource(
    private val context: Context
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var appOwnedFocusActive: Boolean = false

    fun hasNotificationPolicyAccess(): Boolean =
        notificationManager.isNotificationPolicyAccessGranted

    fun isAppFocusActive(): Boolean = appOwnedFocusActive

    fun isExternalDndActive(): Boolean {
        if (appOwnedFocusActive) return false
        return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    fun activateFocusMode(): Boolean {
        if (!hasNotificationPolicyAccess()) return false

        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            toggleAppOwnedZenRuleApi35(active = true) || fallbackSetFilter(active = true)
        } else {
            fallbackSetFilter(active = true)
        }

        if (success) {
            appOwnedFocusActive = true
        }
        return success
    }

    fun releaseFocusMode(): Boolean {
        if (!hasNotificationPolicyAccess()) {
            appOwnedFocusActive = false
            return false
        }

        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            toggleAppOwnedZenRuleApi35(active = false) || fallbackSetFilter(active = false)
        } else {
            fallbackSetFilter(active = false)
        }

        appOwnedFocusActive = false
        return success
    }

    @SuppressLint("NewApi")
    private fun toggleAppOwnedZenRuleApi35(active: Boolean): Boolean {
        return try {
            val conditionId = Uri.parse(RULE_CONDITION_URI)
            val ruleId = ensureZenRuleIdApi35(conditionId) ?: return false
            val state = if (active) Condition.STATE_TRUE else Condition.STATE_FALSE
            val summary = if (active) {
                "Face-down on desk detected"
            } else {
                "Desk context ended"
            }
            val condition = Condition(conditionId, summary, state)
            notificationManager.setAutomaticZenRuleState(ruleId, condition)
            true
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("NewApi")
    private fun ensureZenRuleIdApi35(conditionId: Uri): String? {
        val savedId = prefs.getString(KEY_ZEN_RULE_ID, null)
        if (!savedId.isNullOrBlank()) {
            val existing = notificationManager.getAutomaticZenRule(savedId)
            if (existing != null) {
                return savedId
            }
        }

        val existingRule = notificationManager.automaticZenRules.entries.firstOrNull { (_, rule) ->
            rule.conditionId == conditionId || rule.name == RULE_NAME
        }
        if (existingRule != null) {
            prefs.edit().putString(KEY_ZEN_RULE_ID, existingRule.key).apply()
            return existingRule.key
        }

        val configActivity = ComponentName(
            context.packageName,
            "com.group8.phonecontext.presentation.main.MainActivity"
        )
        val rule = AutomaticZenRule.Builder(RULE_NAME, conditionId)
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            .setType(AutomaticZenRule.TYPE_OTHER)
            .setTriggerDescription("Activates when phone is stationary and face-down on a desk")
            .setConfigurationActivity(configActivity)
            .setManualInvocationAllowed(true)
            .setEnabled(true)
            .build()

        val createdId = notificationManager.addAutomaticZenRule(rule)
        if (!createdId.isNullOrBlank()) {
            prefs.edit().putString(KEY_ZEN_RULE_ID, createdId).apply()
        }
        return createdId
    }

    private fun fallbackSetFilter(active: Boolean): Boolean {
        return try {
            val filter = if (active) {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            notificationManager.setInterruptionFilter(filter)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun sendTestNotification(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        ensureTestNotificationChannel()

        val timeText = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val notification = NotificationCompat.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ContextSense DND Test Notification")
            .setContentText("Triggered at $timeText — verify DND suppression behavior.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun ensureTestNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(TEST_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    TEST_CHANNEL_ID,
                    "Focus Mode Verification",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Test notifications for validating Do Not Disturb suppression"
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "contextsense_focus_prefs"
        private const val KEY_ZEN_RULE_ID = "desk_focus_zen_rule_id"
        private const val RULE_NAME = "ContextSense Desk Focus"
        private const val RULE_CONDITION_URI = "condition://com.group8.phonecontext/desk_focus"
        private const val TEST_CHANNEL_ID = "contextsense_focus_test"
    }
}
