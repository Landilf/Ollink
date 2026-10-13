package ru.landilf.ollink.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.landilf.ollink.R
import ru.landilf.ollink.presentation.ResponsiveLayout

@Composable
fun ChatWelcome(connected: Boolean, layout: ResponsiveLayout, openSettings: () -> Unit,
                onPrompt: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        val suggestions = listOf(R.string.suggest_explain to R.string.suggest_explain_prompt,
            R.string.suggest_code to R.string.suggest_code_prompt,
            R.string.suggest_idea to R.string.suggest_idea_prompt,
            R.string.suggest_plan to R.string.suggest_plan_prompt)
        LazyColumn(Modifier.fillMaxSize().testTag("welcome_list"), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (layout.compactHeight) 8.dp else 12.dp, Alignment.CenterVertically),
            contentPadding = PaddingValues(horizontal = layout.horizontalPadding.dp, vertical = 16.dp)) {
            item { Text(stringResource(R.string.welcome), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center) }
            item { Text(stringResource(if (connected) R.string.welcome_detail else R.string.setup_detail),
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
            if (!connected) item { Button(openSettings) { Text(stringResource(R.string.setup)) } }
            suggestions.chunked(2).forEachIndexed { row, pair -> item {
                Row(Modifier.fillMaxWidth().testTag("prompt_row_$row"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (label, prompt) -> Suggestion(label, prompt, onPrompt, Modifier.weight(1f)) }
                }
            } }
        }
    }
}

@Composable
private fun Suggestion(label: Int, prompt: Int, onPrompt: (String) -> Unit, modifier: Modifier) {
    val text = stringResource(prompt)
    OutlinedButton(onClick = { onPrompt(text) }, modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Text(stringResource(label))
    }
}
