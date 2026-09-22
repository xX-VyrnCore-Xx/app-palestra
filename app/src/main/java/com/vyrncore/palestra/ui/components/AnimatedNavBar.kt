package com.vyrncore.palestra.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class NavBarItem(
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
)

/**
 * A softer, more "alive" replacement for the plain Material3 NavigationBar: the selected item
 * grows a pill-shaped highlight and its icon springs up slightly, instead of the flat instant
 * color swap Material gives you by default.
 */
@Composable
fun AnimatedNavBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    emphasizedIndex: Int? = null,
) {
    val hairline = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                // A real 1dp hairline on the top edge only - the previous padding+background
                // combo tinted the whole bar with the outline color instead of drawing a border.
                .drawBehind {
                    drawLine(
                        color = hairline,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                // Reserves space for Android's own gesture pill / 3-button bar below the tab
                // labels, on the outer layer so the surface color extends behind it too.
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    if (index == emphasizedIndex) {
                        EmphasizedNavBarTab(
                            item = item,
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        NavBarTab(
                            item = item,
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmphasizedNavBarTab(
    item: NavBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bubbleColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(220),
        label = "navHubColor",
    )
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navHubScale",
    )

    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Tab,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp * scale)
                .clip(RoundedCornerShape(50))
                .background(bubbleColor),
            contentAlignment = Alignment.Center,
        ) {
            if (item.badgeCount > 0) {
                androidx.compose.material3.BadgedBox(
                    badge = { androidx.compose.material3.Badge { Text(badgeLabel(item.badgeCount)) } },
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = contentColor)
                }
            } else {
                Icon(item.icon, contentDescription = item.label, tint = contentColor)
            }
        }
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun NavBarTab(
    item: NavBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(220),
        label = "navPillColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "navContentColor",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navIconScale",
    )
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 56.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navPillWidth",
    )

    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Tab,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = pillWidth, height = 32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(pillColor),
            )
            Box(
                modifier = Modifier.size(24.dp * iconScale),
                contentAlignment = Alignment.Center,
            ) {
                if (item.badgeCount > 0) {
                    androidx.compose.material3.BadgedBox(
                        badge = {
                            androidx.compose.material3.Badge { Text(badgeLabel(item.badgeCount)) }
                        },
                    ) {
                        Icon(item.icon, contentDescription = item.label, tint = contentColor)
                    }
                } else {
                    Icon(item.icon, contentDescription = item.label, tint = contentColor)
                }
            }
        }
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Keeps the badge compact on small screens: anything above 99 reads as "99+". */
private fun badgeLabel(count: Int): String = if (count > 99) "99+" else count.toString()
