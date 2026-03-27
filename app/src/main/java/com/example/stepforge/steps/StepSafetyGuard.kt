package com.example.stepforge.debug

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * StepSafetyGuard
 *
 * Amaç:
 * - Uygulama içinde belirli hata / reject / anormal durumlar art arda yaşanırsa
 *   sistemi tamamen kilitlemek yerine geçici bir "guard / bypass / soft-protect" kararı üretmek.
 *
 * Kullanım alanı:
 * - StepCounterService reject kararları
 * - Sensor watchdog
 * - Sync blokları
 * - Notification / widget update flood koruması
 * - Tekrarlayan hata patlamalarını yumuşatma
 *
 * Not:
 * - Bu sınıf doğrudan crash yakalayıp uygulamayı kurtarmaz.
 * - Ancak çağrıldığı noktalarda "şu an sıkıntılı mod var mı?" kararını verir
 *   ve kodun daha güvenli fallback davranış seçmesini sağlar.
 */
class StepSafetyGuard(
    private val config: Config = Config()
) {

    data class Config(
        val rejectWindowMs: Long = 15_000L,
        val rejectThreshold: Int = 4,
        val bypassDurationMs: Long = 20_000L,
        val eventCooldownMs: Long = 1_500L,
        val errorWindowMs: Long = 20_000L,
        val errorThreshold: Int = 5,
        val softBlockDurationMs: Long = 25_000L
    )

    data class StateSnapshot(
        val bypassActive: Boolean,
        val softBlockActive: Boolean,
        val bypassUntilMs: Long,
        val softBlockUntilMs: Long,
        val rejectCount: Int,
        val errorCount: Int,
        val lastReason: String?
    )

    private val rejectWindowStartMs = AtomicLong(0L)
    private val rejectCount = AtomicInteger(0)

    private val errorWindowStartMs = AtomicLong(0L)
    private val errorCount = AtomicInteger(0)

    private val bypassUntilMs = AtomicLong(0L)
    private val softBlockUntilMs = AtomicLong(0L)

    private val lastEventMsByKey = ConcurrentHashMap<String, AtomicLong>()
    private val lastReasonRef = AtomicLongReason()

    /**
     * Aynı key kısa sürede tekrar tekrar geliyorsa flood say.
     */
    fun shouldThrottle(key: String, now: Long = now()): Boolean {
        val holder = lastEventMsByKey.getOrPut(key) { AtomicLong(0L) }
        val previous = holder.get()

        if (previous > 0L && now - previous < config.eventCooldownMs) {
            return true
        }

        holder.set(now)
        return false
    }

    /**
     * Reject / block / validation fail gibi olayları kaydet.
     *
     * return:
     * - true  -> guard bypass aktif oldu / aktif, yani üst kod daha yumuşak davranabilir
     * - false -> normal reject davranışı sürsün
     */
    fun registerReject(reason: String, now: Long = now()): Boolean {
        lastReasonRef.set(reason)
        rotateRejectWindowIfNeeded(now)

        val newCount = rejectCount.incrementAndGet()
        if (newCount >= config.rejectThreshold) {
            bypassUntilMs.set(now + config.bypassDurationMs)
            return true
        }

        return isBypassActive(now)
    }

    /**
     * Exception / ciddi sistem düzensizliği gibi olayları kaydet.
     *
     * Çok sık hata olursa soft block açılır.
     * Bu "servisi öldür" demek değildir;
     * üst katmanda "agresif davranma / tekrar tekrar riskli işlem yapma" kararı için kullanılır.
     */
    fun registerError(reason: String, now: Long = now()): Boolean {
        lastReasonRef.set(reason)
        rotateErrorWindowIfNeeded(now)

        val newCount = errorCount.incrementAndGet()
        if (newCount >= config.errorThreshold) {
            softBlockUntilMs.set(now + config.softBlockDurationMs)
            return true
        }

        return isSoftBlockActive(now)
    }

    fun isBypassActive(now: Long = now()): Boolean {
        return now < bypassUntilMs.get()
    }

    fun isSoftBlockActive(now: Long = now()): Boolean {
        return now < softBlockUntilMs.get()
    }

    /**
     * Dış kod fallback moduna geçtiyse manuel temizleyebilir.
     */
    fun clear() {
        rejectWindowStartMs.set(0L)
        rejectCount.set(0)
        errorWindowStartMs.set(0L)
        errorCount.set(0)
        bypassUntilMs.set(0L)
        softBlockUntilMs.set(0L)
        lastReasonRef.set(null)
        lastEventMsByKey.clear()
    }

    fun snapshot(now: Long = now()): StateSnapshot {
        return StateSnapshot(
            bypassActive = isBypassActive(now),
            softBlockActive = isSoftBlockActive(now),
            bypassUntilMs = bypassUntilMs.get(),
            softBlockUntilMs = softBlockUntilMs.get(),
            rejectCount = rejectCount.get(),
            errorCount = errorCount.get(),
            lastReason = lastReasonRef.get()
        )
    }

    private fun rotateRejectWindowIfNeeded(now: Long) {
        val start = rejectWindowStartMs.get()
        if (start == 0L || now - start > config.rejectWindowMs) {
            rejectWindowStartMs.set(now)
            rejectCount.set(0)
        }
    }

    private fun rotateErrorWindowIfNeeded(now: Long) {
        val start = errorWindowStartMs.get()
        if (start == 0L || now - start > config.errorWindowMs) {
            errorWindowStartMs.set(now)
            errorCount.set(0)
        }
    }

    private fun now(): Long = SystemClock.elapsedRealtime()

    /**
     * AtomicReference yerine çok hafif bir wrapper tutuyoruz.
     */
    private class AtomicLongReason {
        @Volatile
        private var value: String? = null

        fun set(newValue: String?) {
            value = newValue
        }

        fun get(): String? = value
    }
}