package com.example.omnitrix

import androidx.compose.animation.core.animateFloatAsState
import kotlin.math.roundToInt
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var soundPool: SoundPool
    private var activateSoundId: Int = 0
    private var tickSoundId: Int = 0
    private var soundLoaded = false

    // Rotary state shared with Compose
    private val dialAngleState = mutableFloatStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundLoaded = true
        }

        activateSoundId = soundPool.load(this, R.raw.omnitrix_activate, 1)
        tickSoundId = soundPool.load(this, R.raw.omnitrix_tick, 1)

        setContent {
            OmnitrixScreen(
                dialAngle = dialAngleState.floatValue,
                onActivate = {
                    if (soundLoaded) {
                        soundPool.play(
                            activateSoundId,
                            1f,
                            1f,
                            1,
                            0,
                            1f
                        )
                    }
                },
                onTick = {
                    if (soundLoaded) {
                        soundPool.play(
                            tickSoundId,
                            0.7f,
                            0.7f,
                            1,
                            0,
                            1f
                        )
                    }
                }
            )
        }
    }

    // Capture rotary directly from Android
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL &&
            event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)
        ) {
            val delta = event.getAxisValue(MotionEvent.AXIS_SCROLL)
            dialAngleState.floatValue += delta * 10f
            println("ROTARY CAPTURED: $delta")
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
    }
}

@Composable
fun OmnitrixScreen(
    dialAngle: Float,
    onActivate: () -> Unit,
    onTick: () -> Unit
) {
    val slotCount = 15
    val slotAngle = 360f / slotCount

    // Normalize angle to 0..360 range
    val normalizedAngle = ((dialAngle % 360f) + 360f) % 360f

    // Snap to nearest slot
    val snappedIndex = (normalizedAngle / slotAngle).roundToInt() % slotCount
    val snappedAngle = snappedIndex * slotAngle

    val animatedAngle by animateFloatAsState(
        targetValue = snappedAngle,
        animationSpec = tween(
            durationMillis = 120,
            easing = FastOutSlowInEasing
        ),
        label = "dialAnimation"
    )

    // Track previous slot to trigger tick sound
    var previousSlot by remember { mutableIntStateOf(snappedIndex) }

    LaunchedEffect(snappedIndex) {
        if (snappedIndex != previousSlot) {
            onTick()
            previousSlot = snappedIndex
        }
    }

    var showTransformedText by remember { mutableStateOf(false) }
    var activationBoost by remember { mutableFloatStateOf(0f) }

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

    LaunchedEffect(showTransformedText) {
        if (showTransformedText) {
            onActivate()
            activationBoost = 0.6f
            delay(300)
            delay(1200)
            activationBoost = 0f
            showTransformedText = false
        }
    }

    val brightness = idlePulse + activationBoost

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                showTransformedText = true
            },
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.omnitrix_symbol),
            contentDescription = "Omnitrix Symbol",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = brightness.coerceIn(0.1f, 5f)
                    rotationZ = animatedAngle
                }
        )
    }
}