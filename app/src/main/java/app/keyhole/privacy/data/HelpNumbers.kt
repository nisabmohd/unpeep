package app.keyhole.privacy.data

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Who to call where you are (screen 11, sheet 15): the emergency number, the police's own number when it differs,
 * and where to report online, per country. Bundled, so it works offline; add countries by pull request.
 */
@Serializable
data class HelpEntry(
    /** ISO 3166 code ("DE"). */
    val country: String,
    val emergency: String,
    val police: String = emergency,
    /** The official page to report it, if the country has one. */
    val site: String? = null,
    /** A helpline for this kind of crime (India's cybercrime line), if any. */
    val line: String? = null,
)

class HelpNumbers(private val read: () -> String, private val phoneRegion: () -> String?) {
    constructor(context: Context) : this(
        { context.assets.open("help/numbers.json").bufferedReader().use { it.readText() } },
        { region(context) },
    )

    val all: List<HelpEntry> by lazy { parse(read()) }

    /** The phone's own country (SIM, then network, then language settings); null when nothing says. */
    fun phoneCountry(): String? = phoneRegion()?.uppercase()?.takeIf { it.length == 2 }

    /** The entry for [country], or the phone's when null; null when the list has no such country. */
    fun entry(country: String?): HelpEntry? {
        val code = country ?: phoneCountry() ?: return null
        return all.firstOrNull { it.country.equals(code, ignoreCase = true) }
    }

    companion object {
        /** Where nothing is known: the number GSM phones route to emergency services in most of the world. */
        const val FALLBACK = "112"

        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): List<HelpEntry> = json.decodeFromString<List<HelpEntry>>(text)

        private fun region(context: Context): String? {
            val tm = context.getSystemService(TelephonyManager::class.java)
            return tm?.simCountryIso?.takeIf { it.isNotBlank() } ?: tm?.networkCountryIso?.takeIf { it.isNotBlank() } ?: Locale.getDefault().country.takeIf { it.isNotBlank() }
        }
    }
}
