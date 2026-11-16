package app.keyhole.privacy.scan.net

/**
 * What the scanner says to a device and how it reads the answer (docs/SCANNING.md). Pure text in, facts out, so
 * every rule here has a unit test. It only ever asks what anyone on the Wi-Fi may ask: discovery, a plain
 * `OPTIONS` and a plain `GET /`. Nothing here logs in, and nothing asks for a stream.
 */
object Banners {
    /** SSDP and WS-Discovery share one multicast group. */
    const val GROUP = "239.255.255.250"
    const val SSDP_PORT = 1900
    const val ONVIF_PORT = 3702

    /** "Who is here?" in UPnP. */
    const val SSDP_SEARCH = "M-SEARCH * HTTP/1.1\r\nHOST: $GROUP:$SSDP_PORT\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: ssdp:all\r\n\r\n"

    /** The ONVIF probe: only network video devices answer it (printers answer a plain WS-Discovery probe too). */
    fun onvifProbe(messageId: String): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<e:Envelope xmlns:e=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:w=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" " +
            "xmlns:d=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\" xmlns:dn=\"http://www.onvif.org/ver10/network/wsdl\">" +
            "<e:Header><w:MessageID>uuid:$messageId</w:MessageID><w:To e:mustUnderstand=\"true\">urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To>" +
            "<w:Action e:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action></e:Header>" +
            "<e:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></e:Body></e:Envelope>"

    /** "What can you do?" in RTSP. It names no stream and starts none. */
    fun rtspOptions(ip: String, port: Int): String = "OPTIONS rtsp://$ip:$port/ RTSP/1.0\r\nCSeq: 1\r\nUser-Agent: Keyhole\r\n\r\n"

    /** A plain page request, as a browser's first line. */
    fun httpGet(host: String, path: String = "/"): String = "GET $path HTTP/1.0\r\nHost: $host\r\nUser-Agent: Keyhole\r\nAccept: */*\r\nConnection: close\r\n\r\n"

    /** The value of header [name] in a reply's head, or null. */
    fun header(reply: String, name: String): String? {
        val head = reply.substringBefore("\r\n\r\n").substringBefore("\n\n")
        for (line in head.lineSequence().drop(1)) {
            val colon = line.indexOf(':')
            if (colon > 0 && line.substring(0, colon).trim().equals(name, ignoreCase = true)) return line.substring(colon + 1).trim().ifEmpty { null }
        }
        return null
    }

    data class Rtsp(val server: String?)

    /** Any RTSP status line counts, 401 included: the device speaks RTSP, which is all Keyhole asks. */
    fun rtsp(reply: String): Rtsp? = if (reply.startsWith("RTSP/")) Rtsp(header(reply, "Server")) else null

    data class Http(val server: String?, val title: String?)

    private val titleRe = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    fun http(reply: String): Http? {
        if (!reply.startsWith("HTTP/")) return null
        val title = titleRe.find(reply)?.groupValues?.get(1)?.let(::clean)
        return Http(header(reply, "Server"), title)
    }

    /** Where a device says its description is. Only `http://<IPv4>[:port]/path`: a host name would mean a lookup. */
    data class Target(val ip: String, val port: Int, val path: String)

    fun target(location: String): Target? {
        val rest = location.trim().takeIf { it.startsWith("http://", ignoreCase = true) }?.substring(7) ?: return null
        val hostPort = rest.substringBefore('/')
        val path = "/" + rest.substringAfter('/', "")
        val ip = hostPort.substringBefore(':')
        if (LocalOnly.parseIpv4(ip) == null) return null
        val port = if (':' in hostPort) hostPort.substringAfter(':').toIntOrNull()?.takeIf { it in 1..65535 } ?: return null else 80
        return Target(ip, port, path)
    }

    data class Upnp(val name: String?, val manufacturer: String?, val model: String?)

    /** The three lines of a UPnP device description Keyhole reads. */
    fun upnp(xml: String): Upnp = Upnp(tag(xml, "friendlyName"), tag(xml, "manufacturer"), tag(xml, "modelName"))

    private fun tag(xml: String, name: String): String? =
        Regex("<$name[^>]*>(.*?)</$name>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(xml)?.groupValues?.get(1)?.let(::clean)

    /** True when a WS-Discovery reply comes from a network video device. */
    fun isOnvif(reply: String): Boolean = reply.contains("NetworkVideoTransmitter", ignoreCase = true) || reply.contains("onvif.org", ignoreCase = true)

    private val scopeNameRe = Regex("onvif://www\\.onvif\\.org/name/([^\\s<]+)", RegexOption.IGNORE_CASE)

    /** The name an ONVIF device gives itself in its scopes, if any. */
    fun onvifName(reply: String): String? = scopeNameRe.find(reply)?.groupValues?.get(1)?.let(::percentDecode)?.let(::clean)

    private fun percentDecode(s: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val hex = if (c == '%' && i + 2 < s.length) s.substring(i + 1, i + 3).toIntOrNull(16) else null
            if (hex != null) {
                out.append(hex.toChar())
                i += 3
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    /** One tidy line: no tags' entities left raw, no control characters, never longer than a title should be. */
    private fun clean(s: String): String? =
        s.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
            .replace(Regex("[\\p{Cntrl}\\s]+"), " ").trim().take(MAX_TEXT).ifEmpty { null }

    private const val MAX_TEXT = 80
}
