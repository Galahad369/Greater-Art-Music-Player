package com.local.listentomusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.local.listentomusic.ui.theme.GaSpacing
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Opt-in parody. No SDK, network request, download, payment or notification. */
@Composable
fun FakeAdInterstitial(onSkip: () -> Unit, onOpenLink: () -> Unit) {
    var remaining by remember { mutableIntStateOf(5) }
    val corner = remember { Random.nextInt(4) }
    LaunchedEffect(Unit) { while (remaining > 0) { delay(1_000); remaining-- } }
    BackHandler { if (remaining == 0) onSkip() }
    val motion = rememberInfiniteTransition(label = "parody")
    val wobble by motion.animateFloat(-3f, 3f, infiniteRepeatable(tween(240), RepeatMode.Reverse), label = "wobble")
    val pulse by motion.animateFloat(0.97f, 1.04f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFFF00B8), Color(0xFF592EFF), Color(0xFF00FFCC)))).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize().padding(GaSpacing.xl), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("★ PREMIUM MEGA AI ★", color = Color.Yellow, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.graphicsLayer { rotationZ = wobble })
            Spacer(Modifier.height(GaSpacing.xl))
            Text("YOUR MUSIC\nNEEDS MORE\nBUTTONS!!!", color = Color.White, style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse })
            Spacer(Modifier.height(GaSpacing.xl))
            listOf("DOWNLOAD TO PLAY THIS NOW!", "UNLOCK 9000% MORE SOUND", "YES! GIVE ME ABSOLUTELY NOTHING").forEachIndexed { index, label ->
                Button(onClick = onOpenLink, colors = ButtonDefaults.buttonColors(containerColor = if (index % 2 == 0) Color.Yellow else Color.Cyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).graphicsLayer { rotationZ = if (index % 2 == 0) wobble else -wobble }) {
                    Text(label, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }
            Text("100% unnecessary • 0% improvement\nYou enabled this joke in Settings.", color = Color.White, textAlign = TextAlign.Center)
        }
        val alignment = when (corner) { 0 -> Alignment.TopStart; 1 -> Alignment.TopEnd; 2 -> Alignment.BottomStart; else -> Alignment.BottomEnd }
        Button(onClick = onSkip, enabled = remaining == 0, modifier = Modifier.align(alignment).padding(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White,
                disabledContainerColor = Color.Black, disabledContentColor = Color.White)) {
            Text(if (remaining > 0) "Skip in $remaining" else "Skip ad →")
        }
    }
}
