import ch.qos.logback.core.util.Duration
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

interface WebSocketSession {
    val incoming: ReceiveChannel<Frame>
    val outgoing: SendChannel<Frame>
    fun close(reason: CloseReason)

    suspend fun send(frame: Frame)

    val call: ApplicationCall
    val application: Application

    var pingInterval: Duration?
    var timeout: Duration
    var masking: Boolean
    var maxFrameSize: Long

    val closeReason: Deferred<CloseReason>
    suspend fun flush()

    fun terminate()
}
