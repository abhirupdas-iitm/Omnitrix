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

    val baseAlpha by transition.animateFloat(
        transitionSpec = { tween(200) },
        label = "baseAlpha"
    ) {
        when (it) {
            OmnitrixState.Activating -> 0.4f
            OmnitrixState.Flash -> 0f
            else -> 1f
        }
    }

    val centerAlpha by transition.animateFloat(
        transitionSpec = { tween(250) },
        label = "centerAlpha"
    ) {
        when (it) {
            OmnitrixState.Flash -> 1f
            else -> 0f
        }
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

        // BASE SYMBOL
        Image(
            painter = painterResource(R.drawable.omnitrix_symbol),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = animatedAngle
                    alpha = baseAlpha * brightness.coerceIn(0.1f, 5f)
                }
        )

        // 🔥 FIXED DIAMOND (NOW STATE CONTROLLED)
        Image(
            painter = painterResource(R.drawable.omnitrix_center_diamond),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(0.9f)
                .graphicsLayer {
                    scaleX = 1.4f
                    scaleY = 1.15f
                    alpha = centerAlpha   // 🔥 THIS LINE FIXES EVERYTHING
                }
        )
    }
}