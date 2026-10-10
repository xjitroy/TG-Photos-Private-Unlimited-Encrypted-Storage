package app.tgphotos.feature.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.tgphotos.core.designsystem.*
import app.tgphotos.core.model.MediaKind
import app.tgphotos.core.testing.PreviewCatalog

@Composable
fun PhotosScreen(onConnect: () -> Unit, modifier: Modifier = Modifier) {
    var filter by rememberSaveable { mutableStateOf("All") }
    var viewingId by rememberSaveable { mutableStateOf<Int?>(null) }
    val media = PreviewCatalog.media.filter {
        when (filter) { "Photos" -> it.kind == MediaKind.PHOTO; "Videos" -> it.kind == MediaKind.VIDEO; "Favorites" -> it.favorite; else -> true }
    }
    LazyVerticalGrid(columns = GridCells.Adaptive(104.dp), modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                SectionTitle("Your moments,\nyour space.", "Photos (Cloud)")
                PreviewNotice()
                StatusCard("Connect your private cloud", "Telegram connection and encrypted backup will be available after integration.")
                FilledTonalButton(onClick = onConnect) { Icon(Icons.Outlined.Login, null); Spacer(Modifier.width(8.dp)); Text("Explore login screens") }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Photos", "Videos", "Favorites").forEach { value -> FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) }) }
                }
                Text("Sample collection", style = MaterialTheme.typography.titleMedium)
                Text("Original illustrations · swipe a preview to explore", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
            }
        }
        items(media, key = { it.id }) { item ->
            Card(onClick = { viewingId = item.id }) {
                Box {
                    LandscapeArt(item.id, Modifier.fillMaxWidth().aspectRatio(1f))
                    if (item.kind == MediaKind.VIDEO) Icon(Icons.Outlined.PlayCircle, "Video preview: ${item.title}", Modifier.align(Alignment.BottomEnd).padding(8.dp), tint = MaterialTheme.colorScheme.inverseOnSurface)
                    else if (item.favorite) Icon(Icons.Outlined.FavoriteBorder, "Favorite: ${item.title}", Modifier.align(Alignment.BottomEnd).padding(8.dp), tint = MaterialTheme.colorScheme.inverseOnSurface)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(20.dp)) }
    }
    viewingId?.let { id -> PreviewViewer(id, onClose = { viewingId = null }) }
}

@Composable
private fun PreviewViewer(initialId: Int, onClose: () -> Unit) {
    val items = PreviewCatalog.media
    val pager = rememberPagerState(initialPage = items.indexOfFirst { it.id == initialId }.coerceAtLeast(0), pageCount = { items.size })
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close viewer") }
                    Text(items[pager.currentPage].title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${pager.currentPage + 1} / ${items.size}", style = MaterialTheme.typography.labelLarge)
                }
                HorizontalPager(state = pager, modifier = Modifier.weight(1f), key = { items[it].id }) { index ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LandscapeArt(items[index].id, Modifier.fillMaxWidth().aspectRatio(.8f)) }
                }
                Text(if (items[pager.currentPage].kind == MediaKind.VIDEO) "Illustration only · video playback not integrated" else "Design preview · swipe left or right", Modifier.padding(24.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
