package ken.ktorapp.first

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.routing
import io.ktor.sessions.*
import io.ktor.util.generateNonce
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import kotlinx.css.Cursor
import java.time.Duration

fun Application.main() {
    ChatApplication().apply { main() }
}

class ChatApplication {
    private val server = ChatServer()

    fun Application.main() {
        install(DefaultHeaders)
        install(CallLogging)
        install(WebSockets) {
            pingPeriod = Duration.ofMillis(1)
        }
        install(Sessions) {
            cookie<ChatSession>("SESSION")
        }

        intercept(ApplicationCallPipeline.Features) {
            if ( call.sessions.get<ChatSession>() == null ) {
                call.sessions.set(ChatSession(generateNonce()))
            }
        }
        routing {
            webSocket("/ws") {
                val session = call.sessions.get<ChatSession>()

                if ( session == null ) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No Session"))
                    return@webSocket
                }

                server.memberJoin(session.id, this)

                try {
                    incoming.consumeEach { frame ->
                        if ( frame is Frame.Text ) {
                            receiveMessage(session.id, frame.readText())
                        }
                    }
                } finally {
                    defaultResource("index.html", "web")
                    resource("web")
                }
            }

            static {
                defaultResource("index.html", "web")
                resources("web")
            }
        }
    }

    data class ChatSession(val id: String)

    private suspend fun receiveMessage(id: String, command: String) {
        when {
            command.startsWith("/who") -> server.who(id)

            command.startsWith("/user") -> {
                val newName = command.removePrefix("/user").trim()

                when {
                    newName.isEmpty() -> server.sendTo(id, "server::help", "/user [newName]")
                    newName.length > 50 -> server.sendTo(
                        id,
                        "server::help",
                        "new name is too long: 50 characters limit"
                    )
                    else -> server.memberRenamed(id, newName)
                }
            }

            command.startsWith("/help") -> server.help(id)
            command.startsWith("/") -> server.sendTo(
                id,
                "server::help",
                "Unknown command ${command.takeWhile { !it.isWhitespace() }}"
            )

            else -> server.message(id, command)
        }
    }
}
