package app.tgphotos.feature.vault

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.tgphotos.core.designsystem.*

private fun Context.activity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.activity(); else -> null }

/** Phase 1 remains locked; no password or biometric is treated as a real unlock. */
@Composable
fun VaultScreen(modifier: Modifier = Modifier) {
    val activity = LocalContext.current.activity()
    DisposableEffect(activity) {
        val window = activity?.window
        val alreadySecure = ((window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!alreadySecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PreviewNotice()
        Spacer(Modifier.height(20.dp))
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(Icons.Outlined.Lock, "Vault locked", Modifier.padding(28.dp).size(56.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text("Only for you.", style = MaterialTheme.typography.headlineLarge)
        Text("Private Vault", style = MaterialTheme.typography.titleMedium)
        Text("Separate authentication and encrypted storage are not integrated yet. The vault stays locked.", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(value = "", onValueChange = {}, enabled = false, label = { Text("Vault password") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Unlock vault") }
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Fingerprint, null); Spacer(Modifier.width(8.dp)); Text("Use biometrics") }
        FeatureRow("Screen capture protection", "FLAG_SECURE is applied on this screen; device verification is pending.", Icons.Outlined.Security)
        FeatureRow("Independent key hierarchy", "Planned · vault keys and previews stay separate from the gallery.", Icons.Outlined.Key)
        FeatureRow("Auto-lock", "Planned · no private content is accessible in this phase.", Icons.Outlined.Timer)
    }
}
