package app.unpeep.privacy.ui.checklist

import androidx.compose.foundation.layout.Arrangement
import app.unpeep.privacy.ui.components.AddPhotoTile
import app.unpeep.privacy.ui.theme.Size
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.unpeep.privacy.R
import app.unpeep.privacy.data.SettingsRepository
import app.unpeep.privacy.data.SweepNotes
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.domain.ChecklistAnswer
import app.unpeep.privacy.domain.ChecklistEntry
import app.unpeep.privacy.domain.ChecklistItem
import app.unpeep.privacy.domain.ChecklistSection
import app.unpeep.privacy.domain.Place
import app.unpeep.privacy.domain.checklist
import app.unpeep.privacy.domain.checklistDone
import app.unpeep.privacy.domain.checklistItems
import app.unpeep.privacy.domain.sweepSteps
import app.unpeep.privacy.ui.components.AnswerMark
import app.unpeep.privacy.ui.components.Buzz
import app.unpeep.privacy.ui.components.ButtonPair
import app.unpeep.privacy.ui.components.ChecklistRow
import app.unpeep.privacy.ui.components.ChoiceRow
import app.unpeep.privacy.ui.components.CircleTone
import app.unpeep.privacy.ui.components.DetailScreen
import app.unpeep.privacy.ui.components.Grouped
import app.unpeep.privacy.ui.components.IconCircle
import app.unpeep.privacy.ui.components.UnpeepSheet
import app.unpeep.privacy.ui.components.UnpeepTextField
import app.unpeep.privacy.ui.components.PhotoThumb
import app.unpeep.privacy.ui.components.PrimaryButton
import app.unpeep.privacy.ui.components.ProgressLine
import app.unpeep.privacy.ui.components.SecondaryButton
import app.unpeep.privacy.ui.components.SectionLabel
import app.unpeep.privacy.ui.components.SheetLine
import app.unpeep.privacy.ui.components.Tag
import app.unpeep.privacy.ui.components.TagKind
import app.unpeep.privacy.ui.components.rememberHaptics
import app.unpeep.privacy.ui.label
import app.unpeep.privacy.ui.next
import app.unpeep.privacy.ui.rememberPermissionAsk
import app.unpeep.privacy.ui.theme.UnpeepTheme
import app.unpeep.privacy.ui.theme.Space
import java.io.File
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Immutable
data class ChecklistState(
    val place: Place,
    val sections: List<Pair<ChecklistSection, List<ChecklistItem>>> = checklist(place),
    val answers: Map<ChecklistItem, ChecklistEntry> = emptyMap(),
    val sweep: Boolean = false,
    /** The step after the checklist in a sweep; null when it is the last (then the button says "See report"). */
    val next: Check? = null,
) {
    val total: Int get() = checklistItems(place).size
    val done: Int get() = checklistDone(place, answers)
}

/** 09 Checklist: one list per place; each item Looks fine · Not sure (photo and note) · Skip. Answers stay in the visit. */
class ChecklistViewModel(private val place: Place, private val sweep: Boolean, settings: SettingsRepository, private val notes: SweepNotes) : ViewModel() {
    init {
        notes.begin(place)
    }

