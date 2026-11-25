package au.edu.jcu.wordkeep.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import au.edu.jcu.wordkeep.data.audio.PlaybackState
import au.edu.jcu.wordkeep.domain.model.Grade
import au.edu.jcu.wordkeep.domain.quiz.DefinitionMasker
import au.edu.jcu.wordkeep.domain.spelling.LetterMark
import au.edu.jcu.wordkeep.domain.spelling.SpellingVerdict
import au.edu.jcu.wordkeep.ui.components.PaperCard
import au.edu.jcu.wordkeep.ui.components.PartOfSpeech
import au.edu.jcu.wordkeep.ui.components.PhoneticText
import au.edu.jcu.wordkeep.ui.components.PronounceButton
import au.edu.jcu.wordkeep.ui.components.SectionLabel
import au.edu.jcu.wordkeep.ui.components.SenseList
import au.edu.jcu.wordkeep.ui.theme.WordkeepTheme

/** Scrollable body with the action area pinned below, so large text never hides the buttons. */
@Composable
private fun ItemLayout(
    body: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = body,
        )
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = actions,
        )
    }
}

// ---------------------------------------------------------------- Flashcards

@Composable
fun FlashcardContent(item: SessionItem.Flashcard, playback: PlaybackState, actions: SessionActions) {
    val word = item.word.word
    val reduceMotion = WordkeepTheme.accessibility.reduceMotion
    ItemLayout(
        body = {
            if (item.isRepeat) {
                Text(
                    "Second look: you marked this “Again” earlier.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            FlipCard(
                revealed = item.revealed,
                reduceMotion = reduceMotion,
                onReveal = actions.onReveal,
                front = {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(word.text, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                        PhoneticText(word.phonetic)
                        PronounceButton(word.text, playback, actions.onPronounce)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Say the meaning to yourself, then tap to check",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                back = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(word.text, style = MaterialTheme.typography.headlineMedium)
                                PhoneticText(word.phonetic)
                            }
                            PronounceButton(word.text, playback, actions.onPronounce)
                        }
                        SenseList(word.senses.take(2), showSynonyms = false)
                        word.note?.let { note ->
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Row(Modifier.padding(12.dp)) {
                                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Your memory hook: $note", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                },
            )
        },
        actions = {
            if (!item.revealed) {
                Button(onClick = actions.onReveal, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("Show meaning")
                }
            } else {
                Text(
                    if (item.isRepeat) "Practice only: this word’s next review is already scheduled."
                    else "How well did you know it?",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                GradeButtons(item.intervals, actions.onGrade)
            }
        },
    )
}

/**
 * Flips with a 3D turn. Only the visible face is composed, so TalkBack never reads both sides.
 * With reduced motion the faces cross-fade instead.
 */
@Composable
private fun FlipCard(
    revealed: Boolean,
    reduceMotion: Boolean,
    onReveal: () -> Unit,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
) {
    val cardModifier = Modifier
        .fillMaxWidth()
        .semantics { stateDescription = if (revealed) "Showing meaning" else "Showing word" }
        .then(if (!revealed) Modifier.clickable(onClickLabel = "Show meaning", role = Role.Button, onClick = onReveal) else Modifier)

    if (reduceMotion) {
        PaperCard(cardModifier, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Crossfade(revealed, label = "faces") { showBack -> if (showBack) back() else front() }
        }
        return
    }
    val rotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(320),
        label = "flip",
    )
    val density = LocalDensity.current.density
    PaperCard(
        cardModifier.graphicsLayer {
            rotationY = rotation
            cameraDistance = 12f * density
        },
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        if (rotation <= 90f) {
            front()
        } else {
            // Counter-rotate the back face so its text is not mirrored.
            Box(Modifier.graphicsLayer { rotationY = 180f }) { back() }
        }
    }
}

@Composable
private fun GradeButtons(intervals: Map<Grade, Int>, onGrade: (Grade) -> Unit) {
    val haptics = LocalHapticFeedback.current
    // With very large text, four buttons in a row get cramped; switch to a 2 x 2 grid.
    val stacked = LocalDensity.current.fontScale > 1.3f
    val buttons: List<@Composable (Modifier) -> Unit> = Grade.entries.map { grade ->
        { modifier ->
            GradeButton(grade, intervals[grade], modifier) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onGrade(grade)
            }
        }
    }
    if (stacked) {
        buttons.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { pair.forEach { it(Modifier.weight(1f)) } }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { buttons.forEach { it(Modifier.weight(1f)) } }
    }
}

@Composable
private fun GradeButton(grade: Grade, intervalDays: Int?, modifier: Modifier, onClick: () -> Unit) {
    val interval = intervalDays?.let { if (it == 1) "1 day" else "$it days" }
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(grade.label, style = MaterialTheme.typography.labelLarge)
            interval?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
    val described = modifier
        .heightIn(min = 56.dp)
        .semantics { contentDescription = interval?.let { "${grade.label}. Next review in $it" } ?: grade.label }
    when (grade) {
        Grade.AGAIN -> OutlinedButton(
            onClick = onClick,
            modifier = described,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) { content() }
        Grade.GOOD -> Button(onClick = onClick, modifier = described, contentPadding = ButtonDefaults.TextButtonContentPadding) { content() }
        else -> FilledTonalButton(onClick = onClick, modifier = described, contentPadding = ButtonDefaults.TextButtonContentPadding) { content() }
    }
}

// ---------------------------------------------------------------- Meaning match

@Composable
fun MeaningContent(item: SessionItem.Meaning, playback: PlaybackState, actions: SessionActions) {
    val question = item.question
    val haptics = LocalHapticFeedback.current
    ItemLayout(
        body = {
            PaperCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                SectionLabel("Which word means…")
                Spacer(Modifier.height(8.dp))
                PartOfSpeech(question.sense.partOfSpeech)
                Text(question.prompt, style = MaterialTheme.typography.titleLarge)
            }
            question.options.forEachIndexed { index, option ->
                OptionRow(
                    letter = 'A' + index,
                    text = option,
                    status = when {
                        !item.answered -> OptionStatus.OPEN
                        index == question.answerIndex -> OptionStatus.ANSWER
                        index == item.selectedIndex -> OptionStatus.WRONG_CHOICE
                        else -> OptionStatus.OTHER
                    },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        actions.onChoose(index)
                    },
                )
            }
            AnimatedVisibility(item.answered, enter = fadeIn() + expandVertically()) {
                Feedback(
                    correct = item.isCorrect,
                    headline = if (item.isCorrect) "Correct: ${question.answer}" else "Not quite. The answer is “${question.answer}”.",
                    detail = question.sense.definition,
                    word = question.answer,
                    playback = playback,
                    onPronounce = actions.onPronounce,
                )
            }
        },
        actions = {
            if (item.answered) {
                Button(onClick = actions.onNext, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Next") }
            }
        },
    )
}

private enum class OptionStatus { OPEN, ANSWER, WRONG_CHOICE, OTHER }

/** An answer option. After answering, state is shown with an icon and a label, not colour alone. */
@Composable
private fun OptionRow(letter: Char, text: String, status: OptionStatus, onClick: () -> Unit) {
    val colors = WordkeepTheme.colors
    val (border, container, icon, label) = when (status) {
        OptionStatus.ANSWER -> Quad(colors.correct, colors.correctContainer, Icons.Rounded.CheckCircle, "Correct answer")
        OptionStatus.WRONG_CHOICE -> Quad(colors.incorrect, colors.incorrectContainer, Icons.Rounded.Cancel, "Your answer")
        else -> Quad(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.surfaceContainerLow, null, null)
    }
    Surface(
        onClick = onClick,
        enabled = status == OptionStatus.OPEN,
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(if (icon != null) 2.dp else 1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (label != null) stateDescription = label },
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$letter",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp),
            )
            Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (icon != null && label != null) {
                Icon(icon, contentDescription = null, tint = border, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = border)
            }
        }
    }
}

