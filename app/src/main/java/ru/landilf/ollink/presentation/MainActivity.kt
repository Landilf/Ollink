package ru.landilf.ollink.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.landilf.ollink.OllinkApplication
import ru.landilf.ollink.R
import ru.landilf.ollink.presentation.theme.OllinkTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ChatViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as OllinkApplication).container
        viewModel = ViewModelProvider(this, container.viewModelFactory)[ChatViewModel::class.java]
        setContent {
            OllinkTheme {
                val state = viewModel.uiState.collectAsStateWithLifecycle()
                ChatScreen(state.value, viewModel::onAction)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ChatEvent.CopyToClipboard -> {
                            getSystemService(ClipboardManager::class.java).setPrimaryClip(
                                ClipData.newPlainText(getString(R.string.app_name), event.text))
                            Toast.makeText(this@MainActivity, R.string.copied, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
    override fun onStop() {
        viewModel.onAction(ChatAction.PersistHistory)
        super.onStop()
    }
}
