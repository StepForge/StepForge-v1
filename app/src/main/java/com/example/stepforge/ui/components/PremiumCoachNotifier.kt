package com.example.stepforge.ui.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import com.example.stepforge.R
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.ui.streak.PremiumCoachDecision
import com.example.stepforge.ui.streak.PremiumCoachMessageType
import com.example.stepforge.ui.streak.StreakShieldPrefs
import kotlinx.coroutines.flow.first

class PremiumCoachNotifier(
    private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "premium_coach_alerts"
        private const val NOTIFICATION_ID = 4401
        private const val MIN_NOTIFY_INTERVAL_MS = 2 * 60 * 60 * 1000L
    }

    suspend fun maybeNotify(decision: PremiumCoachDecision) {
        if (!decision.shouldNotify || decision.type == null) return

        val prefs = context.stepforgeStore.data.first()
        val now = System.currentTimeMillis()

        val lastNotifyAt = prefs[StreakShieldPrefs.PREMIUM_AI_LAST_NOTIFY_AT_MS] ?: 0L
        val lastNotifyType = prefs[StreakShieldPrefs.PREMIUM_AI_LAST_NOTIFY_TYPE] ?: ""

        if (now - lastNotifyAt < MIN_NOTIFY_INTERVAL_MS) return

        // ✅ Aynı tip bildirimi 6 saatte bir tekrar göster (sonsuza dek engelleme)
        val sameTypeBlockMs = 6 * 60 * 60 * 1000L
        if (lastNotifyType == decision.type.name && now - lastNotifyAt < sameTypeBlockMs) return

        createChannelIfNeeded()

        // ✅ Android 13+ bildirim izni kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val title = buildTitle(decision)
        val body = buildBody(decision)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)

        context.stepforgeStore.edit {
            it[StreakShieldPrefs.PREMIUM_AI_LAST_NOTIFY_AT_MS] = now
            it[StreakShieldPrefs.PREMIUM_AI_LAST_NOTIFY_TYPE] = decision.type.name
        }
    }

    private fun buildTitle(decision: PremiumCoachDecision): String {
        return when (decision.type) {
            PremiumCoachMessageType.STREAK_RISK ->
                context.getString(R.string.premium_coach_title_streak_risk)

            PremiumCoachMessageType.NEXT_SHIELD_MILESTONE ->
                context.getString(R.string.premium_coach_title_next_shield)

            PremiumCoachMessageType.GOAL_ALMOST_COMPLETE ->
                context.getString(R.string.premium_coach_title_goal_close)

            PremiumCoachMessageType.RESCUE_AVAILABLE ->
                context.getString(R.string.premium_coach_title_rescue_ready)

            PremiumCoachMessageType.LOW_ACTIVITY_PATTERN ->
                context.getString(R.string.premium_coach_title_low_activity)

            null ->
                context.getString(R.string.app_name)
        }
    }

    private fun buildBody(decision: PremiumCoachDecision): String {
        return when (decision.type) {
            PremiumCoachMessageType.STREAK_RISK ->
                context.getString(R.string.premium_coach_body_streak_risk)

            PremiumCoachMessageType.NEXT_SHIELD_MILESTONE ->
                context.getString(
                    R.string.premium_coach_body_next_shield,
                    decision.stepsRemainingToNextShieldHour
                )

            PremiumCoachMessageType.GOAL_ALMOST_COMPLETE ->
                context.getString(R.string.premium_coach_body_goal_close)

            PremiumCoachMessageType.RESCUE_AVAILABLE ->
                context.getString(
                    R.string.premium_coach_body_rescue_ready,
                    decision.currentShieldMinutesLeft / 60,
                    decision.currentShieldMinutesLeft % 60
                )

            PremiumCoachMessageType.LOW_ACTIVITY_PATTERN ->
                context.getString(R.string.premium_coach_body_low_activity)

            null ->
                ""
        }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.premium_coach_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.premium_coach_channel_desc)
        }

        manager.createNotificationChannel(channel)
    }
}