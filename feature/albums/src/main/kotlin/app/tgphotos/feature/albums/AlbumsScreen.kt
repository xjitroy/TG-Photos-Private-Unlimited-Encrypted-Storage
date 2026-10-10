package app.tgphotos.feature.albums

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tgphotos.core.designsystem.*

@Composable
fun AlbumsScreen(modifier: Modifier = Modifier) {
    val albums = listOf("Camera", "Screenshots", "Downloads", "Favorites", "Trips", "Videos")
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var policyPreview by remember { mutableStateOf(false) }
    LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SectionTitle("A place for\nevery memory.", "Albums · local folders")
            PreviewNotice()
            StatusCard("Choose what stays backed up", "Folder access and automatic backup are not connected. These folders are examples.", Icons.Outlined.Folder)
        } }
        itemsIndexed(albums, key = { _, name -> name }) { index, name ->
            Card(onClick = { selected = name; policyPreview = false }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                LandscapeArt(index, Modifier.fillMaxWidth().aspectRatio(1.22f))
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text("Sample folder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    selected?.let { name -> AlertDialog(onDismissRequest = { selected = null }, title = { Text(name) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Folder policy preview only. This switch does not access files, save a backup policy or schedule uploads.")
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Auto backup preview", Modifier.weight(1f))
                Switch(checked = policyPreview, onCheckedChange = { policyPreview = it })
            }
            Text("Live backup stays off until folder consent, encryption and Telegram transport are ready.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Done") } }) }
}
