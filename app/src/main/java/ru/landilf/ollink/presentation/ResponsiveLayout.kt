package ru.landilf.ollink.presentation

/** Values use the actual content viewport in dp, after system bars, cutouts and IME. */
data class ResponsiveLayout(
    val persistentSidebar: Boolean,
    val sidebarWidth: Float,
    val horizontalPadding: Float,
    val compactHeight: Boolean,
    val composerMaxHeight: Float,
    val composerMaxLines: Int,
    val contentMaxWidth: Float = 760f
) {
    companion object {
        fun calculate(width: Float, height: Float, fontScale: Float): ResponsiveLayout {
            val compact = height < 480f
            val sidebar = width >= 840f || (width >= 600f && width > height)
            val lineHeight = 24f * fontScale.coerceAtLeast(1f)
            val maxHeight = (height * 0.30f).coerceIn(72f, 180f)
            return ResponsiveLayout(sidebar,
                sidebarWidth = (width * 0.32f).coerceIn(220f, 300f),
                horizontalPadding = if (width < 360f || compact) 12f else 20f,
                compactHeight = compact,
                composerMaxHeight = maxHeight,
                composerMaxLines = ((maxHeight - 24f) / lineHeight).toInt().coerceIn(1, 6))
        }
    }
}
