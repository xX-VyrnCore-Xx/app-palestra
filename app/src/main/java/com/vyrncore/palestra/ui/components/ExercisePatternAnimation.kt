package com.vyrncore.palestra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Movement patterns the animated demo glyphs can represent. Chosen per exercise from its
 * name/equipment so any catalog entry gets an animation without any network asset. */
enum class MovementPattern {
    HORIZONTAL_PRESS, // bench/panca family: barbell pressed off the chest while lying
    VERTICAL_PULL,    // lat machine / pull-down: bar travels down to the clavicle
    ROW,              // seated cable row: handle pulled into the torso
    SQUAT,            // squat / leg press: hips travel down and up under a barbell
    HINGE,            // deadlift / hip thrust: torso hinges at the hip, weight rises
    SHOULDER_PRESS,   // overhead press: barbell moves up from the shoulders
    CURL,             // biceps: forearm rotates up with a dumbbell
    EXTENSION,        // triceps pushdown: cable handle pressed down
    CORE,             // plank / crunch: braced torso with tempo pulse
    CARDIO,           // treadmill / bike: running rhythm
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
        n.contains("press") && (n.contains("spalle") || n.contains("military") || n.contains("arnold") || n.contains("shoulder") || n.contains("push press") || n.contains("russian press")) -> MovementPattern.SHOULDER_PRESS
        n.contains("curl") -> MovementPattern.CURL
        n.contains("push down") || n.contains("french") || n.contains("kick back") -> MovementPattern.EXTENSION
        n.contains("plank") || n.contains("crunch") || n.contains("core") || n.contains("addome") || n.contains("bug") || n.contains("superman") -> MovementPattern.CORE
        n.contains("tapis") || n.contains("cyclette") || n.contains("corsa") || n.contains("corda") || n.contains("burpee") || n.contains("jumping") -> MovementPattern.CARDIO
        else -> MovementPattern.GENERIC
    }
}

/**
 * A looping animated demonstration of the exercise's movement, drawn on Canvas - an inline
 * "GIF" that costs nothing (no network, no bytes), loops forever and works for every exercise.
 * Side-view skeleton figures with equipment props (barbell with plates, bench, cables,
 * dumbbells) and smoothstep-eased motion so the repetition looks natural.
 */
@Composable
fun ExercisePatternAnimation(
    exerciseName: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    base: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
) {
    val pattern = movementPatternFor(exerciseName)
    val transition = rememberInfiniteTransition(label = "exerciseDemo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "exerciseDemoPhase",
    )

    Canvas(modifier = modifier) {
        drawDemoScene(t, pattern, base, accent)
    }
}

