package com.example.stepforge.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import com.example.stepforge.R // Eğer bu satır kırmızı yanarsa kendi paket isminle düzelt
import kotlinx.coroutines.delay

/**
 * Hedef tamamlandığında konfetti patlatmak ve SES çalmak için.
 */
@Composable
fun GoalKonfetti(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onFinished: (() -> Unit)? = null
) {
    if (!visible) return

    val context = LocalContext.current

    // --- SES EFEKTİ ---
    val mediaPlayer = remember {
        try {
            // Dosya adın 'confetti_pop' olmalı
            MediaPlayer.create(context, R.raw.confetti_pop)
        } catch (e: Exception) {
            null
        }
    }

    // Ekrandan çıkınca sesi temizle
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    // Efekt başladığında sesi çal
    LaunchedEffect(Unit) {
        mediaPlayer?.start() // Sesi oynat
        delay(7000) // Animasyon süresi kadar bekle
        onFinished?.invoke()
    }

    // --- GÖRSEL AYARLAR (Senin Yavaş Süzülen Ayarların) ---
    val colors = listOf(
        0xFF00F5FF.toInt(), // Neon Cyan
        0xFF00FFA3.toInt(), // Neon Green
        0xFFFFD54F.toInt(), // Yellow
        0xFFEF5350.toInt(), // Red
        0xFFB388FF.toInt()  // Purple
    )

    // 2 saniye boyunca konfetti üretir
    val emitter = Emitter(duration = 2000, TimeUnit.MILLISECONDS).perSecond(50)

    val leftParty = Party(
        angle = Angle.RIGHT - 60,     // Yukarıya doğru
        spread = 60,
        speed = 25f,                  // HIZI DÜŞÜK (Yavaş çıkış)
        maxSpeed = 40f,
        damping = 0.92f,              // Hava direnci (Yavaşça süzülür)
        timeToLive = 6000L,           // 6 Saniye yaşar
        colors = colors,
        emitter = emitter,
        position = Position.Relative(0.0, 0.4) // Ekranın ortasının solundan
    )

    val rightParty = Party(
        angle = Angle.LEFT + 60,      // Yukarıya doğru
        spread = 60,
        speed = 25f,                  // HIZI DÜŞÜK
        maxSpeed = 40f,
        damping = 0.92f,
        timeToLive = 6000L,
        colors = colors,
        emitter = emitter,
        position = Position.Relative(1.0, 0.4) // Ekranın ortasının sağından
    )

    // İstersen senin verdiğin ekstra kodu (Rain efekti gibi) buraya 3. bir party olarak ekleyebilirsin
    // Ama şu anki haliyle sağ ve sol çıkış yeterli olacaktır.

    KonfettiView(
        modifier = modifier.fillMaxSize(),
        parties = listOf(leftParty, rightParty)
    )
}