    val state: StateFlow<ChecklistState> = combine(notes.checklist, settings.settings) { answers, s ->
        val steps = sweepSteps(place, s.stepOrder, s.stepsOff)
        ChecklistState(place, answers = answers, sweep = sweep, next = if (sweep) steps.getOrNull(steps.indexOf(Check.CHECKLIST) + 1) else null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChecklistState(place, answers = notes.checklist.value, sweep = sweep))

    fun answer(item: ChecklistItem, entry: ChecklistEntry?) {
        // A photo that is replaced or dropped no longer belongs to the report.
        val old = notes.checklist.value[item]?.photoPath
        if (old != null && old != entry?.photoPath) File(old).delete()
        notes.answer(item, entry)
    }
}

/** A new file for a checklist photo, in app storage; the camera app writes into it through the FileProvider. */
private fun newPhoto(context: Context): Pair<File, Uri> {
    val dir = File(context.filesDir, "photos").apply { mkdirs() }
    val file = File(dir, "check-${System.currentTimeMillis()}.jpg")
    return file to FileProvider.getUriForFile(context, context.packageName + ".files", file)
}

@Composable
fun ChecklistScreen(vm: ChecklistViewModel, onBack: () -> Unit, onNext: (Check?) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf<ChecklistItem?>(null) }
    val haptics = rememberHaptics()
    ChecklistContent(s, onBack, onItem = { open = it }, onNext = { onNext(s.next) })
    open?.let { item ->
        AnswerSheet(
            item, s.answers[item], onDismiss = { open = null },
            onAnswer = { entry ->
                haptics.buzz(if (entry?.answer == ChecklistAnswer.NOT_SURE) Buzz.CONFIRM else Buzz.TICK)
                vm.answer(item, entry)
                open = null
            },
        )
    }
}

