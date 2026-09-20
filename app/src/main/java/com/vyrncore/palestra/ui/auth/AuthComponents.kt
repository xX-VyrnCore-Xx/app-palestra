package com.vyrncore.palestra.ui.auth

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vyrncore.palestra.ui.components.pressScale
import com.vyrncore.palestra.ui.theme.Magenta60
import com.vyrncore.palestra.ui.theme.Orange50
import com.vyrncore.palestra.ui.theme.Violet20
import com.vyrncore.palestra.ui.theme.Violet40

/**
 * Full-screen animated gradient backdrop shared by Login and Register: three brand hues slowly
 * drifting into each other (Technogym-style "living" hero surface) plus two soft glowing orbs
 * for depth. Cheap to render - two infinite transitions, no bitmaps.
 */
@Composable
fun AnimatedAuthBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "authBackground")

    // Slow hue drift: the gradient's anchor colors crossfade between their brand value and a
    // neighbour, so the whole surface feels like it's breathing instead of being a static image.
    val shiftA by transition.animateColor(
        initialValue = Orange50,
        targetValue = Magenta60,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "authBgA",
    )
    val shiftB by transition.animateColor(
        initialValue = Violet40,
        targetValue = Violet20,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "authBgB",
    )
    val orbPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "authBgOrb",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(shiftB, shiftA.copy(alpha = 0.55f), shiftB)),
            ),
    ) {
        // Soft glowing orbs: pure decoration, alpha modulated by the same phase so they pulse
        // gently rather than blink.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 24.dp)
                .size(180.dp)
                .graphicsLayer(alpha = 0.18f + 0.08f * orbPhase)
                .background(Brush.radialGradient(listOf(Color.White, Color.Transparent)), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 100.dp, start = 12.dp)
                .size(220.dp)
                .graphicsLayer(alpha = 0.12f + 0.08f * (1f - orbPhase))
                .background(Brush.radialGradient(listOf(Color.White, Color.Transparent)), CircleShape),
        )
    }
}

/**
 * Frosted-glass card floating over [AnimatedAuthBackground]: translucent white surface, rounded
 * 28dp corners and a soft shadow, holding the form controls on top of the vivid hero gradient.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 2.dp,
    ) {
        content()
    }
}

/** Circular brand badge (dumbbell) that springs in above the card title. */
@Composable
fun BrandBadge(modifier: Modifier = Modifier, icon: ImageVector) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "brandBadgeScale",
    )
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = modifier
            .size(72.dp)
            .scale(scale),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(listOf(Orange50, Magenta60)),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * Outlined text field tuned for the dark hero surface: filled dark container so the label stays
 * readable over the gradient, animated container color on focus, inline validation message that
 * fades/zooms in when non-null.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val focusedContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    val unfocusedContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val interaction = remember { MutableInteractionSource() }
    val containerColor by animateColorAsState(
        targetValue = if (interaction.collectIsFocusedAsState().value) focusedContainer else unfocusedContainer,
        animationSpec = tween(200),
        label = "authFieldContainer",
    )

    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null) },
            trailingIcon = trailingContent,
            isError = isError,
            singleLine = singleLine,
            visualTransformation = if (isPassword && !passwordVisible) {
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            } else {
                androidx.compose.ui.text.input.VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                errorBorderColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.animation.AnimatedVisibility(
            visible = errorMessage != null,
            enter = androidx.compose.animation.fadeIn(tween(150)) +
                androidx.compose.animation.expandVertically(tween(150)),
            exit = androidx.compose.animation.fadeOut(tween(100)) +
                androidx.compose.animation.shrinkVertically(tween(100)),
        ) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
    }
}

/**
 * Primary call-to-action: full-width gradient button with a press "give" (pressScale) and a
 * loading state that swaps label for spinner without changing the button's size.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.5f),
        ),
        interactionSource = interaction,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .pressScale(interaction)
            .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(16.dp), spotColor = Magenta60)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(Orange50, Magenta60)) else Brush.horizontalGradient(listOf(Violet40, Violet40)),
                RoundedCornerShape(16.dp),
            ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Small helper for the role chips used in Register: selected state animates via FilterChip. */
@Composable
fun RoleChip(selected: Boolean, label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        label = { Text(label) },
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier,
    )
}
