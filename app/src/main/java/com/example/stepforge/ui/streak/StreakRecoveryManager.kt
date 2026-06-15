package com.example.stepforge.ui.streak

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private val Context.streakRecoveryStore by preferencesDataStore(name = "streak_recovery")

@Immutable
data class StreakRecoveryState(
    val visible: Boolean = false,
    val expiryMillis: Long = 0L,
    val lostStreakDays: Int = 0,
    val restored: Boolean = false,
    val price: String = StreakRecoveryManager.DEFAULT_PRICE,
    val discountedPrice: String = StreakRecoveryManager.DEFAULT_DISCOUNTED_PRICE,
    val offerActive: Boolean = false,
    val lastTriggerTimeMillis: Long = 0L,
    val remainingText: String = "",
    val expiredVisibleState: Boolean = false
) {
    val displayPrice: String
        get() = if (offerActive && discountedPrice.isNotBlank()) discountedPrice else price
}

object StreakRecoveryManager {

    const val DEFAULT_PRICE = "€2.99"
    const val DEFAULT_DISCOUNTED_PRICE = "€1.99"
    private val recoveryVisibleKey = booleanPreferencesKey("recovery_visible")
    private val recoveryExpiryKey = longPreferencesKey("recovery_expiry")
    private val lostStreakDaysKey = intPreferencesKey("lost_streak_days")
    private val restoredKey = booleanPreferencesKey("recovery_restored")
    private val recoveryPriceKey = stringPreferencesKey("recovery_price")
    private val recoveryDiscountedPriceKey = stringPreferencesKey("recovery_discounted_price")
    private val recoveryOfferActiveKey = booleanPreferencesKey("recovery_offer_active")
    private val recoveryLastTriggerTimeKey = longPreferencesKey("recovery_last_trigger_time")

    fun recoveryState(context: Context): Flow<StreakRecoveryState> {
        val appContext = context.applicationContext
        return combine(appContext.streakRecoveryStore.data, tickerFlow()) { prefs, now ->
            prefs.toRecoveryState(now)
        }.distinctUntilChanged()
    }

    suspend fun triggerRecovery(
        context: Context,
        streakDays: Int,
        recoveryHours: Int = StreakBehaviorEngine.LOST_RECOVERY_WINDOW_HOURS
    ) = withContext(Dispatchers.IO) {
        val store = context.applicationContext.streakRecoveryStore
        val now = System.currentTimeMillis()
        val prefs = store.data.first()
        val currentExpiry = prefs[recoveryExpiryKey] ?: 0L
        val currentlyVisible = prefs[recoveryVisibleKey] == true
        val alreadyRestored = prefs[restoredKey] == true
        val dismissedDuringActiveWindow = !currentlyVisible &&
                (prefs[recoveryLastTriggerTimeKey] ?: 0L) > 0L &&
                now <= currentExpiry

        if (now <= currentExpiry && (currentlyVisible || alreadyRestored || dismissedDuringActiveWindow)) {
            return@withContext
        }

        val windowMs = recoveryHours.coerceIn(1, 72) * 60L * 60L * 1000L
        store.edit {
            it[recoveryVisibleKey] = true
            it[recoveryExpiryKey] = now + windowMs
            it[lostStreakDaysKey] = streakDays.coerceAtLeast(0)
            it[restoredKey] = false
            it[recoveryPriceKey] = it[recoveryPriceKey] ?: DEFAULT_PRICE
            it[recoveryDiscountedPriceKey] =
                it[recoveryDiscountedPriceKey] ?: DEFAULT_DISCOUNTED_PRICE
            it[recoveryOfferActiveKey] = it[recoveryOfferActiveKey] ?: false
            it[recoveryLastTriggerTimeKey] = now
        }
    }

    suspend fun dismissRecovery(context: Context) = withContext(Dispatchers.IO) {
        context.applicationContext.streakRecoveryStore.edit {
            it[recoveryVisibleKey] = false
        }
        StreakBehaviorEngine.dismissLostDialog(context.applicationContext)
    }

    suspend fun restoreStreak(context: Context): Boolean = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val restored = StreakBehaviorEngine.restoreLostStreak(appContext)
        appContext.streakRecoveryStore.edit {
            it[recoveryVisibleKey] = false
            it[restoredKey] = true
            it[recoveryExpiryKey] = 0L
        }
        restored
    }

    suspend fun expireIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val store = context.applicationContext.streakRecoveryStore
        val prefs = store.data.first()
        val now = System.currentTimeMillis()
        val expiry = prefs[recoveryExpiryKey] ?: 0L
        val shouldExpire = prefs[recoveryVisibleKey] == true && expiry > 0L && expiry < now

        if (shouldExpire) {
            store.edit {
                it[recoveryVisibleKey] = false
            }
            StreakBehaviorEngine.dismissLostDialog(context.applicationContext)
        }
    }

    suspend fun isRecoveryVisible(context: Context): Boolean = withContext(Dispatchers.IO) {
        context.applicationContext.streakRecoveryStore.data.first()
            .toRecoveryState(System.currentTimeMillis())
            .visible
    }

    suspend fun getLostStreakDays(context: Context): Int = withContext(Dispatchers.IO) {
        context.applicationContext.streakRecoveryStore.data.first()[lostStreakDaysKey] ?: 0
    }

    suspend fun getRemainingText(context: Context): String = withContext(Dispatchers.IO) {
        context.applicationContext.streakRecoveryStore.data.first()
            .toRecoveryState(System.currentTimeMillis())
            .remainingText
    }

    suspend fun getRecoveryPrice(context: Context): String = withContext(Dispatchers.IO) {
        context.applicationContext.streakRecoveryStore.data.first()[recoveryPriceKey] ?: DEFAULT_PRICE
    }

    suspend fun getDiscountedPrice(context: Context): String = withContext(Dispatchers.IO) {
        context.applicationContext.streakRecoveryStore.data.first()[recoveryDiscountedPriceKey]
            ?: DEFAULT_DISCOUNTED_PRICE
    }

    suspend fun isDiscountActive(context: Context): Boolean = withContext(Dispatchers.IO) {
        context.applicationContext.streakRecoveryStore.data.first()[recoveryOfferActiveKey] ?: false
    }

    private fun tickerFlow(): Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            emit(System.currentTimeMillis())
            delay(60_000L)
        }
    }

    private fun Preferences.toRecoveryState(now: Long): StreakRecoveryState {
        val visible = this[recoveryVisibleKey] ?: false
        val expiry = this[recoveryExpiryKey] ?: 0L
        val restored = this[restoredKey] ?: false
        val isExpired = visible && expiry > 0L && now > expiry
        val activeVisible = visible && !restored && expiry > now

        return StreakRecoveryState(
            visible = activeVisible,
            expiryMillis = expiry,
            lostStreakDays = this[lostStreakDaysKey] ?: 0,
            restored = restored,
            price = this[recoveryPriceKey] ?: DEFAULT_PRICE,
            discountedPrice = this[recoveryDiscountedPriceKey] ?: DEFAULT_DISCOUNTED_PRICE,
            offerActive = this[recoveryOfferActiveKey] ?: false,
            lastTriggerTimeMillis = this[recoveryLastTriggerTimeKey] ?: 0L,
            remainingText = remainingText(expiry, now),
            expiredVisibleState = isExpired
        )
    }

    private fun remainingText(expiry: Long, now: Long): String {
        val remaining = expiry - now
        if (remaining <= 0L || expiry <= 0L) return "Recovery expired"

        val totalMinutes = remaining / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return "Recovery expires in ${hours}h ${minutes}m"
    }
}
