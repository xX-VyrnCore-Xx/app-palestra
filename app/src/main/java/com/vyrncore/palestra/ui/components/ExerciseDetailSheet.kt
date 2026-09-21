package com.vyrncore.palestra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vyrncore.palestra.data.local.entity.ExerciseEntity
import com.vyrncore.palestra.util.exerciseMedia
import com.vyrncore.palestra.util.youtubeThumbnailUrl

/** Full exercise sheet: hero media (image or gradient artwork), play-tutorial button,
 * recommended machine badge (Technogym/Panatta) and the technique notes. Shared by the
 * exercise picker, global search and the workout screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailSheet(
    exercise: ExerciseEntity,
    onDismiss: () -> Unit,
    historySlot: (@Composable () -> Unit)? = null,
) {
    val media = remember(exercise.name, exercise.equipment) {
        exerciseMedia(exercise.name, exercise.equipment)
    }
    val uriHandler = LocalUriHandler.current
    var showVideo by remember { mutableStateOf(false) }

    if (showVideo) {
        InAppVideoDialog(url = media.videoUrl, onDismiss = { showVideo = false })
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            // Hero media: the exercise image when available, otherwise a YouTube thumbnail
            // of the demo video when one is mapped, otherwise the muscle-group artwork -
            // always overlaid with the play button so the video is the obvious tap.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showVideo = true },
            ) {
                val thumbnail = remember(media.videoUrl) { youtubeThumbnailUrl(media.videoUrl) }
                when {
                    exercise.imageUrl != null -> AsyncImage(
                        model = exercise.imageUrl,
                        contentDescription = exercise.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                    )
                    thumbnail != null -> AsyncImage(
                        model = thumbnail,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                    )
                    else -> MuscleGroupArtwork(group = exercise.muscleGroup ?: "", modifier = Modifier.fillMaxWidth().height(180.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.55f),
                            )
                        ),
                )
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.align(Alignment.Center).size(64.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Guarda il video dimostrativo",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Text(
                    "VIDEO TUTORIAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                )
            }

            Text(
                exercise.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                DifficultyRank(difficulty = exercise.difficulty)
                exercise.equipment?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Recommended machine: brand badge + catalog name, links to the official page.
            if (media.machineBrand != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clickable { uriHandler.openUri(media.machineBrand.siteUrl) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                "MACCHINARIO CONSIGLIATO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            )
                            Text(
                                media.machineName ?: media.machineBrand.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Icon(
                            Icons.Filled.OpenInNew,
                            contentDescription = "Apri il sito ${media.machineBrand.displayName}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            exercise.notes?.let { notes ->
                Text(
                    "TECNICA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Text(
                    notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            historySlot?.invoke()
        }
    }
}
