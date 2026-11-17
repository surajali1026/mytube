package com.mytube.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun GoogleLoginCard() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("mytube_preferences", 0) }
    var webClientId by remember { mutableStateOf(preferences.getString("google_web_client_id", "") ?: "") }
    var accountEmail by remember { mutableStateOf(preferences.getString("google_account_email", "") ?: "") }
    var accountName by remember { mutableStateOf(preferences.getString("google_account_name", "") ?: "") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Google account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (accountEmail.isNotBlank()) {
                Text("Signed in as $accountEmail", style = MaterialTheme.typography.bodyMedium)
                if (accountName.isNotBlank()) Text(accountName, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = {
                        accountEmail = ""
                        accountName = ""
                        preferences.edit().remove("google_account_email").remove("google_account_name").apply()
                        message = "Signed out of MyTube."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sign out") }
            } else {
                Text("Enter the Web application OAuth client ID from the same Google Cloud project. Do not use the Android OAuth client ID here.")
                OutlinedTextField(
                    value = webClientId,
                    onValueChange = {
                        webClientId = it
                        preferences.edit().putString("google_web_client_id", it.trim()).apply()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Web OAuth Client ID") },
                    singleLine = true
                )
                Button(
                    enabled = webClientId.endsWith(".apps.googleusercontent.com") && !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            message = "Opening Google sign-in…"
                            try {
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setServerClientId(webClientId.trim())
                                    .setFilterByAuthorizedAccounts(false)
                                    .setAutoSelectEnabled(false)
                                    .build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()
                                val credentialManager = CredentialManager.create(context)
                                val result = credentialManager.getCredential(context, request)
                                val credential = result.credential
                                if (credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                ) {
                                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    accountEmail = googleCredential.id
                                    accountName = googleCredential.displayName.orEmpty()
                                    preferences.edit()
                                        .putString("google_account_email", accountEmail)
                                        .putString("google_account_name", accountName)
                                        .apply()
                                    message = "Google account connected to MyTube."
                                } else {
                                    message = "Google returned an unsupported credential type."
                                }
                            } catch (e: GetCredentialException) {
                                message = e.localizedMessage ?: "Google sign-in was cancelled or failed."
                            } catch (e: Exception) {
                                message = e.localizedMessage ?: "Google sign-in failed."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Continue with Google")
                }
            }
            if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Setup required: create an Android OAuth client for package com.mytube.app and its signing SHA-1, plus a Web application OAuth client ID in Google Cloud. This signs into the app only; YouTube subscriptions, comments or other private account data need separate YouTube OAuth scopes and consent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
