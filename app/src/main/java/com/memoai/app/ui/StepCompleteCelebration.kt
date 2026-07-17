package com.memoai.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Spark(
    val angle: Float,
    val speed: Float,
    val color: Color,
    val size: Float
)

@Composable
fun StepCompleteCelebration(
    shellCount: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val sparks = remember {
        List(36) {
            Spark(
                angle = Random.nextFloat() * 360f,
                speed = 80f + Random.nextFloat() * 160f,
                color = listOf(
                    Color(0xFFFF6B6B),
                    Color(0xFFFFD93D),
                    Color(0xFF6BCB77),
                    Color(0xFF4D96FF),
                    Color(0xFFFF922B)
                ).random(),
                size = 4f + Random.nextFloat() * 6f
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        delay(1600)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            val p = progress.value
            sparks.forEach { spark ->
                val rad = Math.toRadians(spark.angle.toDouble())
                val dist = spark.speed * p
                val x = center.x + cos(rad).toFloat() * dist
                val y = center.y + sin(rad).toFloat() * dist
                val alpha = (1f - p).coerceIn(0f, 1f)
                drawCircle(
                    color = spark.color.copy(alpha = alpha),
                    radius = spark.size * (1f - p * 0.35f),
                    center = Offset(x, y)
                )
            }
            if (p < 0.35f) {
                drawCircle(
                    color = Color(0xFFFFD93D).copy(alpha = 0.5f * (1f - p / 0.35f)),
                    radius = 36f + p * 80f,
                    center = center
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🎆", fontSize = 28.sp)
                Text(
                    text = "太棒啦！完成此任务获得了${shellCount}个🐚",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1C1E),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
