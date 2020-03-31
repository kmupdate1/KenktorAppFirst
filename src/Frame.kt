import io.ktor.http.cio.websocket.CloseReason
import kotlinx.coroutines.DisposableHandle
import java.nio.ByteBuffer

enum class FrameType { TEXT, BINARY, CLOSE, PING, PONG}

sealed class Frame {
    val fin: Boolean
    val frameType: FrameType
    val buffer: ByteBuffer
    val disposableHandle: DisposableHandle

    class Binary : Frame
    class Text : Frame {
        fun readText(): String
    }
    class Close : Frame {
        fun readReason(): CloseReason?
    }
    class Ping : Frame
    class Pong : Frame
}