private data class Quad(
    val border: Color,
    val container: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val label: String?,
)

/** Result panel shared by the quiz modes. Announced by TalkBack as soon as it appears. */
@Composable
private fun Feedback(
    correct: Boolean,
    headline: String,
    detail: String?,
    word: String,
    playback: PlaybackState,
    onPronounce: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    val colors = WordkeepTheme.colors
    Surface(
        color = if (correct) colors.correctContainer else colors.incorrectContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                if (correct) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                contentDescription = null,
                tint = if (correct) colors.correct else colors.incorrect,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(headline, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                extra?.invoke()
                detail?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PronounceButton(word, playback, onPronounce)
        }
    }
}

// ---------------------------------------------------------------- Listen & spell

@Composable
fun SpellingContent(
    item: SessionItem.Spelling,
    playback: PlaybackState,
    hintsEnabled: Boolean,
    actions: SessionActions,
) {
    val word = item.word.word
    val focus = remember { FocusRequester() }
    LaunchedEffect(word.id) { runCatching { focus.requestFocus() } }

    ItemLayout(
        body = {
            PaperCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionLabel("Listen, then type the word")
                    FilledTonalButton(
                        onClick = actions.onPronounce,
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (playback) {
                                PlaybackState.LOADING -> "Loading…"
                                PlaybackState.PLAYING -> "Playing…"
                                PlaybackState.IDLE -> "Play the word"
                            }
                        )
                    }
                    if (hintsEnabled) {
                        TextButton(onClick = actions.onToggleHint) {
                            Text(if (item.hintVisible) "Hide meaning hint" else "Show meaning hint")
                        }
                        AnimatedVisibility(item.hintVisible) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                word.primarySense?.let { sense ->
                                    PartOfSpeech(sense.partOfSpeech)
                                    Text(
                                        DefinitionMasker.mask(sense.definition, word.text),
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = item.attempt,
                onValueChange = actions.onAttemptChange,
                enabled = !item.finished,
                singleLine = true,
                label = { Text("Your spelling") },
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { actions.onSubmitSpelling() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
            SpellingFeedback(item, playback, actions)
        },
        actions = {
            if (item.finished) {
                Button(onClick = actions.onNext, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Next") }
            } else {
                Button(
                    onClick = actions.onSubmitSpelling,
                    enabled = item.attempt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text(if (item.canRetry) "Check again" else "Check") }
                TextButton(onClick = actions.onGiveUp, modifier = Modifier.fillMaxWidth()) { Text("Show me the spelling") }
            }
        },
    )
}

@Composable
private fun SpellingFeedback(item: SessionItem.Spelling, playback: PlaybackState, actions: SessionActions) {
    val word = item.word.word
    when (val verdict = item.verdict) {
        null -> Unit
        is SpellingVerdict.Correct -> Feedback(
            correct = true,
            headline = if (verdict.usedAlternate) "Correct (accepted spelling variant of “${word.text}”)" else "Correct: ${word.text}",
            detail = word.primarySense?.definition,
            word = word.text,
            playback = playback,
            onPronounce = actions.onPronounce,
        )
        is SpellingVerdict.NearMiss -> if (!item.finished) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    "Very close: ${if (verdict.distance == 1) "one letter is" else "${verdict.distance} letters are"} off. Listen again and have one more try.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            WrongSpelling(word.text, verdict.diff, playback, actions)
        }
        is SpellingVerdict.Incorrect -> WrongSpelling(word.text, verdict.diff, playback, actions)
    }
}

@Composable
private fun WrongSpelling(answer: String, diff: List<LetterMark>, playback: PlaybackState, actions: SessionActions) {
    Feedback(
        correct = false,
        headline = "The spelling is “$answer”.",
        detail = describeDiff(diff),
        word = answer,
        playback = playback,
        onPronounce = actions.onPronounce,
        extra = { DiffText(diff) },
    )
}

/**
 * A text version of the underlined letters, e.g. "Check letter 4 (l) and letter 8 (n).", so the
 * same information reaches screen-reader users and anyone who cannot see the underline colour.
 */
internal fun describeDiff(diff: List<LetterMark>): String? {
    val positions = diff.mapIndexedNotNull { index, mark ->
        if (mark.status == LetterMark.Status.MATCHED) null else "letter ${index + 1} (${mark.letter})"
    }
    if (positions.isEmpty()) return null
    val list = if (positions.size == 1) positions.single()
    else positions.dropLast(1).joinToString(", ") + " and " + positions.last()
    return "Check $list."
}

/** The correct word with missed or changed letters underlined and bold (visual aid; the text above says the same). */
@Composable
private fun DiffText(diff: List<LetterMark>) {
    val incorrect = WordkeepTheme.colors.incorrect
    val text = buildAnnotatedString {
        diff.forEach { mark ->
            if (mark.status == LetterMark.Status.MATCHED) {
                append(mark.letter)
            } else {
                withStyle(SpanStyle(color = incorrect, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                    append(mark.letter)
                }
            }
        }
    }
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.clearAndSetSemantics { })
}
