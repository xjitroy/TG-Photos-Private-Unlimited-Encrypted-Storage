package app.tgphotos.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.tgphotos.core.designsystem.*
import app.tgphotos.core.telegram.Countries
import java.util.Locale

private enum class AuthPreviewStep(val title: String) { PHONE("Phone"), CODE("OTP"), TWO_FACTOR("Telegram 2FA"), PRIVATE_PASSWORD("Encryption") }

/** Explicit screen catalogue. Navigation between steps never changes authorization state. */
@Composable
fun AuthScreen(onExplore: () -> Unit, onOpenTelegram: () -> Unit, modifier: Modifier = Modifier) {
    var step by rememberSaveable { mutableStateOf(AuthPreviewStep.PHONE) }
    var country by remember { mutableStateOf(Countries.suggest(Locale.getDefault().country)) }
    var picker by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PreviewNotice()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                Icon(if (step == AuthPreviewStep.PRIVATE_PASSWORD) Icons.Outlined.Key else Icons.Outlined.PhotoLibrary,
                    null, Modifier.padding(24.dp).size(44.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text("TG Photos", style = MaterialTheme.typography.headlineLarge)
        Text(when (step) {
            AuthPreviewStep.PHONE -> "A private home for\nyour memories."
            AuthPreviewStep.CODE -> "Your Telegram code"
            AuthPreviewStep.TWO_FACTOR -> "Telegram two-step\nverification"
            AuthPreviewStep.PRIVATE_PASSWORD -> "Your encryption\npassword"
        }, style = MaterialTheme.typography.headlineMedium)
        Text(when (step) {
            AuthPreviewStep.PHONE -> "Explore the login design. Live sign-in is unavailable until official TDLib is integrated."
            AuthPreviewStep.CODE -> "The real code delivery method and length will come from TDLib. No OTP has been requested."
            AuthPreviewStep.TWO_FACTOR -> "This is your Telegram account password, requested only if Telegram requires it."
            AuthPreviewStep.PRIVATE_PASSWORD -> "A separate private password will unlock your encryption keys. It is different from Telegram 2FA."
        }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (step) {
            AuthPreviewStep.PHONE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { picker = true }, modifier = Modifier.heightIn(min = 56.dp)) { Text(country.callingCode); Icon(Icons.Outlined.ExpandMore, "Select country") }
                    OutlinedTextField(value = "", onValueChange = {}, enabled = false, label = { Text("Phone number") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
                Text("Suggested country: ${country.name} · editable", style = MaterialTheme.typography.bodySmall)
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Continue with Telegram") }
            }
            AuthPreviewStep.CODE -> {
                OutlinedTextField(value = "", onValueChange = {}, enabled = false, label = { Text("Verification code") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                FilledTonalButton(onClick = onOpenTelegram, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Open Telegram") }
                Text("Opens Telegram without sending your phone number or reading messages.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Verify code") }
            }
            AuthPreviewStep.TWO_FACTOR -> {
                PasswordPreviewField("Telegram 2FA password")
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Verify Telegram password") }
            }
            AuthPreviewStep.PRIVATE_PASSWORD -> {
                PasswordPreviewField("Private encryption password")
                PasswordPreviewField("Confirm encryption password")
                StatusCard("Recovery is part of setup", "Offline recovery export and authenticated key wrapping must be ready before creating a real key.", Icons.Outlined.Key)
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Create private keys") }
            }
        }
        HorizontalDivider()
        Text("Preview a login screen", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AuthPreviewStep.entries.forEach { value -> FilterChip(selected = value == step, onClick = { step = value }, label = { Text(value.title) }) }
        }
        OutlinedButton(onClick = onExplore, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Explore the app preview"); Spacer(Modifier.width(8.dp)); Icon(Icons.Outlined.ArrowForward, null) }
        Text("Independent application. Not affiliated with or endorsed by Telegram.", style = MaterialTheme.typography.bodySmall)
    }
    if (picker) AlertDialog(onDismissRequest = { picker = false }, title = { Text("Country code") }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            Text("Starter country list for this design preview.", style = MaterialTheme.typography.bodySmall)
            Countries.choices.forEach { value -> TextButton(onClick = { country = value; picker = false }, modifier = Modifier.fillMaxWidth()) { Text("${value.name}  ${value.callingCode}") } }
        }
    }, confirmButton = { TextButton(onClick = { picker = false }) { Text("Close") } })
}

@Composable
private fun PasswordPreviewField(label: String) {
    OutlinedTextField(value = "", onValueChange = {}, enabled = false, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
}
