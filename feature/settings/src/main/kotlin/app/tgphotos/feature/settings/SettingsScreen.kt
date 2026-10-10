package app.tgphotos.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tgphotos.core.designsystem.*
import app.tgphotos.core.model.ThemeMode

@Composable
fun SettingsScreen(mode: ThemeMode, dynamicColors: Boolean, onModeChange: (ThemeMode) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit, onAccount: () -> Unit, modifier: Modifier = Modifier) {
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Make it yours.", "TG Photos · Settings") }
        item { PreviewNotice() }
        item { FeatureRow("Telegram account", "Not connected · explore login screens", Icons.Outlined.AccountCircle, onClick = onAccount) }
        item { Text("APPEARANCE", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp)) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { value -> FilterChip(selected = mode == value, onClick = { onModeChange(value) }, label = { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
        } }
        item { FeatureRow("Dynamic colors", "Use your Android 12+ wallpaper palette", Icons.Outlined.Palette,
            trailing = { Switch(checked = dynamicColors, onCheckedChange = onDynamicColorsChange) }) }
        item { Text("YOUR PRIVATE SPACE", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp)) }
        item { FeatureRow("Backup & sync", "Folder policies · background queue", Icons.Outlined.CloudUpload, onClick = { detail = "Backup & sync" }) }
        item { FeatureRow("Storage & cache", "Encrypted previews · cache budget", Icons.Outlined.Storage, onClick = { detail = "Storage & cache" }) }
        item { FeatureRow("Security & recovery", "Private keys · recovery · vault", Icons.Outlined.Security, onClick = { detail = "Security & recovery" }) }
        item { FeatureRow("Player", "Ratios · audio · subtitles", Icons.Outlined.PlayCircle, onClick = { detail = "Player" }) }
        item { FeatureRow("Privacy", "Your data has no third-party app backend", Icons.Outlined.PrivacyTip, onClick = { detail = "Privacy" }) }
        item { FeatureRow("About TG Photos", "0.1.0 · design preview", Icons.Outlined.Info, onClick = { detail = "About TG Photos" }) }
        item { FeatureRow("Third-party notices", "Dependency licenses and source references", Icons.Outlined.Description, onClick = { detail = "Third-party notices" }) }
    }
    detail?.let { title -> AlertDialog(onDismissRequest = { detail = null }, title = { Text(title) }, text = {
        Text(when (title) {
            "Backup & sync" -> "Live backup is not integrated. Planned controls: per-folder consent, Wi-Fi only, charging only and retry progress. No files are uploaded or deleted in this preview."
            "Storage & cache" -> "No media cache exists in this preview. Persistent thumbnails, manifests and prefetch data must be encrypted in app-private storage."
            "Security & recovery" -> "Encryption, Android Keystore key wrapping, recovery export, biometrics and automatic vault locking are pending integration and independent security review. No keys or passwords are collected."
            "Player" -> "Media3 integration is planned for 9:16, 1:1 and 16:9 layouts, track selection and resume. Encrypted playback must authenticate chunks before the decoder receives bytes."
            "Privacy" -> "This preview has no internet permission, analytics, advertising or crash-upload SDK. Referenced apps do not receive your data. Future approved backups send encrypted content to Telegram; Telegram can still observe account and traffic metadata."
            "Third-party notices" -> "Kotlin, AndroidX Compose, Room, WorkManager and Media3: Apache 2.0. Material Icons: Apache 2.0. Gradle: Apache 2.0. JUnit: EPL 1.0. No application source or protected reference assets were copied. Complete inventory is in the project NOTICE and licensing documentation."
            else -> "TG Photos\nVersion 0.1.0 · Phase 1 design preview\n\nPrivate photo, video and file backup is planned. Not affiliated with or endorsed by Telegram. Service restrictions apply; unlimited storage is not promised."
        })
    }, confirmButton = { TextButton(onClick = { detail = null }) { Text("Close") } }) }
}
