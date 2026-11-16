package app.keyhole.privacy.ui.wifi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.KnownAs
import app.keyhole.privacy.ui.components.ButtonPair
import app.keyhole.privacy.ui.components.ChoiceRow
import app.keyhole.privacy.ui.components.CircleTone
import app.keyhole.privacy.ui.components.IconCircle
import app.keyhole.privacy.ui.components.KeyholeSheet
import app.keyhole.privacy.ui.components.PrimaryButton
import app.keyhole.privacy.ui.components.SecondaryButton
import app.keyhole.privacy.ui.components.SheetLine
import app.keyhole.privacy.ui.theme.Hues

/** The three answers; [NOT_SURE] is no mark, so the device stays on the list. */
private enum class Answer { MINE, ROOM, NOT_SURE }

/**
 * 06 Not a camera?: It's mine · Part of the room · Not sure, and a line to report a wrong result. The answer stays
 * on the phone. [onMark] gets null for Not sure.
 */
@Composable
fun NotACameraSheet(current: KnownAs?, onDismiss: () -> Unit, onMark: (KnownAs?) -> Unit, onReport: () -> Unit) {
    var answer by rememberSaveable {
        mutableStateOf(
            when (current) {
                KnownAs.ROOM -> Answer.ROOM
                KnownAs.MINE, null -> Answer.MINE
            },
        )
    }
    KeyholeSheet(
        stringResource(R.string.not_a_camera), onDismiss, line = stringResource(R.string.not_a_camera_sheet_line),
        footer = {
            ButtonPair(
                secondary = { m -> SecondaryButton(stringResource(R.string.cancel), onDismiss, m) },
                primary = { m ->
                    PrimaryButton(
                        stringResource(if (answer == Answer.NOT_SURE) R.string.done else R.string.mark_as_known),
                        {
                            onMark(
                                when (answer) {
                                    Answer.MINE -> KnownAs.MINE
                                    Answer.ROOM -> KnownAs.ROOM
                                    Answer.NOT_SURE -> null
                                },
                            )
                        },
                        m,
                    )
                },
            )
        },
    ) {
        Column(Modifier.selectableGroup()) {
            ChoiceRow(
                stringResource(R.string.its_mine), answer == Answer.MINE, { answer = Answer.MINE }, line = stringResource(R.string.its_mine_line),
                leading = { IconCircle(painterResource(R.drawable.ic_smartphone), CircleTone.Hue(Hues.WIFI)) },
            )
            ChoiceRow(
                stringResource(R.string.part_of_the_room), answer == Answer.ROOM, { answer = Answer.ROOM }, line = stringResource(R.string.part_of_the_room_line),
                leading = { IconCircle(painterResource(R.drawable.ic_tv), CircleTone.Hue(Hues.HOTSPOTS)) },
            )
            ChoiceRow(
                stringResource(R.string.not_sure), answer == Answer.NOT_SURE, { answer = Answer.NOT_SURE }, line = stringResource(R.string.not_sure_line),
                leading = { IconCircle(painterResource(R.drawable.ic_help), CircleTone.Warning) },
            )
        }
        SheetLine(stringResource(R.string.report_wrong), icon = painterResource(R.drawable.ic_chat), onClick = onReport, leaves = true)
    }
}
