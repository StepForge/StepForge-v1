package com.example.stepforge.debug

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

/**
 * Main thread freeze / ANR benzeri durumları tespit etmek için hafif watchdog.
 *
 * Mantık:
 * - Arka plandaki thread belli aralıklarla main thread'e ping atar
 * - Main thread cevap vermezse "freeze" kabul edilir
 * - Exception crash'i değil, cevap vermeme problemini loglar
 */
class AnrWatchdog(
    private val timeoutMs: Long = 6000L,
    private val checkIntervalMs: Long = 1500L,
    private val onAnrDetected: (blockedMs: Long) -> Unit
) {

    @Volatile
    private var running = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lastMainBeat = AtomicLong(SystemClock.uptimeMillis())

    private val beatRunnable = Runnable {
        lastMainBeat.set(SystemClock.uptimeMillis())
    }

    private var worker: Thread? = null

    fun start() {
        if (running) return
        running = true

        worker = Thread {
            while (running) {
                try {
                    mainHandler.post(beatRunnable)

                    Thread.sleep(checkIntervalMs)

                    val now = SystemClock.uptimeMillis()
                    val lastBeat = lastMainBeat.get()
                    val blockedFor = now - lastBeat

                    if (blockedFor >= timeoutMs) {
                        onAnrDetected(blockedFor)

                        // aynı freeze için spam log atmasın diye beat resetliyoruz
                        lastMainBeat.set(SystemClock.uptimeMillis())
                    }
                } catch (_: InterruptedException) {
                    break
                } catch (_: Exception) {
                    // watchdog asla uygulamayı bozmasın
                }
            }
        }.apply {
            name = "StepForgeAnrWatchdog"
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        worker?.interrupt()
        worker = null
    }
}