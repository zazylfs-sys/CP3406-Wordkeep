package au.edu.jcu.wordkeep.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.wordkeep.data.settings.TextScale
import au.edu.jcu.wordkeep.data.settings.ThemeMode
import au.edu.jcu.wordkeep.data.settings.UserSettings
import au.edu.jcu.wordkeep.domain.model.Accent
import au.edu.jcu.wordkeep.ui.components.PaperCard
import au.edu.jcu.wordkeep.ui.components.SectionLabel
import au.edu.jcu.wordkeep.ui.components.SegmentedChoice
import au.edu.jcu.wordkeep.ui.components.SerifHeading
import au.edu.jcu.wordkeep.ui.components.SwitchRow

@Composable
fun SettingsRoute(onOpenYourData: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScreen(settings, viewModel::update, onOpenYourData)
}

/** Every change applies immediately and is stored with DataStore. */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onUpdate: ((UserSettings) -> UserSettings) -> Unit,
    onOpenYourData: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SerifHeading("Settings")

        SettingsGroup("Study") {
            SegmentedChoice(
                title = "Session length",
                description = "Cards or questions per session. Sessions always end.",
                options = UserSettings.SESSION_LENGTH_CHOICES,
                selected = settings.sessionLength,
                onSelect = { value -> onUpdate { it.copy(sessionLength = value) } },
                label = { it.toString() },
            )
            SegmentedChoice(
                title = "New words per day",
                description = "Fewer new words means fewer reviews piling up later.",
                options = UserSettings.DAILY_NEW_WORD_CHOICES,
                selected = settings.dailyNewWords,
                onSelect = { value -> onUpdate { it.copy(dailyNewWords = value) } },
                label = { it.toString() },
            )
            SwitchRow(
                title = "Meaning hints in Listen & spell",
                description = "Offer the definition as an optional hint.",
                checked = settings.spellingHints,
                onCheckedChange = { value -> onUpdate { it.copy(spellingHints = value) } },
            )
        }

        SettingsGroup("Sound") {
            SegmentedChoice(
                title = "Pronunciation accent",
                description = "Recordings are used when available, otherwise the device voice.",
                options = Accent.entries,
                selected = settings.accent,
                onSelect = { value -> onUpdate { it.copy(accent = value) } },
                label = { it.label },
            )
            SwitchRow(
                title = "Play pronunciation automatically",
                description = "On flashcards. Listen & spell always plays the word.",
                checked = settings.autoPlayPronunciation,
                onCheckedChange = { value -> onUpdate { it.copy(autoPlayPronunciation = value) } },
            )
            SwitchRow(
                title = "Answer sounds",
                description = "A short tone after each answer. Results are always shown as text too.",
                checked = settings.soundEffects,
                onCheckedChange = { value -> onUpdate { it.copy(soundEffects = value) } },
            )
        }

        SettingsGroup("Display and accessibility") {
            SegmentedChoice(
                title = "Theme",
                options = ThemeMode.entries,
                selected = settings.themeMode,
                onSelect = { value -> onUpdate { it.copy(themeMode = value) } },
                label = { it.label },
            )
            SegmentedChoice(
                title = "Text size",
                description = "Added on top of your system font size.",
                options = TextScale.entries,
                selected = settings.textScale,
                onSelect = { value -> onUpdate { it.copy(textScale = value) } },
                label = { it.label },
            )
            SwitchRow(
                title = "High contrast",
                description = "Pure black and white surfaces with stronger outlines.",
                checked = settings.highContrast,
                onCheckedChange = { value -> onUpdate { it.copy(highContrast = value) } },
            )
            SwitchRow(
                title = "Reduce motion",
                description = "Replaces card flips and slides with simple fades. Also follows the system setting.",
                checked = settings.reduceMotion,
                onCheckedChange = { value -> onUpdate { it.copy(reduceMotion = value) } },
            )
        }

        SettingsGroup("Privacy") {
            SwitchRow(
                title = "Offline mode",
                description = "Stops all network use. Saved words, reviews and the device voice keep working.",
                checked = settings.offlineMode,
                onCheckedChange = { value -> onUpdate { it.copy(offlineMode = value) } },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClickLabel = "Open", onClick = onOpenYourData)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your data", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "What is stored, what is sent, export and delete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    PaperCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        SectionLabel(title)
        content()
    }
}
