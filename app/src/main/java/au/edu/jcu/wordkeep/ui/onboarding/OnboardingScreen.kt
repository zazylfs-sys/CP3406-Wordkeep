package au.edu.jcu.wordkeep.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.edu.jcu.wordkeep.data.repository.WordRepository
import au.edu.jcu.wordkeep.data.settings.SettingsRepository
import au.edu.jcu.wordkeep.domain.StarterPack
import au.edu.jcu.wordkeep.ui.components.PaperCard
import au.edu.jcu.wordkeep.ui.components.SwitchRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val wordRepository: WordRepository,
) : ViewModel() {

    /** Saves the learner's choices. The starter list is only added (and downloaded) if they opt in. */
    fun finish(addStarterPack: Boolean, offlineMode: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            if (addStarterPack) wordRepository.addStarterPack()
            settingsRepository.update { it.copy(onboardingComplete = true, offlineMode = offlineMode) }
            onDone()
        }
    }
}

@Composable
fun OnboardingRoute(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    OnboardingScreen(onStart = { starter, offline -> viewModel.finish(starter, offline, onFinished) })
}

/**
 * First run: explain what the app does with data *before* anything is sent, and let the learner
 * choose. Nothing is requested from the network until "Start learning" is pressed.
 */
@Composable
fun OnboardingScreen(onStart: (addStarterPack: Boolean, offlineMode: Boolean) -> Unit) {
    var addStarter by rememberSaveable { mutableStateOf(true) }
    var offline by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "Wordkeep",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Build the academic vocabulary your lectures and readings use, with short, spaced practice.",
            style = MaterialTheme.typography.titleMedium,
        )
        Text("Before you start, here is how Wordkeep handles your data.", style = MaterialTheme.typography.bodyMedium)

        Promise(Icons.Rounded.PhoneAndroid, "Your progress stays on this phone", "No account, no cloud backup, no analytics, no location.")
        Promise(
            Icons.Rounded.CloudQueue,
            "Only the word you look up is sent",
            "Definitions come from dictionaryapi.dev and quiz options from api.datamuse.com. You can switch all network use off.",
        )
        Promise(
            Icons.Rounded.Accessibility,
            "Built to be used your way",
            "Works with TalkBack and large text. Contrast, motion and sound can be changed in Settings.",
        )

        PaperCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
            SwitchRow(
                title = "Add the starter list",
                description = "${StarterPack.words.size} high-frequency academic words (${StarterPack.NAME}). Their definitions are downloaded once.",
                checked = addStarter,
                onCheckedChange = { addStarter = it },
            )
            SwitchRow(
                title = "Start in offline mode",
                description = "Nothing is sent. You can add definitions later by turning it off.",
                checked = offline,
                onCheckedChange = { offline = it },
            )
        }

        Button(
            onClick = { onStart(addStarter, offline) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) { Text("Start learning") }
        Text(
            "You can export or delete everything at any time in Settings › Your data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Promise(icon: ImageVector, title: String, body: String) {
    Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
