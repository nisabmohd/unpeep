package app.keyhole.privacy.rules

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * CLAUDE.md's rules, checked on the source so a whole class of mistake can't come back: sockets outside
 * LocalOnly, raw colours / sizes / text sizes / motion outside the theme, dialogs, hard-coded text, words the app
 * never says, broken translations, Google code in foss. Fix the code, never the test.
 */
class ProjectRulesTest {
    private val root: File = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }
    private val src = File(root, "app/src")
    private val main = File(src, "main")
    private val kotlin: List<File> = File(main, "java").walkTopDown().filter { it.extension == "kt" }.toList()
    private val allKotlin: List<File> = src.walkTopDown().filter { it.extension == "kt" && !it.path.replace('\\', '/').contains("/src/test/") && !it.path.replace('\\', '/').contains("/src/androidTest/") }.toList()
    private fun rel(f: File) = f.relativeTo(root).path.replace('\\', '/')
    private fun code(l: String) = l.substringBefore("//").takeIf { !l.trim().startsWith("*") && !l.trim().startsWith("/*") } ?: ""
    private fun inTheme(f: File) = rel(f).contains("/ui/theme/")
    private fun inComponents(f: File) = rel(f).contains("/ui/components/")

    private fun violations(files: List<File> = kotlin, predicate: (File, Int, String) -> Boolean): List<String> =
        files.flatMap { f -> f.readLines().mapIndexedNotNull { i, l -> if (predicate(f, i, l)) "${rel(f)}:${i + 1}: ${l.trim().take(120)}" else null } }

    private fun assertNone(what: String, found: List<String>) {
        if (found.isNotEmpty()) fail("$what (${found.size}):\n" + found.joinToString("\n"))
    }

    // --- Hard rules --------------------------------------------------------------------------------

    /** Local network only: every socket is made in scan/net/LocalOnly.kt, which refuses anything not local. */
    @Test fun socketsOnlyInLocalOnly() = assertNone("a socket outside scan/net/LocalOnly.kt",
        violations(allKotlin) { f, _, l ->
            !rel(f).endsWith("scan/net/LocalOnly.kt") &&
                Regex("""(?<![\w.])(Socket|DatagramSocket|MulticastSocket|ServerSocket|SSLSocket|SocketChannel|DatagramChannel)\s*\(|SocketFactory|\.open\(\)\s*//\s*channel|SocketChannel\.open|DatagramChannel\.open""").containsMatchIn(code(l))
        })

    /** Only the github build's update check may open a URL; nothing else talks to the internet. */
    @Test fun noInternetCallsOutsideTheUpdateCheck() = assertNone("a URL connection outside data/update (github update check)",
        violations(allKotlin) { f, _, l ->
            !rel(f).contains("/data/update/") &&
                Regex("""openConnection\(|HttpURLConnection|HttpsURLConnection|\bURL\(|InetAddress\.getByName\(|InetAddress\.getAllByName\(|DownloadManager|WebView\b|okhttp|retrofit|ktor""", RegexOption.IGNORE_CASE).containsMatchIn(code(l))
        })

    /** LocalOnly really is the guard: every way out checks the address first. */
    @Test fun localOnlyChecksEveryAddress() {
        val t = File(main, "java/app/keyhole/privacy/scan/net/LocalOnly.kt").readText()
        for (fn in listOf("fun connect(", "fun send(")) {
            val body = t.substringAfter(fn).substringBefore("\n    }\n")
            assertTrue("$fn must call require(address) before using the socket", body.indexOf("require(address)") in 0 until (listOf(body.indexOf(".connect("), body.indexOf(".send(")).filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE))
        }
    }

    /** Detect, never intrude: no logins, no passwords, no streams, no writes. */
    @Test fun neverIntrudes() = assertNone("something that logs in, tries a password, or opens a stream",
        violations(allKotlin) { _, _, l ->
            Regex("""Authorization:|Basic |Digest |\bDESCRIBE\b|\bSETUP\b|\bPLAY\b rtsp|password\s*=|"admin"|"12345"|"POST |"PUT |"DELETE |GetStreamUri|GetSnapshotUri""").containsMatchIn(code(l))
        })

    /** A scan never runs in the background or on a schedule. */
    @Test fun nothingRunsInTheBackground() = assertNone("background or scheduled work",
        violations(allKotlin) { _, _, l -> Regex("""WorkManager|JobScheduler|AlarmManager|PeriodicWorkRequest|ForegroundService|startForeground\(|BOOT_COMPLETED""").containsMatchIn(code(l)) })

    /** No analytics, no crash reporting, no accounts, no ads. */
    @Test fun noTrackingLibraries() {
        val gradle = File(root, "app/build.gradle.kts").readText() + File(root, "gradle/libs.versions.toml").readText()
        for (bad in listOf("firebase", "crashlytics", "analytics", "sentry", "bugsnag", "admob", "play-services-ads", "facebook", "appsflyer", "mixpanel", "amplitude", "okhttp", "retrofit", "ktor", "hilt", "dagger", "koin", "coil", "glide", "work-runtime", "material-icons-extended"))
            assertTrue("\"$bad\" in the Gradle files", !gradle.contains(bad, ignoreCase = true))
    }

    /** foss never contains Google code: Play-only code lives in src/play, with a stand-in in src/noPlay. */
    @Test fun fossHasNoGoogleCode() {
        val found = src.walkTopDown().filter { it.extension == "kt" && !rel(it).contains("/src/play/") && !rel(it).contains("/src/test/") }
            .filter { f -> f.readLines().any { Regex("""^import com\.google\.android\.(gms|play)""").containsMatchIn(it) } }.map(::rel).toList()
        assertNone("com.google.android.gms / play import outside src/play", found)
        val gradle = File(root, "app/build.gradle.kts").readLines().filter { "libs.play" in it }
        assertTrue("Play libraries must be playImplementation only: $gradle", gradle.all { "\"playImplementation\"" in it })
    }

    /** Only the permissions the plan lists (docs/PLAN.md § Permissions), in every manifest. */
    @Test fun onlyThePlannedPermissions() {
        val allowed = setOf(
            "INTERNET", "ACCESS_NETWORK_STATE", "ACCESS_WIFI_STATE", "CHANGE_WIFI_STATE", "CHANGE_WIFI_MULTICAST_STATE", "NEARBY_WIFI_DEVICES",
            "ACCESS_FINE_LOCATION", "BLUETOOTH_SCAN", "BLUETOOTH", "BLUETOOTH_ADMIN", "CAMERA",
        )
        val found = src.walkTopDown().filter { it.name == "AndroidManifest.xml" }.flatMap { f ->
            Regex("""uses-permission[^>]*android:name="android\.permission\.([A-Z_]+)"""").findAll(f.readText()).map { it.groupValues[1] }.filter { it !in allowed }.map { "${rel(f)}: $it" }
        }.toList()
        assertNone("a permission the plan doesn't list", found)
        val m = File(main, "AndroidManifest.xml").readText()
        assertTrue("location is only for Wi-Fi scan results before Android 13", Regex("""ACCESS_FINE_LOCATION"\s+android:maxSdkVersion="32"""").containsMatchIn(m))
        assertTrue("Bluetooth and nearby Wi-Fi never derive location", Regex("neverForLocation").findAll(m).count() == 2)
        assertTrue("no backups", "android:allowBackup=\"false\"" in m)
    }

    // --- Tokens: one place for each kind of value ----------------------------------------------------

    @Test fun animationsOnlyInMotion() = assertNone("tween( / spring( outside Motion.kt",
        violations { f, _, l -> f.name != "Motion.kt" && Regex("""\b(tween|spring|keyframes|snap)\(""").containsMatchIn(code(l)) })

    @Test fun coloursOnlyInColorKt() = assertNone("raw Color(0x…) / Color.hsl outside Color.kt",
        violations { f, _, l -> f.name != "Color.kt" && Regex("""Color\(0x|Color\.hsl\(|Color\.hsv\(|Color\(\s*\d""").containsMatchIn(code(l)) })

    @Test fun noNamedColours() = assertNone("named colour (Color.Gray, Color.Black…): use a colour role or a Color.kt token",
        violations { f, _, l -> !inTheme(f) && Regex("""\bColor\.(Black|White|Gray|Grey|DarkGray|LightGray|Red|Green|Blue|Yellow|Cyan|Magenta)\b""").containsMatchIn(code(l)) })

    @Test fun alphasOnlyInColorKt() = assertNone("raw alpha: add or use a token in Alpha (Color.kt)",
        violations { f, _, l -> !inTheme(f) && Regex("""alpha\s*=\s*\d*\.\d|\.alpha\(\s*\d*\.\d""").containsMatchIn(code(l)) })

    @Test fun textSizesOnlyInTypeKt() = assertNone("raw .sp / FontWeight outside Type.kt",
        violations { f, _, l -> f.name != "Type.kt" && Regex("""\d\.sp\b|FontWeight\.|fontSize\s*=\s*\d""").containsMatchIn(code(l)) })

    @Test fun sizesOnlyInDimensKt() = assertNone("raw .dp outside Dimens.kt: add a token to Space / Size / LensSize",
        violations { f, _, l -> !inTheme(f) && Regex("""(?<![\w.])(?!0\.dp)\d+(\.\d+)?\.dp\b""").containsMatchIn(code(l)) })

    @Test fun shapesOnlyInDimensKt() = assertNone("RoundedCornerShape / CircleShape outside Dimens.kt: use Shapes.*",
        violations { f, _, l -> !inTheme(f) && Regex("""\b(RoundedCornerShape|CutCornerShape|AbsoluteRoundedCornerShape)\(|\bCircleShape\b""").containsMatchIn(code(l)) })

    @Test fun borderWidthsAreNamed() = assertNone(".border( width that isn't Size.outline / Size.ring",
        violations { f, _, l -> !inTheme(f) && Regex("""\.border\(\s*(?!Size\.(outline|ring)\b)[^)]""").containsMatchIn(code(l)) })

    /** Space.* is a gap, Size.* is a size; screens don't do sums on tokens. */
    @Test fun spacingTokensAreGapsNotSizes() = assertNone("spacing token used as a size, or token arithmetic on a screen: add a named token",
        violations { f, _, l ->
            val c = code(l)
            !inTheme(f) && (
                Regex("""\.(size|width|height|requiredSize|border)\(\s*Space\.""").containsMatchIn(c) && !Regex("""Spacer\(Modifier\.(height|width|size)\(Space\.\w+\)\)""").containsMatchIn(c) ||
                    !inComponents(f) && Regex("""(Spacer|padding|size|height|width|spacedBy)\([^)]*\b(Space|Size)\.\w+\s*[-+*/]\s*[\w.]""").containsMatchIn(c)
                )
        })

    // --- One look per element ------------------------------------------------------------------------

    /** Sheets first: every choice, picker and confirm is a KeyholeSheet. No dialogs anywhere. */
    @Test fun noDialogs() = assertNone("a dialog (use KeyholeSheet)",
        violations { _, _, l -> Regex("""(?<![\w.])(AlertDialog|BasicAlertDialog|Dialog|DatePickerDialog|TimePickerDialog)\(|AlertDialog\.Builder|DialogFragment""").containsMatchIn(code(l)) })

    @Test fun sheetsOnlyThroughKeyholeSheet() = assertNone("ModalBottomSheet outside KeyholeSheet.kt",
        violations { f, _, l -> f.name != "KeyholeSheet.kt" && Regex("""(?<![\w.])ModalBottomSheet\(""").containsMatchIn(code(l)) })

    /** Screens build from the shared components only: Material widgets are wrapped once in ui/components. */
    @Test fun materialWidgetsOnlyInComponents() = assertNone("Material widget on a screen: use the Keyhole component",
        violations { f, _, l ->
            !inComponents(f) && !inTheme(f) &&
                Regex("""(?<![\w.])(Button|OutlinedButton|TextButton|FilledTonalButton|ElevatedButton|IconButton|FilledIconButton|FilledTonalIconButton|FilterChip|AssistChip|SuggestionChip|InputChip|Switch|Checkbox|RadioButton|Slider|Card|ElevatedCard|OutlinedCard|ModalBottomSheet|HorizontalDivider|VerticalDivider|Divider|NavigationBar|TopAppBar|CenterAlignedTopAppBar|Scaffold|FloatingActionButton|LinearProgressIndicator|CircularProgressIndicator)\(""").containsMatchIn(code(l))
        })

    /** No divider lines: rows sit 2 dp apart. */
    @Test fun noDividers() = assertNone("a divider", violations { _, _, l -> Regex("""(?<![\w.])(HorizontalDivider|VerticalDivider|Divider)\(""").containsMatchIn(code(l)) })

    @Test fun noMaterialIconsLibrary() = assertNone("material-icons (use Solar drawables)",
        violations { _, _, l -> "androidx.compose.material.icons" in l })

    /** Every Text names its style (KeyholeType); the Material default would bring another size and weight. */
    @Test fun everyTextHasAStyle() {
        val found = kotlin.flatMap { f ->
            val t = f.readText()
            Regex("""(?<![\w.])(Text|BasicText)\(""").findAll(t).mapNotNull { m ->
                var i = m.range.last + 1; var depth = 1
                while (i < t.length && depth > 0) { if (t[i] == '(') depth++ else if (t[i] == ')') depth--; i++ }
                val call = t.substring(m.range.first, i)
                if ("style" in call) null else "${rel(f)}:${t.substring(0, m.range.first).count { it == '\n' } + 1}: ${call.take(90)}"
            }.toList()
        }
        assertNone("Text( without style =", found)
    }

    /** Only the eight styles: nobody reaches around KeyholeType into MaterialTheme.typography. */
    @Test fun onlyTheEightTextStyles() = assertNone("MaterialTheme.typography outside Type.kt (use KeyholeType)",
        violations { f, _, l -> f.name != "Type.kt" && "MaterialTheme.typography" in code(l) })

    @Test fun noHardCodedUserText() = assertNone("Text(\"…\") / contentDescription = \"…\" with words: use strings.xml",
        violations { _, _, l ->
            val c = code(l)
            (Regex("""\b(Text|BasicText)\(\s*"([^"]*)"""").find(c)?.groupValues?.get(2) ?: Regex("""contentDescription\s*=\s*"([^"]*)"""").find(c)?.groupValues?.get(1))
                ?.replace(Regex("""\$\{[^}]*\}|\$[A-Za-z_]+"""), "")
                ?.let { Regex("[A-Za-z]{2,}").containsMatchIn(it) } == true
        })

    /** A clickable sits after a clip(...) in its modifier chain, or its press ripple is a square. */
    @Test fun everyTapRippleIsClippedToItsShape() {
        val tap = Regex("""\.(clickable|combinedClickable|selectable|toggleable)\(""")
        val found = kotlin.flatMap { f ->
            val lines = f.readLines()
            lines.indices.mapNotNull { i ->
                val m = tap.find(lines[i]) ?: return@mapNotNull null
                if ("indication = null" in lines[i]) return@mapNotNull null
                val before = (maxOf(0, i - 10) until i).joinToString("\n") { lines[it] } + "\n" + lines[i].substring(0, m.range.first)
                val outer = before.trimEnd().removeSuffix("Modifier").replace(Regex("""(->\s*|if\s*\([^()]*\)\s*|else\s+)Modifier"""), "")
                val fromStart = outer.substring(outer.lastIndexOf("odifier").coerceAtLeast(0))
                // A full-bleed sheet row (ChoiceRow) is a rectangle on purpose: its ripple is the row.
                if ("clip(" in fromStart || f.name == "Controls.kt" && "fun ChoiceRow" in lines.subList(maxOf(0, i - 12), i).joinToString("\n")) null
                else "${rel(f)}:${i + 1}: ${lines[i].trim().take(110)}"
            }
        }
        assertNone("clickable without a clip before it: its press ripple is a square", found)
    }

    /** No buttons inside cards: a composable named …Row, …Tile, …Card or Box24 never draws a button. */
    @Test fun noButtonsInsideCards() {
        val button = Regex("""(?<![\w.])(PrimaryButton|SecondaryButton|ButtonPair|RoundIconButton|KeyholeButton|Button|TextButton|IconButton)\(""")
        val found = kotlin.flatMap { f ->
            val t = f.readText()
            Regex("""fun (\w*(Card|Row|Tile|Box24|Note))\(""").findAll(t).filter { it.groupValues[1] !in setOf("CheckRow") }.mapNotNull { m ->
                val open = t.indexOf('{', t.indexOf(')', m.range.last)).takeIf { it >= 0 } ?: return@mapNotNull null
                var i = open + 1; var depth = 1
                while (i < t.length && depth > 0) { if (t[i] == '{') depth++ else if (t[i] == '}') depth--; i++ }
                if (button.containsMatchIn(t.substring(open, i))) "${rel(f)}: ${m.groupValues[1]} draws a button" else null
            }.toList()
        }
        assertNone("button inside a card / row / tile", found)
    }

    /** A one-line label never wraps out of sight: it ends in "…" or shrinks to fit. */
    @Test fun oneLineTextNeverHidesWords() = assertNone("Text(maxLines = 1) without overflow / softWrap = false / autoSize",
        violations { _, _, l -> "Text(" in l && Regex("""maxLines = 1\b""").containsMatchIn(l) && !Regex("overflow|softWrap|autoSize").containsMatchIn(l) })

    /** Lazy items have stable keys, so rows keep their state when the list changes. */
    @Test fun lazyItemsHaveKeys() = assertNone("items / itemsIndexed over data without key =",
        violations { _, _, l -> Regex("""\b(itemsIndexed|items)\(""").containsMatchIn(code(l)) && "key" !in l })

    @Test fun notificationsUseTheKeyholeMark() = assertNone("setSmallIcon not R.drawable.ic_notification",
        violations { _, _, l -> "setSmallIcon(" in l && "ic_notification" !in l })

    /** Every screen's components are listed in docs/COMPONENTS.md: add it there first. */
    @Test fun everyComponentIsDocumented() {
        val doc = File(root, "docs/COMPONENTS.md").readText()
        val missing = File(main, "java/app/keyhole/privacy/ui/components").listFiles().orEmpty().flatMap { f ->
            Regex("""(?m)^fun ([A-Z]\w+)\(""").findAll(f.readText().replace(Regex("""@Composable\s+"""), "")).map { it.groupValues[1] }.filter { "`$it`" !in doc }.map { "${f.name}: $it" }.toList()
        }
        assertNone("component missing from docs/COMPONENTS.md", missing)
    }

    /** Icons are Solar drawables written by tools/solar_icon.py, the brand marks by tools/keyhole_mark.py. No PNGs in the app. */
    @Test fun iconsComeFromTheTools() {
        val res = File(main, "res")
        val bitmaps = res.walkTopDown().filter { it.extension in setOf("png", "jpg", "jpeg", "webp", "gif") }.map(::rel).toList()
        assertNone("bitmap in res (vector drawables only)", bitmaps)
        val marks = setOf("ic_launcher_foreground.xml", "ic_launcher_monochrome.xml", "ic_notification.xml")
        val hand = File(res, "drawable").listFiles().orEmpty().filter { it.name.startsWith("ic_") && it.name !in marks && "Solar" !in it.readText() && "tools/solar_icon.py" !in it.readText() }.map(::rel)
        assertNone("icon not written by tools/solar_icon.py", hand)
    }

    // --- Words ---------------------------------------------------------------------------------------

    private val stringRe = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val itemRe = Regex("""<item quantity="[^"]+">(.*?)</item>""", RegexOption.DOT_MATCHES_ALL)
    private val pluralRe = Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
    private fun text(dir: String): String = File(main, "res/$dir/strings.xml").takeIf { it.exists() }?.readText().orEmpty()
    private fun strings(dir: String): Map<String, String> = stringRe.findAll(text(dir)).associate { it.groupValues[1] to it.groupValues[2] }
    private fun plurals(dir: String): Map<String, List<String>> = pluralRe.findAll(text(dir)).associate { p -> p.groupValues[1] to itemRe.findAll(p.groupValues[2]).map { it.groupValues[1] }.toList() }
    private val languages = File(main, "res").listFiles().orEmpty().map { it.name }.filter { it.startsWith("values-") && File(main, "res/$it/strings.xml").exists() }.sorted()
    private val english = strings("values")
    private val englishPlurals = plurals("values")
    private val untranslatable = Regex("""<string name="([^"]+)"[^>]*translatable="false"""").findAll(text("values")).map { it.groupValues[1] }.toSet()
    private fun placeholders(s: String) = Regex("""%(\d+\$)?[sdf]""").findAll(s).map { it.value }.sorted().toList()
    private fun everyEnglishText(): List<Pair<String, String>> = english.toList() + englishPlurals.flatMap { (k, v) -> v.map { k to it } }

    /** Honest results: the app never says a room is safe, and never uses fear words (docs/COPY.md). */
    @Test fun theAppNeverSaysSafeOrSpy() {
        val banned = Regex("""\b(safe|safely|safety|clean|secure|secured|protected|spy|spying|danger|dangerous|threat|hacker|hacked|oops|bug detect\w*|no cameras)\b""", RegexOption.IGNORE_CASE)
        val bad = everyEnglishText().filter { (k, v) -> banned.containsMatchIn(v) && k !in WORDS_ALLOWED }.map { (k, v) -> "$k: $v" }
        assertNone("a word the app never says", bad)
    }

    @Test fun noExclamationMarks() = assertNone("an exclamation mark", everyEnglishText().filter { (_, v) -> "!" in v }.map { (k, v) -> "$k: $v" })

    /** The magnet never says "camera". */
    @Test fun theMagnetNeverSaysCamera() = assertNone("magnet text that says camera without saying it isn't one",
        everyEnglishText().filter { (k, v) -> k.startsWith("magnet_") && Regex("camera", RegexOption.IGNORE_CASE).containsMatchIn(v) && !Regex("not a camera|isn\\\\'t a camera|never|doesn\\\\'t mean", RegexOption.IGNORE_CASE).containsMatchIn(v) }.map { (k, v) -> "$k: $v" })

    /** Sentence case: no Title Case Buttons, no ALL CAPS. */
    @Test fun sentenceCase() = assertNone("ALL CAPS text",
        everyEnglishText().filter { (k, v) -> k !in untranslatable && Regex("""\b[A-Z]{5,}\b""").containsMatchIn(v.replace(Regex("ONVIF|RTSP|HTTPS|IPCAM"), "")) }.map { (k, v) -> "$k: $v" })

    @Test fun sevenLanguages() = assertEquals(listOf("values-de", "values-es", "values-fr", "values-hi", "values-ja", "values-pt-rBR"), languages)

    @Test fun everyTranslationHasTheSamePlaceholders() {
        val bad = languages.flatMap { lang ->
            strings(lang).mapNotNull { (k, v) -> english[k]?.let { en -> if (placeholders(en) != placeholders(v)) "$lang/$k: en=${placeholders(en)} vs ${placeholders(v)}" else null } } +
                plurals(lang).flatMap { (k, items) ->
                    val en = englishPlurals[k]?.map(::placeholders)?.maxByOrNull { it.size } ?: return@flatMap emptyList()
                    // A language may leave the number out of its "one" form, never add or change a placeholder.
                    items.filter { !en.containsAll(placeholders(it)) || placeholders(it).isEmpty() && en.isNotEmpty() && items.size == 1 }.map { "$lang/$k: en=$en vs ${placeholders(it)}" }
                }
        }
        assertNone("placeholders differ from English", bad)
    }

    @Test fun everyEnglishStringIsTranslated() {
        val bad = languages.flatMap { lang ->
            val t = strings(lang); val p = plurals(lang)
            english.keys.filter { it !in t && it !in untranslatable }.map { "$lang/$it" } + englishPlurals.keys.filter { it !in p }.map { "$lang/$it (plural)" }
        }
        assertNone("missing translations", bad)
    }

    @Test fun noStrayTranslations() {
        val bad = languages.flatMap { lang -> strings(lang).keys.filter { it !in english || it in untranslatable }.map { "$lang/$it" } + plurals(lang).keys.filter { it !in englishPlurals }.map { "$lang/$it" } }
        assertNone("translation for a string English doesn't have (or one marked translatable=false)", bad)
    }

    @Test fun apostrophesAreEscaped() {
        val bad = (listOf("values") + languages).flatMap { lang ->
            (strings(lang).toList() + plurals(lang).flatMap { (k, v) -> v.map { k to it } })
                .filter { (_, v) -> Regex("""(?<!\\)'""").containsMatchIn(v) && !v.startsWith("\"") }.map { "$lang/${it.first}" }
        }
        assertNone("unescaped ' (aapt drops the text after it)", bad)
    }

    /** Every plural has the forms its language needs ("other" always; "one" where the language has it). */
    @Test fun pluralsHaveTheirForms() {
        val bad = (listOf("values") + languages).flatMap { lang ->
            pluralRe.findAll(text(lang)).mapNotNull { p ->
                val q = Regex("""quantity="([^"]+)"""").findAll(p.groupValues[2]).map { it.groupValues[1] }.toSet()
                val need = if (lang == "values-ja") setOf("other") else setOf("one", "other")
                if (q.containsAll(need)) null else "$lang/${p.groupValues[1]}: has $q, needs $need"
            }.toList()
        }
        assertNone("plural form missing", bad)
    }

    /** The check row's labels must fit 62 dp: keep them short in every language. */
    @Test fun checkLabelsAreShort() {
        val bad = (listOf("values") + languages).flatMap { lang -> strings(lang).filter { (k, v) -> k.startsWith("check_") && !k.startsWith("check_state") && k != "check_this_room" && v.length > 12 }.map { "$lang/${it.key}: ${it.value}" } }
        assertNone("check label longer than 12 letters", bad)
    }

    /** No string is left unused, and no code asks for one that isn't there. */
    @Test fun everyStringIsUsed() {
        val code = allKotlin.joinToString("\n") { it.readText() } + File(main, "AndroidManifest.xml").readText() + File(main, "res").walkTopDown().filter { it.extension == "xml" && it.name != "strings.xml" }.joinToString("\n") { it.readText() }
        val unused = (english.keys + englishPlurals.keys).filter { k -> !Regex("""R\.(string|plurals)\.$k\b|@string/$k\b""").containsMatchIn(code) }
        assertNone("string never used", unused)
    }

    private companion object {
        /** Strings that must use one of the words to say what the app does not claim ("This doesn't mean the room is safe"). */
        val WORDS_ALLOWED = setOf("report_not_safe_note", "welcome_honest_line", "limits_intro")
    }
}
