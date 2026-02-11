package com.example.stepforge

import android.app.Activity
import android.content.Context
import android.content.Intent

object AppLock {

    private const val PREF = "stepforge_applock"
    private const val KEY_LAST_BG = "last_bg"
    private const val KEY_TIMEOUT = "timeout_sec"

    fun shouldLock(context: Context): Boolean {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

        val timeout = p.getInt(KEY_TIMEOUT, -1)
        if (timeout == -1) return false   // kilit kapalı
        if (timeout == 0) return true    // immediate

        val lastBg = p.getLong(KEY_LAST_BG, 0L)
        if (lastBg == 0L) return false

        val diff = (System.currentTimeMillis() - lastBg) / 1000
        return diff >= timeout
    }

    fun onForeground(activity: Activity) {
        if (!shouldLock(activity)) return

        activity.startActivity(
            Intent(activity, LockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
        )
    }

    fun onBackground(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BG, System.currentTimeMillis())
            .apply()
    }

    fun setTimeout(context: Context, seconds: Int) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_TIMEOUT, seconds)
            .apply()
    }
}
