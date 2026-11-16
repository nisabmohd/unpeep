package app.unpeep.privacy.scan.net

/** The addresses a Wi-Fi scan visits (docs/SCANNING.md § Wi-Fi): the /24 around the phone's own address. */
object Subnet {
    /** `.1` to `.254` of [ip]'s /24, without [ip] itself. Empty when [ip] isn't a dotted IPv4 address. */
    fun hosts(ip: String): List<String> {
        if (LocalOnly.parseIpv4(ip) == null) return emptyList()
        val base = ip.substringBeforeLast('.')
        return (1..254).map { "$base.$it" }.filter { it != ip }
    }
}