private fun DrawScope.drawDemoScene(t: Float, pattern: MovementPattern, body: Color, equip: Color) {
    val w = size.width
    val h = size.height
    val lw = w * 0.085f // limb stroke width
    // smoothstep easing of the 0..1..0 swing for natural-looking reps.
    val swing = 1f - kotlin.math.abs(t * 2f - 1f)
    val e = swing * swing * (3f - 2f * swing)
    val dim = body.copy(alpha = 0.45f)

    fun head(c: Offset, r: Float = w * 0.082f) = drawCircle(body, r, c)
    fun limb(a: Offset, b: Offset, color: Color = body, width: Float = lw) =
        drawLine(color, a, b, width, StrokeCap.Round)
    fun torso(a: Offset, b: Offset) = drawLine(body, a, b, lw * 1.5f, StrokeCap.Round)
    fun ground(y: Float) = drawLine(dim, Offset(w * 0.06f, y), Offset(w * 0.94f, y), lw * 0.35f, StrokeCap.Round)

    /** Barbell seen from the side: short vertical bar with a plate at each end. */
    fun barbellSide(x: Float, y: Float) {
        drawLine(equip, Offset(x, y - h * 0.07f), Offset(x, y + h * 0.07f), lw * 0.5f, StrokeCap.Round)
        drawCircle(equip, w * 0.052f, Offset(x, y - h * 0.07f))
        drawCircle(equip, w * 0.052f, Offset(x, y + h * 0.07f))
    }

    /** Barbell seen from the front: horizontal bar with a plate at each end. */
    fun barbellFront(y: Float, halfW: Float) {
        drawLine(equip, Offset(w / 2 - halfW, y), Offset(w / 2 + halfW, y), lw * 0.55f, StrokeCap.Round)
        drawCircle(equip, w * 0.05f, Offset(w / 2 - halfW, y))
        drawCircle(equip, w * 0.05f, Offset(w / 2 + halfW, y))
    }

    fun dumbbell(c: Offset) {
        drawLine(equip, Offset(c.x - w * 0.055f, c.y), Offset(c.x + w * 0.055f, c.y), lw * 0.5f, StrokeCap.Round)
        drawCircle(equip, w * 0.035f, Offset(c.x - w * 0.055f, c.y))
        drawCircle(equip, w * 0.035f, Offset(c.x + w * 0.055f, c.y))
    }

    fun cable(a: Offset, b: Offset) = drawLine(equip.copy(alpha = 0.5f), a, b, lw * 0.28f)

    when (pattern) {
        MovementPattern.HORIZONTAL_PRESS -> {
            // Bench press, side view: lifter on a bench, barbell dips to the chest and back up.
            val benchY = h * 0.60f
            drawLine(dim, Offset(w * 0.14f, benchY), Offset(w * 0.86f, benchY), lw * 0.9f, StrokeCap.Round)
            limb(Offset(w * 0.24f, benchY), Offset(w * 0.24f, h * 0.94f), dim, lw * 0.4f)
            limb(Offset(w * 0.76f, benchY), Offset(w * 0.76f, h * 0.94f), dim, lw * 0.4f)

            val barY = h * (0.32f + e * 0.21f) // top 0.32h, at the chest 0.53h
            head(Offset(w * 0.20f, h * 0.565f))
            torso(Offset(w * 0.27f, h * 0.575f), Offset(w * 0.60f, h * 0.575f))
            limb(Offset(w * 0.60f, h * 0.575f), Offset(w * 0.72f, h * 0.66f))
            limb(Offset(w * 0.72f, h * 0.66f), Offset(w * 0.74f, h * 0.88f))
            limb(Offset(w * 0.74f, h * 0.88f), Offset(w * 0.84f, h * 0.90f))
            limb(Offset(w * 0.34f, h * 0.565f), Offset(w * 0.385f, barY))
            barbellSide(w * 0.385f, barY)
        }

        MovementPattern.VERTICAL_PULL -> {
            // Lat machine, front view: bar descends from above the head to the clavicle line,
            // with cable lines running up to the pulleys.
            val barY = h * (0.16f + e * 0.26f)
            head(Offset(w / 2, h * 0.30f))
            torso(Offset(w / 2, h * 0.39f), Offset(w / 2, h * 0.80f))
            limb(Offset(w / 2 - w * 0.08f, h * 0.40f), Offset(w / 2 - w * 0.24f, barY))
            limb(Offset(w / 2 + w * 0.08f, h * 0.40f), Offset(w / 2 + w * 0.24f, barY))
            cable(Offset(w * 0.10f, h * 0.05f), Offset(w / 2 - w * 0.24f, barY))
            cable(Offset(w * 0.90f, h * 0.05f), Offset(w / 2 + w * 0.24f, barY))
            drawLine(equip, Offset(w / 2 - w * 0.30f, barY), Offset(w / 2 + w * 0.30f, barY), lw * 0.55f, StrokeCap.Round)
            // Seat and thighs so the seated posture reads.
            drawLine(dim, Offset(w * 0.35f, h * 0.90f), Offset(w * 0.65f, h * 0.90f), lw * 0.7f, StrokeCap.Round)
            limb(Offset(w / 2, h * 0.80f), Offset(w * 0.36f, h * 0.88f), dim)
            limb(Offset(w / 2, h * 0.80f), Offset(w * 0.64f, h * 0.88f), dim)
        }

        MovementPattern.ROW -> {
            // Seated cable row, side view: torso rocks back while the handle comes into it.
            ground(h * 0.94f)
            drawLine(dim, Offset(w * 0.10f, h * 0.86f), Offset(w * 0.34f, h * 0.86f), lw * 0.5f, StrokeCap.Round)
            drawLine(dim, Offset(w * 0.56f, h * 0.66f), Offset(w * 0.82f, h * 0.66f), lw * 0.8f, StrokeCap.Round)

            val hip = Offset(w * 0.66f, h * 0.62f)
            val shoulder = Offset(w * (0.48f + e * 0.10f), h * 0.40f)
            head(shoulder + Offset(-w * 0.01f, -w * 0.115f))
            torso(shoulder, hip)
            val hand = Offset(w * (0.26f + e * 0.14f), h * 0.52f)
            limb(shoulder, hand)
            cable(Offset(w * 0.04f, h * 0.60f), hand)
            drawLine(equip, Offset(hand.x, hand.y - w * 0.05f), Offset(hand.x, hand.y + w * 0.05f), lw * 0.55f, StrokeCap.Round)
            limb(hip, Offset(w * 0.44f, h * 0.72f))
            limb(Offset(w * 0.44f, h * 0.72f), Offset(w * 0.30f, h * 0.86f))
        }

        MovementPattern.SQUAT -> {
            // Squat, side view: barbell on the shoulders rides down and up with the hips.
            ground(h * 0.92f)
            val shY = h * (0.36f + e * 0.16f)
            val hipY = shY + h * 0.22f
            head(Offset(w * 0.52f, shY - h * 0.10f))
            torso(Offset(w * 0.50f, shY), Offset(w * 0.50f, hipY))
            barbellSide(w * 0.50f, shY)
            val kneeX = w * (0.58f + e * 0.08f)
            val kneeY = h * 0.74f
            limb(Offset(w * 0.50f, hipY), Offset(kneeX, kneeY))
            limb(Offset(kneeX, kneeY), Offset(w * 0.54f, h * 0.92f))
            limb(Offset(w * 0.50f, hipY), Offset(kneeX - w * 0.06f, kneeY + h * 0.01f), dim)
            limb(Offset(kneeX - w * 0.06f, kneeY + h * 0.01f), Offset(w * 0.47f, h * 0.92f), dim)
        }

        MovementPattern.HINGE -> {
            // Deadlift / hip thrust, side view: the torso hinges while the bar rises to the hips.
            ground(h * 0.92f)
            val hip = Offset(w * 0.50f, h * 0.58f)
            val shoulder = Offset(w * (0.36f + 0.14f * e), h * (0.44f - 0.08f * e))
            head(shoulder + Offset(-w * 0.03f * (1f - e) - w * 0.02f, -w * 0.115f))
            torso(shoulder, hip)
            val handY = h * (0.78f - e * 0.20f)
            limb(shoulder, Offset(w * 0.52f, handY))
            barbellSide(w * 0.52f, handY)
            limb(hip, Offset(w * 0.56f, h * 0.75f))
            limb(Offset(w * 0.56f, h * 0.75f), Offset(w * 0.53f, h * 0.92f))
            limb(hip, Offset(w * 0.44f, h * 0.75f), dim)
            limb(Offset(w * 0.44f, h * 0.75f), Offset(w * 0.46f, h * 0.92f), dim)
        }

        MovementPattern.SHOULDER_PRESS -> {
            // Overhead press, front view: barbell with plates travels from the shoulders to lockout.
            head(Offset(w / 2, h * 0.28f))
            torso(Offset(w / 2, h * 0.37f), Offset(w / 2, h * 0.72f))
            val handY = h * (0.34f - e * 0.16f)
            limb(Offset(w / 2 - w * 0.14f, h * 0.40f), Offset(w / 2 - w * 0.20f, handY))
            limb(Offset(w / 2 + w * 0.14f, h * 0.40f), Offset(w / 2 + w * 0.20f, handY))
            barbellFront(handY, halfW = w * 0.26f)
        }

        MovementPattern.CURL -> {
            // Biceps curl, side view: the forearm rotates up around a fixed elbow.
            ground(h * 0.92f)
            head(Offset(w / 2, h * 0.22f))
            torso(Offset(w / 2, h * 0.31f), Offset(w / 2, h * 0.62f))
            val elbow = Offset(w * 0.53f, h * 0.50f)
            limb(Offset(w / 2, h * 0.33f), elbow)
            val hanging = Offset(w * 0.05f, h * 0.17f)
            val curled = Offset(w * 0.16f, -h * 0.07f)
            val hand = elbow + lerp(hanging, curled, e)
            limb(elbow, hand)
            dumbbell(hand)
            limb(Offset(w * 0.47f, h * 0.33f), Offset(w * 0.47f, h * 0.50f), dim)
        }

        MovementPattern.EXTENSION -> {
            // Triceps pushdown, side view: the cable handle travels from chest height to the hip.
            ground(h * 0.92f)
            head(Offset(w / 2, h * 0.20f))
            torso(Offset(w / 2, h * 0.29f), Offset(w / 2, h * 0.60f))
            val elbow = Offset(w * 0.54f, h * 0.46f)
            limb(Offset(w / 2, h * 0.31f), elbow)
            val up = Offset(-w * 0.02f, -h * 0.06f)
            val down = Offset(w * 0.01f, h * 0.17f)
            val hand = elbow + lerp(up, down, e)
            limb(elbow, hand)
            cable(Offset(w / 2, h * 0.04f), hand)
            drawLine(equip, Offset(hand.x - w * 0.04f, hand.y), Offset(hand.x + w * 0.04f, hand.y), lw * 0.5f, StrokeCap.Round)
        }

        MovementPattern.CORE -> {
            // Plank, side view: braced body line with a tempo pulse at the core.
            ground(h * 0.92f)
            val bodyY = h * (0.62f - e * 0.05f)
            head(Offset(w * 0.20f, bodyY - w * 0.04f))
            drawLine(body, Offset(w * 0.30f, bodyY), Offset(w * 0.76f, bodyY), lw * 1.4f, StrokeCap.Round)
            limb(Offset(w * 0.70f, bodyY), Offset(w * 0.72f, h * 0.92f))
            limb(Offset(w * 0.32f, bodyY), Offset(w * 0.34f, h * 0.92f))
            drawCircle(equip, w * (0.05f + e * 0.05f), Offset(w * 0.55f, bodyY - w * 0.12f))
        }

        MovementPattern.CARDIO -> {
            // Runner, front view: legs and arms swing in opposite phase.
            ground(h * 0.94f)
            val headC = Offset(w / 2, h * 0.24f)
            head(headC, w * 0.10f)
            torso(headC + Offset(0f, w * 0.12f), Offset(w / 2, h * 0.58f))
            val phase = t * 2f * kotlin.math.PI.toFloat()
            val legSwing = kotlin.math.sin(phase) * w * 0.16f
            limb(Offset(w / 2, h * 0.58f), Offset(w / 2 + legSwing, h * 0.90f))
            limb(Offset(w / 2, h * 0.58f), Offset(w / 2 - legSwing, h * 0.90f))
            limb(headC + Offset(0f, w * 0.13f), Offset(w / 2 - legSwing * 0.7f, h * 0.44f))
            limb(headC + Offset(0f, w * 0.13f), Offset(w / 2 + legSwing * 0.7f, h * 0.44f))
        }

        MovementPattern.GENERIC -> {
            // Front raise with dumbbells: the fallback movement that still reads as training.
            head(Offset(w / 2, h * 0.28f))
            torso(Offset(w / 2, h * 0.37f), Offset(w / 2, h * 0.72f))
            val handY = h * (0.55f - e * 0.16f)
            limb(Offset(w / 2 - w * 0.14f, h * 0.44f), Offset(w / 2 - w * 0.18f, handY))
            limb(Offset(w / 2 + w * 0.14f, h * 0.44f), Offset(w / 2 + w * 0.18f, handY))
            dumbbell(Offset(w / 2 - w * 0.18f, handY))
            dumbbell(Offset(w / 2 + w * 0.18f, handY))
        }
    }
}
