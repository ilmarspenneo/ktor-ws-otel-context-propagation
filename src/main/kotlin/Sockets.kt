package com.example

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

suspend fun doSomething() = withSpanSuspend {
    val logger = LoggerFactory.getLogger("dosomething")
    val ctx = Span.current().spanContext
    logger.info("Within something: ${ctx.traceId} ${ctx.spanId} ${ctx.traceState}")
    delay(50)
    moreStuff()
}

suspend fun moreStuff() = withSpanSuspend {
    val logger = LoggerFactory.getLogger("dosomething")
    val ctx = Span.current().spanContext
    logger.info("Within something: ${ctx.traceId} ${ctx.spanId} ${ctx.traceState}")
    delay(50)
}

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocketWithOtel("/ws") {
            outgoing.send(Frame.Text("Hello ${Span.current().spanContext.traceId}"))

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    outgoing.send(Frame.Text("YOU SAID: $text ${Span.current().spanContext.traceId}"))

                    doSomething()

                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                }
            }
        }
    }
}
