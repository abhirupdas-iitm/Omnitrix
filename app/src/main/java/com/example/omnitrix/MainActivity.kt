package com.example.omnitrix

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import android.media.AudioAttributes
import android.media.SoundPool

import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {

    private lateinit var soundPool: SoundPool
    private var dialSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup low-latency sound engine
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        dialSoundId = soundPool.load(this, R.raw.omnitrix_activate, 1)

        setContent {
            OmnitrixScreenWithSound(
                onActivate = {
                    soundPool.play(
                        dialSoundId,
                        1f,
                        1f,
                        0,
                        0,
                        1f
                    )
                }
            )
        }
    }

    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
    }
}

@Composable
fun OmnitrixScreen() {

    // Controls text visibility
    var showTransformedText by remember { mutableStateOf(false) }

    // Controls temporary flash boost
    var activationBoost by remember { mutableFloatStateOf(0f) }

    // Idle breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    // Activation sequence controller
    LaunchedEffect(showTransformedText) {

        if (showTransformedText) {

            // Instant flash up
            activationBoost = 0.6f

            // Hold peak flash briefly
            delay(300)

            // Keep transformed state visible
            delay(1200)

            // Smooth return
            activationBoost = 0f

            // Hide text
            showTransformedText = false
        }
    }

    // Final brightness combines idle breathing + activation flash
    val brightness = idlePulse + activationBoost

    val coreColor = Color(
        red = 0f,
        green = brightness.coerceAtMost(1f),
        blue = 0f,
        alpha = 1f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { showTransformedText = true },
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.omnitrix_symbol),
                contentDescription = "Omnitrix Symbol",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = brightness.coerceIn(0.1f, 5f)
                    }
            )

        }
    }
}
@Composable
fun OmnitrixScreenWithSound(onActivate: () -> Unit) {

    var trigger by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger) {
            onActivate()
            trigger = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                trigger = true
            }
    ) {
        OmnitrixScreen()
    }
}