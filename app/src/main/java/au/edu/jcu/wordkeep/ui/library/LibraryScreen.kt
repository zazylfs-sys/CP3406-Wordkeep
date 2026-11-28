package au.edu.jcu.wordkeep.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.wordkeep.domain.StarterPack
import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.StudyWord
import au.edu.jcu.wordkeep.ui.components.EmptyState
import au.edu.jcu.wordkeep.ui.components.Illustration
import au.edu.jcu.wordkeep.ui.components.Hairline
import au.edu.jcu.wordkeep.ui.components.MasteryTag
import au.edu.jcu.wordkeep.ui.components.PaperCard
import au.edu.jcu.wordkeep.ui.components.SerifHeading
import au.edu.jcu.wordkeep.ui.plural

@Composable
fun LibraryRoute(
    onOpenWord: (Long) -> Unit,
    onLookUp: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onOpenWord = onOpenWord,
        onLookUp = onLookUp,
        onAddStarterPack = viewModel::addStarterPack,
    )
}

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onOpenWord: (Long) -> Unit,
    onLookUp: (String) -> Unit,
    onAddStarterPack: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        item {
            SerifHeading("Your words")
            Text(
                plural(state.totalWords, "word") + " saved on this device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(top = 12.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search or look up a word") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onSearch = { state.lookUpSuggestion?.let(onLookUp) }),
            )
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LibraryFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.label) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        state.lookUpSuggestion?.let { suggestion ->
            item(key = "lookup") {
                LookUpRow(suggestion) { onLookUp(suggestion) }
            }
        }
        if (!state.loading && !state.starterPackAdded && state.query.isBlank()) {
            item(key = "starter") { StarterPackCard(onAddStarterPack) }
        }
        if (!state.loading && state.words.isEmpty() && state.lookUpSuggestion == null) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.LibraryAdd,
                    art = if (state.totalWords == 0) Illustration.EmptyLibrary else null,
                    title = if (state.totalWords == 0) "No words yet" else "No matches",
                    body = if (state.totalWords == 0) "Search the dictionary above, or add the starter list."
                    else "Try another filter or search term.",
                )
            }
        }
        items(state.words, key = { it.word.id }) { word ->
            WordRow(word) { onOpenWord(word.word.id) }
            Hairline()
        }
    }
}

@Composable
private fun LookUpRow(word: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.TravelExplore, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Look up “$word”", style = MaterialTheme.typography.titleMedium)
                Text("Sends only this word to dictionaryapi.dev", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StarterPackCard(onAdd: () -> Unit) {
    PaperCard(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(StarterPack.NAME, style = MaterialTheme.typography.titleLarge)
        Text(
            "The ${StarterPack.words.size} most common word families in university texts (Coxhead, 2000). " +
                "Definitions are downloaded once and then work offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 8.dp))
        androidx.compose.material3.FilledTonalButton(onClick = onAdd) { Text("Add starter list") }
    }
}

@Composable
private fun WordRow(word: StudyWord, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClickLabel = "Open ${word.word.text}", onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(word.word.text, style = MaterialTheme.typography.titleLarge)
                word.word.primarySense?.partOfSpeech?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.width(8.dp))
                    au.edu.jcu.wordkeep.ui.components.PartOfSpeech(it)
                }
            }
            Text(
                LibraryViewModel.statusLine(word),
                style = MaterialTheme.typography.bodySmall,
                color = if (word.word.fetchState == FetchState.READY) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        if (word.word.fetchState == FetchState.READY) MasteryTag(word.review.mastery)
    }
}
