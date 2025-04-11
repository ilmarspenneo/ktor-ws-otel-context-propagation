package com.example

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.opentelemetry.api.trace.Span
import io.opentelemetry.extension.kotlin.getOpenTelemetryContext
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

suspend fun doSomething() = withSpan("doing something...") {
    val logger = LoggerFactory.getLogger("dosomething")
    val ctx = Span.current().spanContext
    logger.info("Within something: ${ctx.traceId} ${ctx.spanId} ${ctx.traceState}")
    delay(50)
    moreStuff()
}

suspend fun moreStuff() = withSpan("more stuff...") {
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
        webSocket("/ws") {
            val otelCtx = call.coroutineContext.getOpenTelemetryContext()
            val logger = LoggerFactory.getLogger("dosomething")

            otelCtx.makeCurrent().use {
                logger.warn("initial: ${Span.current().spanContext.traceId}")

                outgoing.send(Frame.Text("Hello ${Span.current().spanContext.traceId}"))

                for (frame in incoming) {
                    call.coroutineContext.getOpenTelemetryContext().makeCurrent().use {
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
    }
}
