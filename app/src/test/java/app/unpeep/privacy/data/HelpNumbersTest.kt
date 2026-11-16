package app.unpeep.privacy.data

import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The bundled help numbers: well formed, one entry per country, and the phone's region picked by default. */
class HelpNumbersTest {
    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }
    private val text = File(root, "app/src/main/assets/help/numbers.json").readText()
    private fun numbers(region: String?) = HelpNumbers({ text }, { region })

    @Test fun everyEntryIsACountryWithDiallableNumbers() {
        val all = numbers(null).all
        assertTrue(all.size >= 40)
        val countries = Locale.getISOCountries().toSet()
        for (e in all) {
            assertTrue(e.country, e.country in countries)
            for (n in listOfNotNull(e.emergency, e.police, e.line)) assertTrue("${e.country}: $n", Regex("^#?[0-9][0-9 -]{1,12}$").matches(n))
            e.site?.let { assertTrue("${e.country}: $it", it.startsWith("https://")) }
        }
    }

    @Test fun noCountryTwiceAndSorted() {
        val codes = numbers(null).all.map { it.country }
        assertEquals(codes.toSet().size, codes.size)
        assertEquals(codes.sorted(), codes)
    }

    @Test fun thePhonesRegionIsTheDefault() {
        // Germany's police emergency number is 110 (112 reaches fire and ambulance), checked 16 Nov 2026.
        assertEquals("110", numbers("de").entry(null)?.emergency)
        assertEquals("911", numbers("de").entry("US")?.emergency)
    }

    @Test fun aSeparatePoliceLineWhereOneExists() {
        val gb = numbers(null).entry("GB")!!
        assertEquals("999", gb.emergency)
        assertEquals("101", gb.police)
        assertEquals("#9110", numbers(null).entry("JP")!!.police)
    }

    @Test fun anUnknownOrMissingRegionHasNoEntry() {
        assertNull(numbers("zz").entry(null))
        assertNull(numbers(null).entry(null))
        assertNull(numbers("").entry(null))
        assertEquals("112", HelpNumbers.FALLBACK)
    }

    @Test fun indiaHasItsCybercrimeLineAndSite() {
        val india = numbers("in").entry(null)!!
        assertEquals("1930", india.line)
        assertEquals("https://cybercrime.gov.in/", india.site)
    }
}
