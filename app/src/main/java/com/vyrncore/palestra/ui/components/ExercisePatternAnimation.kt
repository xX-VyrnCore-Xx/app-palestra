package com.vyrncore.palestra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate

/** Movement patterns the animated demo glyphs can represent. Chosen per exercise from its
 * name/equipment so any catalog entry gets an animation without any network asset. */
enum class MovementPattern {
    HORIZONTAL_PRESS, // chest press / bench family: bar moves toward the viewer's chest
    VERTICAL_PULL,    // lat machine / pull-down: bar moves down toward the chest
    ROW,              // low row: handle pulled toward the torso
    SQUAT,            // squat / leg press: hips travel down and up
    HINGE,            // deadlift / hip thrust / RDL: torso hinges at the hip
    SHOULDER_PRESS,   // overhead press: bar moves up
    CURL,             // biceps: forearm rotates up
    EXTENSION,        // triceps / leg extension: limb extends
    CORE,             // plank / crunch: braced torso
    CARDIO,           // treadmill / bike: steady rhythm dots
    GENERIC,
}

/** Maps an exercise name to its dominant movement pattern. */
fun movementPatternFor(name: String): MovementPattern {
    val n = name.lowercase()
    return when {
        n.contains("panca") || n.contains("chest press") || n.contains("croci") || n.contains("dip") -> MovementPattern.HORIZONTAL_PRESS
        n.contains("lat machine") || n.contains("trazioni") || n.contains("pull") || n.contains("pullover") -> MovementPattern.VERTICAL_PULL
        n.contains("rematore") || n.contains("pulley") || n.contains("row") -> MovementPattern.ROW
        n.contains("squat") || n.contains("leg press") || n.contains("affondi") || n.contains("step") || n.contains("leg extension") -> MovementPattern.SQUAT
        n.contains("stacco") || n.contains("hip thrust") || n.contains("curl femorale") || n.contains("leg curl") -> MovementPattern.HINGE
        n.contains("press") && (n.contains("spalle") || n.contains("military") || n.contains("arnold") || n.contains("shoulder")) -> MovementPattern.SHOULDER_PRESS
        n.contains("curl") -> MovementPattern.CURL
        n.contains("push down") || n.contains("french") || n.contains("kick back") -> MovementPattern.EXTENSION
        n.contains("plank") || n.contains("crunch") || n.contains("core") || n.contains("addome") || n.contains("bug") || n.contains("superman") -> MovementPattern.CORE
        n.contains("cardio") || n.contains("tapis") || n.contains("cyclette") || n.contains("corsa") || n.contains("corda") -> MovementPattern.CARDIO
        else -> MovementPattern.GENERIC
    }
}

/**
 * A looping animated glyph of the exercise's movement, drawn on Canvas - an inline "demo GIF"
 * that costs nothing (no network, no bytes), loops indefinitely and works for every exercise.
 * Drawn inside a size x size box next to the exercise name in the active workout card.
 */
@Composable
fun ExercisePatternAnimation(
    exerciseName: String,
    modifier: Modifier = Modifier,
    accent: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    base: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
) {
    val pattern = movementPatternFor(exerciseName)
    val transition = rememberInfiniteTransition(label = "exerciseDemo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "exerciseDemoPhase",
    )

    Canvas(modifier = modifier) {
        drawPerson(t, pattern, accent, base)
    }
}

