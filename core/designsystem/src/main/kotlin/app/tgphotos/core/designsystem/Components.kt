package app.tgphotos.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PreviewNotice(modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Design preview · No account or files connected", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun StatusCard(title: String, description: String, icon: ImageVector = Icons.Outlined.CloudOff) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Original vector landscapes. No third-party photos, thumbnails or private media. */
@Composable
fun LandscapeArt(seed: Int, modifier: Modifier = Modifier) {
    val palettes = listOf(
        listOf(Color(0xFFD5EAE4), Color(0xFFA3C6BD), Color(0xFF5A8B80), Color(0xFF244E49)),
        listOf(Color(0xFFF6DDD2), Color(0xFFD5AEA8), Color(0xFFAD818C), Color(0xFF69506F)),
        listOf(Color(0xFFDAE5FC), Color(0xFFA6B8E0), Color(0xFF748DCA), Color(0xFF405D9B)),
    )
    val colors = palettes[Math.floorMod(seed, palettes.size)]
    Canvas(modifier.clip(RoundedCornerShape(12.dp)).semantics { contentDescription = "Original landscape illustration" }) {
        drawRect(Brush.verticalGradient(listOf(colors[0], colors[1])))
        drawCircle(Color(0xFFFFF3D3), size.width * .1f, Offset(size.width * .73f, size.height * .25f))
        for (layer in 1..3) {
            val path = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, size.height * (.4f + layer * .12f))
                lineTo(size.width * .3f, size.height * (.14f + layer * .16f))
                lineTo(size.width * .54f, size.height * (.47f + layer * .08f))
                lineTo(size.width * .82f, size.height * (.28f + layer * .16f))
                lineTo(size.width, size.height * (.48f + layer * .1f))
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, colors[layer])
        }
    }
}

@Composable
fun FeatureRow(title: String, subtitle: String, icon: ImageVector, onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.invoke()
        }
    }
    if (onClick == null) Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, content = content)
    else Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, content = content)
}
