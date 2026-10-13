package ru.landilf.ollink.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.landilf.ollink.R
import ru.landilf.ollink.domain.model.ServerAddress

@Composable
fun ConnectionDialog(server: String, dismiss: () -> Unit, connect: (String) -> Unit) {
    var address by rememberSaveable { mutableStateOf(server) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.settings)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.server_description))
            OutlinedTextField(address, { address = it; error = null }, singleLine = true,
                label = { Text(stringResource(R.string.server_address)) },
                placeholder = { Text(stringResource(R.string.server_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), isError = error != null,
                supportingText = { error?.let { Text(it) } })
        } }, confirmButton = { TextButton(onClick = {
            try { connect(ServerAddress.normalize(address)) }
            catch (e: IllegalArgumentException) { error = e.message }
        }) { Text(stringResource(R.string.connect)) } },
        dismissButton = { TextButton(dismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun ModelDialog(models: List<String>, selected: String, dismiss: () -> Unit, choose: (String) -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.choose_model)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            models.forEach { model ->
                TextButton(onClick = { choose(model) }, Modifier.fillMaxWidth()) {
                    RadioButton(model == selected, onClick = null)
                    Text(model, Modifier.weight(1f).padding(start = 8.dp))
                }
            }
        } }, confirmButton = { TextButton(dismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun RenameChatDialog(title: String, dismiss: () -> Unit, rename: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(title) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.rename)) },
        text = { OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.chat_title)) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { rename(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(dismiss) { Text(stringResource(R.string.cancel)) } })
}
