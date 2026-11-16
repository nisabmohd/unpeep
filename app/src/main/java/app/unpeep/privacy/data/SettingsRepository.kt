package app.unpeep.privacy.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.unpeep.privacy.domain.Check
import app.unpeep.privacy.domain.Place
import app.unpeep.privacy.ui.theme.BodyFont
import app.unpeep.privacy.ui.theme.DEFAULT_ACCENT_HUE
import app.unpeep.privacy.ui.theme.TextSize
import app.unpeep.privacy.ui.theme.ThemeMode
import app.unpeep.privacy.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Everything the user sets (DataStore). Few settings: one right way to do each thing. */
@Immutable
data class Settings(
    val theme: ThemeSettings = ThemeSettings(),
    /** False until Welcome's "Get started". */
    val onboarded: Boolean = false,
    /** The place picked on Home. */
    val place: Place = Place.STAY,
    /** Sweep steps (More › Sweep steps): the order, and the ones switched off. */
    val stepOrder: List<Check> = Check.entries,
    val stepsOff: Set<Check> = emptySet(),
    /** Help numbers: an ISO country code, or null to follow the phone's region. */
    val helpRegion: String? = null,
    /** Magnet: beep on a jump. */
    val magnetBeep: Boolean = false,
    /** True once the TV-remote test showed this phone sees infrared; null = not tried. */
    val seesInfrared: Boolean? = null,
    /** True once the TV-remote question was shown and closed without an answer: it isn't asked again by itself. */
    val infraredAsked: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

class SettingsRepository(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.dataStore)

    val settings: Flow<Settings> = store.data.map(::read).distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setDynamicColor(on: Boolean) = store.edit { it[DYNAMIC] = on }
    suspend fun setAccentHue(hue: Float) = store.edit { it[ACCENT] = hue; it[DYNAMIC] = false }
    suspend fun setFont(font: BodyFont) = store.edit { it[FONT] = font.name }
    suspend fun setTextSize(size: TextSize) = store.edit { it[TEXT_SIZE] = size.name }
    suspend fun setOnboarded() = store.edit { it[ONBOARDED] = true }
    suspend fun setPlace(place: Place) = store.edit { it[PLACE] = place.name }
    suspend fun setStepOrder(order: List<Check>) = store.edit { it[STEP_ORDER] = order.joinToString(",") { c -> c.name } }
    suspend fun setStepOn(check: Check, on: Boolean) = store.edit { p ->
        val off = parseChecks(p[STEPS_OFF]).toMutableSet()
        if (on) off -= check else off += check
        p[STEPS_OFF] = off.joinToString(",") { c -> c.name }
    }
    suspend fun setHelpRegion(code: String?) = store.edit { if (code == null) it.remove(HELP_REGION) else it[HELP_REGION] = code }
    suspend fun setMagnetBeep(on: Boolean) = store.edit { it[MAGNET_BEEP] = on }
    suspend fun setSeesInfrared(sees: Boolean) = store.edit { it[SEES_IR] = sees }
    suspend fun clearSeesInfrared() = store.edit { it.remove(SEES_IR); it.remove(IR_ASKED) }
    suspend fun setInfraredAsked() = store.edit { it[IR_ASKED] = true }

    companion object {
        private val THEME = stringPreferencesKey("theme")
        private val DYNAMIC = booleanPreferencesKey("dynamic_color")
        private val ACCENT = floatPreferencesKey("accent_hue")
        private val FONT = stringPreferencesKey("font")
        private val TEXT_SIZE = stringPreferencesKey("text_size")
        private val ONBOARDED = booleanPreferencesKey("onboarded")
        private val PLACE = stringPreferencesKey("place")
        private val STEP_ORDER = stringPreferencesKey("step_order")
        private val STEPS_OFF = stringPreferencesKey("steps_off")
        private val HELP_REGION = stringPreferencesKey("help_region")
        private val MAGNET_BEEP = booleanPreferencesKey("magnet_beep")
        private val SEES_IR = booleanPreferencesKey("sees_infrared")
        private val IR_ASKED = booleanPreferencesKey("infrared_asked")

        private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: default

        /** Names → checks, dropping anything unknown (an older or newer version's value) and repeats. */
        fun parseChecks(text: String?): List<Check> =
            text.orEmpty().split(',').mapNotNull { n -> Check.entries.firstOrNull { it.name == n.trim() } }.distinct()

        /** A saved order always lists every check: ones it lacks (added in a later version) go at the end. */
        fun fullOrder(saved: List<Check>): List<Check> = saved + (Check.entries - saved.toSet())

        fun read(p: Preferences) = Settings(
            theme = ThemeSettings(
                mode = enumOr(p[THEME], ThemeMode.SYSTEM),
                dynamicColor = p[DYNAMIC] ?: false,
                accentHue = p[ACCENT] ?: DEFAULT_ACCENT_HUE,
                font = enumOr(p[FONT], BodyFont.LEXEND),
                textSize = enumOr(p[TEXT_SIZE], TextSize.DEFAULT),
            ),
            onboarded = p[ONBOARDED] ?: false,
            place = enumOr(p[PLACE], Place.STAY),
            stepOrder = fullOrder(parseChecks(p[STEP_ORDER])),
            stepsOff = parseChecks(p[STEPS_OFF]).toSet(),
            helpRegion = p[HELP_REGION],
            magnetBeep = p[MAGNET_BEEP] ?: false,
            seesInfrared = p[SEES_IR],
            infraredAsked = p[IR_ASKED] ?: false,
        )
    }
}
