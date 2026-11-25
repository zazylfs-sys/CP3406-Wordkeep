package au.edu.jcu.wordkeep.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

/** The four places in the navigation bar (or rail on wide screens). */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    TODAY(HomeRoute, HomeRoute::class, "Today", Icons.Rounded.WbSunny, Icons.Outlined.WbSunny),
    WORDS(WordsGraph, WordsGraph::class, "Words", Icons.Rounded.MenuBook, Icons.Outlined.MenuBook),
    PROGRESS(StatsRoute, StatsRoute::class, "Progress", Icons.Rounded.Insights, Icons.Outlined.Insights),
    SETTINGS(SettingsGraph, SettingsGraph::class, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings),
}
