package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

@WithSpan(kind = SpanKind.SERVER)
suspend fun doSomethingElse() {
    val logger = LoggerFactory.getLogger("dosomething")
    val ctx = Span.current().spanContext
    logger.info("/ Within something: ${ctx.traceId} ${ctx.spanId} ${ctx.traceState}")
    delay(100);
}

fun Application.configureRouting() {
    routing {
        get("/") {
            val logger = LoggerFactory.getLogger("adosomething")
            val ctx = Span.current().spanContext
            logger.info("/ Within /: ${ctx.traceId} ${ctx.spanId} ${ctx.traceState}")
            doSomethingElse()
            call.respondText("Hello World!")
        }
    }
}
