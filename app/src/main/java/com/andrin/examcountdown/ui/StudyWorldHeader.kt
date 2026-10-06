package com.andrin.examcountdown.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.R
import com.andrin.examcountdown.ui.theme.LocalDecorativeArtEnabled

/** A shared world with a distinct destination for each real app task. */
internal enum class StudyScene(val artwork: Int, val icon: ImageVector) {
    EXAMS(R.drawable.world_exams, Icons.Outlined.School),
    TIMETABLE(R.drawable.world_timetable, Icons.Outlined.Schedule),
    AGENDA(R.drawable.world_agenda, Icons.Outlined.CalendarToday),
    GRADES(R.drawable.world_grades, Icons.Outlined.Calculate),
    SETTINGS(R.drawable.world_settings, Icons.Outlined.Settings)
}

/** Decorative art never carries information. Text stays on an opaque, tested theme surface. */
@Composable
internal fun StudyWorldHeader(
    title: String,
    subtitle: String,
    illustrated: Boolean = false,
    modifier: Modifier = Modifier,
    scene: StudyScene = StudyScene.EXAMS
) {
    val colors = MaterialTheme.colorScheme
    val artEnabled = illustrated && LocalDecorativeArtEnabled.current
    val largeText = LocalDensity.current.fontScale > 1.3f
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f)),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (artEnabled) 2.dp else 0.dp)
    ) {
        if (artEnabled) {
            Image(
                painter = painterResource(scene.artwork), contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(if (largeText) 64.dp else 112.dp)
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!largeText) {
                Surface(shape = MaterialTheme.shapes.medium, color = colors.primaryContainer) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Icon(scene.icon, null, Modifier.size(23.dp), tint = colors.onPrimaryContainer)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge,
                    fontFamily = if (artEnabled) FontFamily.Serif else FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold, color = colors.onSurface,
                    modifier = Modifier.semantics { heading() })
                if (subtitle.isNotBlank()) Text(subtitle,
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}
