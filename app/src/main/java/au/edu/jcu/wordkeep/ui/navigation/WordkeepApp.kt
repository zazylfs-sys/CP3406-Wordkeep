package au.edu.jcu.wordkeep.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import au.edu.jcu.wordkeep.ui.home.HomeRoute as HomeDestination
import au.edu.jcu.wordkeep.ui.library.LibraryRoute as LibraryDestination
import au.edu.jcu.wordkeep.ui.onboarding.OnboardingRoute as OnboardingDestination
import au.edu.jcu.wordkeep.ui.session.SessionRoute as SessionDestination
import au.edu.jcu.wordkeep.ui.settings.SettingsRoute as SettingsDestination
import au.edu.jcu.wordkeep.ui.settings.YourDataRoute as YourDataDestination
import au.edu.jcu.wordkeep.ui.stats.StatsRoute as StatsDestination
import au.edu.jcu.wordkeep.ui.summary.SummaryRoute as SummaryDestination
import au.edu.jcu.wordkeep.ui.word.LookupRoute as LookupDestination
import au.edu.jcu.wordkeep.ui.word.WordDetailRoute as WordDetailDestination

/** Screens that hide the navigation bar so the learner can focus on one task. */
private val immersiveRoutes = listOf(OnboardingRoute::class, SessionRoute::class, SummaryRoute::class)

/**
 * App shell: adaptive navigation (bottom bar on phones, rail on tablets and landscape) around a
 * type-safe NavHost with nested graphs for Words and Settings.
 */
@Composable
fun WordkeepApp(
    startWithOnboarding: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val immersive = destination == null || immersiveRoutes.any { destination.hasRoute(it) }
    val start: Any = remember { if (startWithOnboarding) OnboardingRoute else HomeRoute }

    NavigationSuiteScaffold(
        layoutType = when {
            immersive -> NavigationSuiteType.None
            wide -> NavigationSuiteType.NavigationRail
            else -> NavigationSuiteType.NavigationBar
        },
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { item ->
                val selected = destination.isOnTab(item)
                item(
                    selected = selected,
                    onClick = { navController.navigateToTab(item) },
                    icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = null) },
                    label = { Text(item.label) },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        NavHost(navController = navController, startDestination = start, modifier = Modifier.fillMaxSize()) {
            composable<OnboardingRoute> {
                OnboardingDestination(onFinished = {
                    navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                })
            }
            composable<HomeRoute> {
                TabPage {
                    HomeDestination(
                        onStartSession = { navController.navigate(SessionRoute(it)) },
                        onLookUp = { navController.navigate(LookupRoute(it)) },
                        onOpenLibrary = { navController.navigateToTab(TopLevelDestination.WORDS) },
                        wideLayout = wide,
                    )
                }
            }
            navigation<WordsGraph>(startDestination = LibraryRoute()) {
                composable<LibraryRoute> {
                    TabPage {
                        LibraryDestination(
                            onOpenWord = { navController.navigate(WordDetailRoute(it)) },
                            onLookUp = { navController.navigate(LookupRoute(it)) },
                        )
                    }
                }
                composable<WordDetailRoute> {
                    WordDetailDestination(onBack = { navController.navigateUp() })
                }
                composable<LookupRoute>(
                    deepLinks = listOf(navDeepLink<LookupRoute>(basePath = LOOKUP_DEEP_LINK)),
                ) {
                    LookupDestination(
                        // navigateUp, not popBackStack: after an external deep link this may be the only
                        // screen, and Up then opens the app's own hierarchy instead of a blank screen.
                        onBack = { navController.navigateUp() },
                        // Replace the look-up with the saved word, so Back does not return to a stale result.
                        onOpenWord = { id ->
                            navController.navigate(WordDetailRoute(id)) { popUpTo<LookupRoute> { inclusive = true } }
                        },
                        onOpenSettings = { navController.navigateToTab(TopLevelDestination.SETTINGS) },
                    )
                }
            }
            composable<StatsRoute> {
                TabPage {
                    StatsDestination(onOpenWord = { navController.navigate(WordDetailRoute(it)) }, wideLayout = wide)
                }
            }
            navigation<SettingsGraph>(startDestination = SettingsRoute) {
                composable<SettingsRoute> {
                    TabPage { SettingsDestination(onOpenYourData = { navController.navigate(YourDataRoute) }) }
                }
                composable<YourDataRoute> {
                    YourDataDestination(
                        onBack = { navController.navigateUp() },
                        // After erasing everything, start again from the first-run screen. Saved tab
                        // stacks are cleared too, so no screen showing deleted data can be restored.
                        onDeleted = {
                            navController.clearBackStack<WordsGraph>()
                            navController.clearBackStack<StatsRoute>()
                            navController.clearBackStack<SettingsGraph>()
                            navController.navigate(OnboardingRoute) { popUpTo(navController.graph.id) { inclusive = true } }
                        },
                    )
                }
            }
            composable<SessionRoute> { entry ->
                SessionDestination(
                    mode = entry.toRoute<SessionRoute>().mode,
                    // The finished session is removed from the back stack: Back from the summary goes home.
                    onFinished = { mode, summary ->
                        navController.navigate(
                            SummaryRoute(mode, summary.answered, summary.correct, summary.seconds, summary.missedWordIds.toLongArray())
                        ) { popUpTo<SessionRoute> { inclusive = true } }
                    },
                    onExit = { navController.popBackStack() },
                    onOpenLibrary = {
                        navController.popBackStack()
                        navController.navigateToTab(TopLevelDestination.WORDS)
                    },
                )
            }
            composable<SummaryRoute> {
                SummaryDestination(
                    onDone = { navController.popBackStack() },
                    onOpenProgress = {
                        navController.popBackStack()
                        navController.navigateToTab(TopLevelDestination.PROGRESS)
                    },
                    onOpenWord = { navController.navigate(WordDetailRoute(it)) },
                )
            }
        }
    }
}

/** Tab roots draw their own content under the status bar, so they pad for it. */
@Composable
private fun TabPage(content: @Composable () -> Unit) {
    Box(Modifier.statusBarsPadding()) { content() }
}

private fun NavDestination?.isOnTab(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true

/**
 * Standard tab navigation: one copy of each tab on the stack, each tab keeps its own back stack
 * (saveState / restoreState), and Today is always the root so Back eventually exits from there.
 *
 * Today is special: it is the root, so selecting it simply pops back to it. Navigating to it with
 * restoreState would bring back whatever screen was just popped from above it.
 */
fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    if (tab == TopLevelDestination.TODAY) {
        val homeOnStack = runCatching { getBackStackEntry<HomeRoute>() }.isSuccess
        if (homeOnStack) {
            popBackStack<HomeRoute>(inclusive = false)
        } else {
            // e.g. opened from a deep link: build the normal stack with Today at the root.
            navigate(HomeRoute) { popUpTo(graph.id) { inclusive = true } }
        }
        return
    }
    navigate(tab.route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
