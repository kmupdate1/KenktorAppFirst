package ken.ktorapp.first

import Frame.Text
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.css.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.ClosedReceiveChannelException

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }

            application.install(WebSockets)

            val received = arrayListOf<String>()
            application.routing {
                webSocket("/echo") {
                    try {
                        while ( true ) {
                            val text = (incoming.receive() as Text).readText()
                            received += text
                            outgoing.send(Text(text))
                        }
                    } catch ( e: ClosedReceiveChannelException) {
                        call.respondText { e.printStackTrace().toString() }
                    } catch ( e: Throwable ) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
