package app.tgphotos.android

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.tgphotos.core.designsystem.TGPhotosTheme
import app.tgphotos.feature.auth.AuthScreen
import app.tgphotos.feature.photos.PhotosScreen
import app.tgphotos.feature.albums.AlbumsScreen
import app.tgphotos.feature.files.FilesScreen
import app.tgphotos.feature.vault.VaultScreen
import app.tgphotos.feature.settings.SettingsScreen

private enum class MainTab(val label: String, val icon: ImageVector) {
    PHOTOS("Photos", Icons.Outlined.PhotoLibrary),
    ALBUMS("Albums", Icons.Outlined.Collections),
    FILES("Files", Icons.Outlined.Folder),
    VAULT("Private Vault", Icons.Outlined.Lock),
}
private enum class Screen { AUTH, HOME, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = ThemePreferences(applicationContext)
        setContent {
            var mode by remember { mutableStateOf(preferences.mode()) }
            var dynamic by remember { mutableStateOf(preferences.dynamicColors()) }
            val dark = when (mode) {
                app.tgphotos.core.model.ThemeMode.SYSTEM -> isSystemInDarkTheme()
                app.tgphotos.core.model.ThemeMode.DARK -> true
                app.tgphotos.core.model.ThemeMode.LIGHT -> false
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            TGPhotosTheme(mode, dynamic) {
                AppShell(mode, dynamic,
                    onMode = { mode = it; preferences.setMode(it) },
                    onDynamic = { dynamic = it; preferences.setDynamicColors(it) },
                    onOpenTelegram = {
                        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("tg://"))) }
                        catch (_: ActivityNotFoundException) { Toast.makeText(this, "Telegram is not installed", Toast.LENGTH_SHORT).show() }
                    })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(mode: app.tgphotos.core.model.ThemeMode, dynamic: Boolean,
    onMode: (app.tgphotos.core.model.ThemeMode) -> Unit, onDynamic: (Boolean) -> Unit, onOpenTelegram: () -> Unit) {
    var screen by rememberSaveable { mutableStateOf(Screen.AUTH) }
    var tab by rememberSaveable { mutableStateOf(MainTab.PHOTOS) }
    var menu by remember { mutableStateOf(false) }
    BackHandler(enabled = screen != Screen.HOME || tab != MainTab.PHOTOS) {
        if (screen != Screen.HOME) screen = Screen.HOME else tab = MainTab.PHOTOS
    }
    Scaffold(topBar = {
        TopAppBar(title = { Column {
            Text(if (screen == Screen.SETTINGS) "Settings" else "TG Photos", style = MaterialTheme.typography.titleLarge)
            if (screen == Screen.HOME) Text(if (tab == MainTab.PHOTOS) "Photos (Cloud)" else tab.label, style = MaterialTheme.typography.labelMedium)
        } }, navigationIcon = {
            if (screen == Screen.SETTINGS) IconButton(onClick = { screen = Screen.HOME }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        }, actions = {
            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Settings") }, leadingIcon = { Icon(Icons.Outlined.Settings, null) }, onClick = { menu = false; screen = Screen.SETTINGS })
                DropdownMenuItem(text = { Text("Login screens") }, leadingIcon = { Icon(Icons.Outlined.Login, null) }, onClick = { menu = false; screen = Screen.AUTH })
            }
        })
    }, bottomBar = {
        if (screen == Screen.HOME) NavigationBar {
            MainTab.entries.forEach { destination -> NavigationBarItem(selected = tab == destination,
                onClick = { tab = destination }, icon = { Icon(destination.icon, destination.label) }, label = { Text(destination.label) }) }
        }
    }) { padding ->
        val modifier = Modifier.padding(padding).fillMaxSize()
        when (screen) {
            Screen.AUTH -> AuthScreen(onExplore = { screen = Screen.HOME }, onOpenTelegram = onOpenTelegram, modifier = modifier)
            Screen.SETTINGS -> SettingsScreen(mode, dynamic, onMode, onDynamic, onAccount = { screen = Screen.AUTH }, modifier = modifier)
            Screen.HOME -> when (tab) {
                MainTab.PHOTOS -> PhotosScreen(onConnect = { screen = Screen.AUTH }, modifier = modifier)
                MainTab.ALBUMS -> AlbumsScreen(modifier)
                MainTab.FILES -> FilesScreen(modifier)
                MainTab.VAULT -> VaultScreen(modifier)
            }
        }
    }
}
