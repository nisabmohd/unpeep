package app.keyhole.privacy.data

import android.content.Context
import app.keyhole.privacy.domain.BleSignatures
import app.keyhole.privacy.domain.HotspotSignature
import app.keyhole.privacy.domain.PortKind
import app.keyhole.privacy.domain.Signatures
import app.keyhole.privacy.domain.TrackerSignature
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reads the bundled lists in assets/signatures (format: community-signatures/README.md). There are no online
 * updates: a new list ships with the next version. Loaded on the first scan, not at start.
 */
class SignatureLoader(private val read: (String) -> String) {
    constructor(context: Context) : this({ name -> context.assets.open("signatures/$name").bufferedReader().use { it.readText() } })

    val signatures: Signatures by lazy {
        Signatures(
            cameras = parseCameras(read("cameras.json")),
            known = parseKnown(read("known.json")),
            ports = parsePorts(read("ports.json")),
        )
    }
    val hotspots: List<HotspotSignature> by lazy { parseHotspots(read("hotspots.json")) }
    val ble: BleSignatures by lazy { parseBle(read("ble.json")) }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        @Serializable private data class CameraJson(val maker: String, val words: List<String>)
        @Serializable private data class KnownJson(val kind: String, val words: List<String> = emptyList(), val services: List<String> = emptyList())
        @Serializable private data class PortJson(val port: Int, val kind: String)
        @Serializable private data class HotspotJson(val pattern: String, val maker: String, val weak: Boolean = false)
        @Serializable private data class TrackerJson(val name: String, val company: Int? = null, val type: Int? = null, val services: List<String> = emptyList())
        @Serializable private data class BleJson(val trackers: List<TrackerJson> = emptyList(), val cameras: List<CameraJson> = emptyList())

        fun parseCameras(text: String): List<Signatures.CameraSignature> =
            json.decodeFromString<List<CameraJson>>(text).map { Signatures.CameraSignature(it.maker, it.words.map(String::lowercase)) }

        fun parseKnown(text: String): List<Signatures.KnownSignature> =
            json.decodeFromString<List<KnownJson>>(text).map { Signatures.KnownSignature(it.kind, it.words.map(String::lowercase), it.services) }

        fun parsePorts(text: String): Map<Int, PortKind> =
            json.decodeFromString<List<PortJson>>(text).associate { it.port to (PortKind.entries.firstOrNull { k -> k.name.equals(it.kind, true) } ?: PortKind.OTHER) }

        fun parseHotspots(text: String): List<HotspotSignature> =
            json.decodeFromString<List<HotspotJson>>(text).map { HotspotSignature(Regex(it.pattern, RegexOption.IGNORE_CASE), it.maker, it.weak) }

        fun parseBle(text: String): BleSignatures = json.decodeFromString<BleJson>(text).let { b ->
            BleSignatures(
                b.trackers.map { TrackerSignature(it.name, it.company, it.type, it.services.map(String::uppercase)) },
                b.cameras.map { Signatures.CameraSignature(it.maker, it.words.map(String::lowercase)) },
            )
        }
    }
}
