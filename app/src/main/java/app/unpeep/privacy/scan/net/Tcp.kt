package app.unpeep.privacy.scan.net

import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket

/** What a port said when knocked on. */
enum class PortState {
    /** Something is listening. */
    OPEN,
    /** The device answered "nothing here": it is there, the port is not. */
    CLOSED,
    /** No answer in time: no device, or one that drops what it doesn't expect. */
    SILENT,
}

/** The scanner's TCP: a knock, and one question with one answer. Every connection goes through [LocalOnly]. */
object Tcp {
    fun probe(address: InetAddress, port: Int, timeoutMs: Int, prepare: (Socket) -> Unit = {}): PortState = try {
        LocalOnly.connect(address, port, timeoutMs, prepare).close()
        PortState.OPEN
    } catch (e: ConnectException) {
        if (e.message.orEmpty().contains("refused", ignoreCase = true)) PortState.CLOSED else PortState.SILENT
    } catch (e: IOException) {
        PortState.SILENT
    }

    /**
     * Sends [request] and returns what came back, read until [enough] says so, the device hangs up, [max] bytes or
     * [readMs] of silence. Null when the device didn't answer at all.
     */
    fun ask(address: InetAddress, port: Int, request: String, connectMs: Int, readMs: Int, max: Int = MAX_REPLY, prepare: (Socket) -> Unit = {}, enough: (String) -> Boolean): String? = try {
        LocalOnly.connect(address, port, connectMs, prepare).use { socket ->
            socket.soTimeout = readMs
            socket.getOutputStream().apply { write(request.toByteArray(Charsets.ISO_8859_1)); flush() }
            val input = socket.getInputStream()
            val buffer = ByteArray(BLOCK)
            val text = StringBuilder()
            try {
                while (text.length < max) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    text.append(String(buffer, 0, n, Charsets.ISO_8859_1))
                    if (enough(text.toString())) break
                }
            } catch (e: IOException) {
                // Silence after a partial answer: keep what came.
            }
            text.toString().ifEmpty { null }
        }
    } catch (e: IOException) {
        null
    }

    private const val MAX_REPLY = 16 * 1024
    private const val BLOCK = 2048
}