/** 09 Checklist: the place and how far along · the progress line · each section's items with their marks · Next or See report. */
@Composable
fun ChecklistContent(s: ChecklistState, onBack: () -> Unit, onItem: (ChecklistItem) -> Unit, onNext: () -> Unit) {
    val progress = stringResource(R.string.checklist_of, s.done, s.total)
    DetailScreen(
        onBack,
        title = stringResource(s.place.label),
        close = s.sweep,
        actions = { Tag(progress, TagKind.GOOD) },
        dock = {
            if (s.sweep && s.next != null) {
                PrimaryButton(stringResource(s.next.next), onNext, Modifier.fillMaxWidth(), trailing = painterResource(R.drawable.ic_arrow_right))
            } else {
                PrimaryButton(stringResource(R.string.see_report), onNext, Modifier.fillMaxWidth())
            }
        },
    ) {
        ProgressLine(if (s.total == 0) 0f else s.done.toFloat() / s.total, progress, Modifier.padding(horizontal = Space.headingInset))
        Spacer(Modifier.height(Space.inline))
        s.sections.forEachIndexed { i, (section, items) ->
            SectionLabel(stringResource(section.label), first = i == 0)
            Grouped {
                items.forEach { item ->
                    val look = item.look
                    val entry = s.answers[item]
                    row { shape ->
                        ChecklistRow(
                            painterResource(look.icon), look.hue, stringResource(look.title),
                            line = when (entry?.answer) {
                                ChecklistAnswer.NOT_SURE -> if (entry.photoPath != null) stringResource(R.string.answer_not_sure_photo) else entry.note.ifBlank { stringResource(R.string.not_sure) }
                                ChecklistAnswer.SKIPPED -> stringResource(R.string.answer_skipped)
                                else -> stringResource(look.line)
                            },
                            mark = when (entry?.answer) {
                                ChecklistAnswer.LOOKS_FINE -> AnswerMark.FINE
                                ChecklistAnswer.NOT_SURE -> AnswerMark.NOT_SURE
                                ChecklistAnswer.SKIPPED -> AnswerMark.SKIPPED
                                null -> AnswerMark.NONE
                            },
                            state = when (entry?.answer) {
                                ChecklistAnswer.LOOKS_FINE -> stringResource(R.string.looks_fine)
                                ChecklistAnswer.NOT_SURE -> stringResource(R.string.not_sure)
                                ChecklistAnswer.SKIPPED -> stringResource(R.string.answer_skipped)
                                null -> null
                            },
                            onClick = { onItem(item) }, shape = shape,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Answering one item: Looks fine and Skip answer at once; Not sure opens a note and a photo, kept with Save.
 * [onAnswer] gets null when an answer is taken back.
 */
@Composable
fun AnswerSheet(item: ChecklistItem, current: ChecklistEntry?, onDismiss: () -> Unit, onAnswer: (ChecklistEntry?) -> Unit) {
    val context = LocalContext.current
    val look = item.look
    // Pick, then Save (09b): a tap only moves the radio. Undo takes a saved answer back.
    var picked by rememberSaveable { mutableStateOf(current?.answer) }
    val notSure = picked == ChecklistAnswer.NOT_SURE
    var note by rememberSaveable { mutableStateOf(current?.note.orEmpty()) }
    var photo by rememberSaveable { mutableStateOf(current?.photoPath) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val file = pending?.let(::File)
        if (taken && file != null && file.length() > 0) photo = file.path else file?.delete()
        pending = null
    }
    fun takePhoto() {
        val (file, uri) = newPhoto(context)
        pending = file.path
        runCatching { camera.launch(uri) }.onFailure { file.delete(); pending = null }
    }
    // The camera app may only be used once Unpeep itself has the camera permission (Android's rule).
    val ask = rememberPermissionAsk(Check.LENS) { granted -> if (granted) takePhoto() }
    UnpeepSheet(
        stringResource(look.title), onDismiss, line = stringResource(look.line),
        footer = {
            ButtonPair(
                secondary = { m ->
                    if (current != null) SecondaryButton(stringResource(R.string.undo), { onAnswer(null) }, m)
                    else SecondaryButton(stringResource(R.string.cancel), onDismiss, m)
                },
                primary = { m ->
                    PrimaryButton(stringResource(R.string.save), {
                        picked?.let { a -> onAnswer(if (a == ChecklistAnswer.NOT_SURE) ChecklistEntry(a, note.trim(), photo) else ChecklistEntry(a)) }
                    }, m, enabled = picked != null)
                },
            )
        },
    ) {
        Column(Modifier.selectableGroup()) {
            ChoiceRow(
                stringResource(R.string.looks_fine), picked == ChecklistAnswer.LOOKS_FINE, { picked = ChecklistAnswer.LOOKS_FINE },
                line = stringResource(R.string.looks_fine_line), leading = { IconCircle(painterResource(R.drawable.ic_check), CircleTone.Accent) },
            )
            ChoiceRow(
                stringResource(R.string.not_sure), notSure, { picked = ChecklistAnswer.NOT_SURE },
                line = stringResource(R.string.not_sure_item_line), leading = { IconCircle(painterResource(R.drawable.ic_help), CircleTone.Warning) },
            )
            ChoiceRow(
                stringResource(R.string.skip), picked == ChecklistAnswer.SKIPPED, { picked = ChecklistAnswer.SKIPPED },
                line = stringResource(R.string.skip_line), leading = { IconCircle(painterResource(R.drawable.ic_skip), CircleTone.Neutral) },
            )
        }
        if (notSure) {
            Column(Modifier.padding(start = Space.screen, end = Space.screen, top = Space.block)) {
                UnpeepTextField(note, { note = it }, stringResource(R.string.note_hint), stringResource(R.string.note_label), singleLine = false)
                Spacer(Modifier.height(Space.block))
                // The photo (09c): its thumbnail, then the dashed tile that takes one (or another), named beside it.
                val take = { if (app.unpeep.privacy.scan.Access.granted(context, Check.LENS)) takePhoto() else ask.ask() }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.inline)) {
                    photo?.let { PhotoThumb(it, stringResource(R.string.change_photo), size = Size.photoSmall, onClick = take) }
                    AddPhotoTile(stringResource(R.string.add_photo), take)
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChecklistPreview() = UnpeepTheme {
    ChecklistContent(
        ChecklistState(
            Place.TRIAL_ROOM,
            answers = mapOf(
                ChecklistItem.MIRROR_LIGHT to ChecklistEntry(ChecklistAnswer.LOOKS_FINE),
                ChecklistItem.HOOKS to ChecklistEntry(ChecklistAnswer.NOT_SURE, "", "/x.jpg"),
                ChecklistItem.LAMPS to ChecklistEntry(ChecklistAnswer.LOOKS_FINE),
                ChecklistItem.CORNERS to ChecklistEntry(ChecklistAnswer.SKIPPED),
            ),
        ),
        {}, {}, {},
    )
}
