package ru.landilf.ollink.presentation.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

@Composable
fun ChatIconButton(@DrawableRes icon: Int, @StringRes label: Int, modifier: Modifier = Modifier,
                   enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick, modifier, enabled = enabled) {
        Icon(painterResource(icon), stringResource(label))
    }
}
