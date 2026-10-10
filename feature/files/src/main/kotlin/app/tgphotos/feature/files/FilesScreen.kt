package app.tgphotos.feature.files

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tgphotos.core.designsystem.*

private data class SampleFile(val name: String, val type: String, val description: String)
private val samples = listOf(
    SampleFile("Camera", "Folders", "Sample folder"), SampleFile("Downloads", "Folders", "Sample folder"),
    SampleFile("Quiet peaks.jpg", "Images", "Example image · no device file"),
    SampleFile("Coastline.mp4", "Videos", "Example video · no device file"),
    SampleFile("Notes.pdf", "Documents", "Example document · no device file"),
    SampleFile("Field recording.flac", "Audio", "Example audio · no device file"),
    SampleFile("Travel.zip", "Archives", "Example archive · no device file"),
)

@Composable
fun FilesScreen(modifier: Modifier = Modifier) {
    var location by rememberSaveable { mutableStateOf("Local") }
    var category by rememberSaveable { mutableStateOf("All") }
    var query by rememberSaveable { mutableStateOf("") }
    var sorted by rememberSaveable { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    val files = samples.filter { (category == "All" || it.type == category) && it.name.contains(query, ignoreCase = true) }.let { if (sorted) it.sortedBy { f -> f.name } else it }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Everything,\nin its place.", "Files · local & encrypted cloud") }
        item { PreviewNotice() }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Local", "Cloud").forEach { value ->
            FilterChip(selected = location == value, onClick = { location = value; selection = emptySet() }, label = { Text(value) })
        } } }
        item { OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Search sample files") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }) }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Folders", "Images", "Videos", "Audio", "Documents", "Archives").forEach { value ->
                FilterChip(selected = category == value, onClick = { category = value }, label = { Text(value) })
            }
        } }
        if (location == "Cloud") item { StatusCard("Your cloud is not connected", "No cloud filenames, files or credentials are loaded.") }
        else {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sample files", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { sorted = !sorted }) { Text(if (sorted) "A–Z" else "Default order") }
            } }
            if (selection.isNotEmpty()) item { StatusCard("${selection.size} samples selected", "Copy, move, rename, backup, restore and delete will be enabled after scoped-storage integration.", Icons.Outlined.CheckCircle) }
            if (files.isEmpty()) item { Text("No sample files match your search.", Modifier.padding(20.dp)) }
            items(files, key = { it.name }) { file ->
                FeatureRow(file.name, file.description, if (file.type == "Folders") Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                    onClick = { selection = if (file.name in selection) selection - file.name else selection + file.name },
                    trailing = { Checkbox(checked = file.name in selection, onCheckedChange = { checked -> selection = if (checked) selection + file.name else selection - file.name }) })
            }
        }
    }
}
