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
                        soundPool.play(activateSoundId, 1f, 1f, 1, 0, 1f)
                    }
                },
                onTick = {
                    if (soundLoaded) {
                        soundPool.play(tickSoundId, 0.7f, 0.7f, 1, 0, 1f)
                    }
                }
            )
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL &&
            event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)
        ) {
            val delta = event.getAxisValue(MotionEvent.AXIS_SCROLL)
            dialAngleState.floatValue += delta * 10f
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
    }
}

enum class OmnitrixState {
    Idle,
    Activating,
    DNA,
    Flash
}

@Composable
fun OmnitrixScreen(
    dialAngle: Float,
    onActivate: () -> Unit,
    onTick: () -> Unit
) {

    val slotCount = 15
    val slotAngle = 360f / slotCount

    val snappedIndex = (dialAngle / slotAngle).roundToInt()
    val snappedAngle = snappedIndex * slotAngle

    val animatedAngle by animateFloatAsState(
        targetValue = snappedAngle,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "dialAnimation"
    )

    val slot = ((snappedIndex % slotCount) + slotCount) % slotCount

    var previousSlot by remember { mutableIntStateOf(slot) }

    LaunchedEffect(slot) {
        if (slot != previousSlot) {
            onTick()
            previousSlot = slot
        }
    }

    var state by remember { mutableStateOf(OmnitrixState.Idle) }

    val transition = updateTransition(state, label = "omnitrixTransition")

    // 🔥 NEW: Hourglass movement
    val hourglassOffset by transition.animateFloat(
        transitionSpec = { tween(400, easing = FastOutSlowInEasing) },
        label = "hourglassOffset"
    ) { state ->
        when (state) {
            OmnitrixState.Idle -> 90f
            OmnitrixState.Activating -> 30f
            OmnitrixState.DNA -> -10f
            OmnitrixState.Flash -> -10f
        }
    }

    // 🔥 NEW: Center diamond visibility
    val centerAlpha by transition.animateFloat(
        transitionSpec = { tween(150) },
        label = "centerAlpha"
    ) { state ->
        when (state) {
            OmnitrixState.DNA,
            OmnitrixState.Flash -> 1f
            else -> 0f
        }
    }

    val flashAlpha by transition.animateFloat(
        transitionSpec = { tween(120) },
        label = "flashAlpha"
    ) {
        if (it == OmnitrixState.Flash) 1f else 0f
    }

    var activationBoost by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition()
    val idlePulse by infiniteTransition.animateFloat(
        0.6f, 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(state) {
        if (state == OmnitrixState.Activating) {

            onActivate()
            activationBoost = 0.6f

            delay(300)
            state = OmnitrixState.DNA

            delay(400)
            state = OmnitrixState.Flash

            delay(200)
            activationBoost = 0f
            state = OmnitrixState.Idle
        }
    }

    val brightness = idlePulse + activationBoost

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                state = OmnitrixState.Activating
            },
        contentAlignment = Alignment.Center
    ) {

        // 🔵 LEFT TRIANGLE
        Image(
            painter = painterResource(R.drawable.hourglass_left),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = brightness.coerceIn(0.1f, 5f)
                    translationX = -hourglassOffset
                }
        )

        // 🔵 RIGHT TRIANGLE
        Image(
            painter = painterResource(R.drawable.hourglass_right),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = brightness.coerceIn(0.1f, 5f)
                    translationX = hourglassOffset
                }
        )

        // 🔥 CENTER DIAMOND
        Image(
            painter = painterResource(R.drawable.omnitrix_center_diamond),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = centerAlpha * brightness.coerceIn(0.1f, 5f)
                }
        )

        // FLASH
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Green.copy(alpha = flashAlpha))
        )
    }
}