/** Skeleton drawing: head + torso as circles/lines, limbs as rounded strokes, animated by phase t. */
private fun DrawScope.drawPerson(t: Float, pattern: MovementPattern, accent: Color, base: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val limbWidth = w * 0.09f
    val torsoColor = base
    val limbColor = accent

    fun head(c: Offset, r: Float) = drawCircle(torsoColor, radius = r, center = c)
    fun limb(from: Offset, to: Offset, color: Color = limbColor) =
        drawLine(color, from, to, strokeWidth = limbWidth, cap = StrokeCap.Round)
    fun torso(top: Offset, bottom: Offset) = drawLine(torsoColor, top, bottom, strokeWidth = limbWidth * 1.6f, cap = StrokeCap.Round)

    // Oscillators: 0..1..0 over the loop for the primary and secondary motion.
    val swing = (1f - kotlin.math.abs(t * 2f - 1f))
    val lift = t.coerceIn(0f, 1f)

    when (pattern) {
        MovementPattern.HORIZONTAL_PRESS -> {
            val headC = Offset(cx, h * 0.28f)
            head(headC, w * 0.11f)
            torso(headC + Offset(0f, w * 0.13f), Offset(cx, h * 0.72f))
            // Arms press "out" toward the viewer: hands travel down the chest line.
            val elbowY = h * (0.52f + swing * 0.10f)
            limb(Offset(cx - w * 0.16f, h * 0.36f), Offset(cx - w * 0.26f, elbowY))
            limb(Offset(cx + w * 0.16f, h * 0.36f), Offset(cx + w * 0.26f, elbowY))
            // The bar: a horizontal accent line whose thickness = closeness to chest.
            val barY = h * (0.36f - swing * 0.06f)
            drawLine(
                accent,
                Offset(cx - w * 0.34f, barY),
                Offset(cx + w * 0.34f, barY),
                strokeWidth = limbWidth * (0.8f + swing * 0.6f),
                cap = StrokeCap.Round,
            )
        }
        MovementPattern.VERTICAL_PULL -> {
            val headC = Offset(cx, h * (0.24f + swing * 0.03f))
            head(headC, w * 0.11f)
            torso(headC + Offset(0f, w * 0.13f), Offset(cx, h * 0.78f))
            // Bar travels down from above the head to the clavicle line.
            val barY = h * (0.14f + swing * 0.28f)
            limb(headC + Offset(-w * 0.10f, w * 0.10f), Offset(cx - w * 0.26f, barY))
            limb(headC + Offset(w * 0.10f, w * 0.10f), Offset(cx + w * 0.26f, barY))
            drawLine(accent, Offset(cx - w * 0.32f, barY), Offset(cx + w * 0.32f, barY), limbWidth, StrokeCap.Round)
        }
        MovementPattern.ROW -> {
            val headC = Offset(cx, h * 0.30f)
            head(headC, w * 0.11f)
            torso(headC + Offset(0f, w * 0.13f), Offset(cx - w * 0.05f, h * 0.74f))
            // Handle pulled into the torso.
            val handX = cx - w * (0.34f - swing * 0.16f)
            limb(Offset(cx - w * 0.12f, h * 0.48f), Offset(handX, h * 0.50f))
            drawLine(accent, Offset(handX - w * 0.06f, h * 0.50f), Offset(handX + w * 0.02f, h * 0.50f), limbWidth, StrokeCap.Round)
        }
        MovementPattern.SQUAT -> {
            val hipY = h * (0.62f - swing * 0.14f) // hips rise and fall
            val headC = Offset(cx, hipY - h * 0.30f)
            head(headC, w * 0.11f)
            torso(headC + Offset(0f, w * 0.13f), Offset(cx, hipY))
            // Thigh + shin, knee bends as hips drop.
            val kneeX = cx + w * (0.12f + swing * 0.10f)
            limb(Offset(cx, hipY), Offset(kneeX, h * 0.80f))
            limb(Offset(kneeX, h * 0.80f), Offset(cx + w * 0.04f, h * 0.94f))
            limb(Offset(cx, hipY), Offset(cx - w * 0.10f, h * 0.94f))
            // Bar across the shoulders.
            drawLine(accent, Offset(cx - w * 0.22f, hipY - h * 0.26f), Offset(cx + w * 0.22f, hipY - h * 0.26f), limbWidth * 0.7f, StrokeCap.Round)
        }
        MovementPattern.HINGE -> {
            val lean = 0.15f + swing * 0.35f // torso angle factor
            val hip = Offset(cx, h * 0.60f)
            val headC = hip + Offset(-w * lean, -h * 0.28f)
            head(headC, w * 0.11f)
            torso(headC + Offset(w * lean * 0.6f, w * 0.14f), hip)
            limb(hip, Offset(cx - w * 0.12f, h * 0.94f))
            limb(hip, Offset(cx + w * 0.12f, h * 0.94f))
            // Weight plate rises with the hinge.
            val plateY = h * (0.88f - lift * 0.06f)
            drawCircle(accent, radius = w * 0.10f, center = Offset(cx + w * 0.26f, plateY))
        }
        MovementPattern.SHOULDER_PRESS -> {
            val headC = Offset(cx, h * 0.30f)
            head(headC, w * 0.11f)
            torso(headC + Offset(0f, w * 0.13f), Offset(cx, h * 0.72f))
            val handY = h * (0.34f - swing * 0.18f)
            limb(Offset(cx - w * 0.14f, h * 0.40f), Offset(cx - w * 0.20f, handY))
            limb(Offset(cx + w * 0.14f, h * 0.40f), Offset(cx + w * 0.20f, handY))
            drawLine(accent, Offset(cx - w * 0.30f, handY), Offset(cx + w * 0.30f, handY), limbWidth, StrokeCap.Round)
        }
        MovementPattern.CURL -> {
            val headC = Offset(cx, h * 0.24f)
            head(headC, w * 0.11f)
            torso(headC + Offset(0f, w * 0.13f), Offset(cx, h * 0.80f))
            // Forearm rotates up around the elbow.
            val angle = -100f + swing * 110f
            rotate(degrees = angle, pivot = Offset(cx - w * 0.02f, h * 0.52f)) {
                limb(Offset(cx - w * 0.02f, h * 0.52f), Offset(cx + w * 0.28f, h * 0.52f))
                drawCircle(accent, radius = w * 0.08f, center = Offset(cx + w * 0.30f, h * 0.52f))
            }
            limb(Offset(cx - w * 0.02f, h * 0.52f), Offset(cx - w * 0.02f, h * 0.74f))
        }
        MovementPattern.EXTENSION -> {
            val headC = Offset(cx, h * 0.30f)
            head(headC, w * 0.11f)
            torso(headC + Offset(0f, w * 0.13f), Offset(cx, h * 0.74f))
            // Forearm extends from folded to straight.
            val elbow = Offset(cx - w * 0.16f, h * 0.50f)
            val hand = elbow + Offset(-w * (0.10f + swing * 0.14f), w * (0.10f - swing * 0.10f))
            limb(elbow, Offset(cx - w * 0.04f, h * 0.40f))
            limb(elbow, hand)
            drawCircle(accent, radius = w * 0.08f, center = hand)
        }
        MovementPattern.CORE -> {
            // Braced plank with breathing cue: body line gently rises.
            val bodyY = h * (0.62f - swing * 0.05f)
            head(Offset(cx - w * 0.30f, bodyY - w * 0.04f), w * 0.10f)
            drawLine(torsoColor, Offset(cx - w * 0.20f, bodyY), Offset(cx + w * 0.26f, bodyY), limbWidth * 1.4f, StrokeCap.Round)
            limb(Offset(cx + w * 0.20f, bodyY), Offset(cx + w * 0.22f, h * 0.92f))
            limb(Offset(cx - w * 0.18f, bodyY), Offset(cx - w * 0.16f, h * 0.92f))
            // Tempo dot pulses at the core.
            drawCircle(accent, radius = w * (0.05f + swing * 0.05f), center = Offset(cx + w * 0.05f, bodyY - w * 0.12f))
        }
        MovementPattern.CARDIO -> {
            val headC = Offset(cx, h * 0.24f)
            head(headC, w * 0.10f)
            torso(headC + Offset(0f, w * 0.12f), Offset(cx, h * 0.58f))
            // Running legs & arms at phase t.
            val phase = t * 2f * kotlin.math.PI.toFloat()
            val legSwing = kotlin.math.sin(phase) * w * 0.16f
            limb(Offset(cx, h * 0.58f), Offset(cx + legSwing, h * 0.90f))
            limb(Offset(cx, h * 0.58f), Offset(cx - legSwing, h * 0.90f))
            limb(headC + Offset(0f, w * 0.13f), Offset(cx - legSwing * 0.7f, h * 0.44f))
            limb(headC + Offset(0f, w * 0.13f), Offset(cx + legSwing * 0.7f, h * 0.44f))
        }
        MovementPattern.GENERIC -> {
            // Dumbbell curl-to-press hybrid: two dots orbit to imply movement.
            val headC = Offset(cx, h * 0.28f)
            head(headC, w * 0.11f)
            torso(headC + Offset(0f, w * 0.13f), Offset(cx, h * 0.72f))
            limb(Offset(cx - w * 0.14f, h * 0.44f), Offset(cx - w * 0.20f, h * (0.52f - swing * 0.16f)))
            limb(Offset(cx + w * 0.14f, h * 0.44f), Offset(cx + w * 0.20f, h * (0.52f - swing * 0.16f)))
            drawCircle(accent, radius = w * 0.07f, center = Offset(cx - w * 0.20f, h * (0.55f - swing * 0.16f)))
            drawCircle(accent, radius = w * 0.07f, center = Offset(cx + w * 0.20f, h * (0.55f - swing * 0.16f)))
        }
    }
}
