package com.example

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.*
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.util.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import java.util.logging.Logger

val TraceContextKey = AttributeKey<Context>("TraceContext")

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    configureSockets()
    configureRouting()

    install(
        createApplicationPlugin(name = "Log trace ID and request") {
            onCallRespond { call ->
                val ctx = Span.current().spanContext
                val logger = Logger.getLogger("ResponseLogger")

                logger.info("Outside of websocket handler: ${call.request.httpMethod.value} ${call.request.uri}: ${ctx.traceId}")
            }
        },
    )

    install(KtorServerTelemetry) {
        setOpenTelemetry(AutoConfiguredOpenTelemetrySdk.initialize().openTelemetrySdk)

        knownMethods(HttpMethod.DefaultMethods)
        capturedRequestHeaders(HttpHeaders.UserAgent)
        capturedResponseHeaders(HttpHeaders.ContentType)
    }

    intercept(ApplicationCallPipeline.Plugins) {
        val ctx = Context.current()
        call.attributes.put(TraceContextKey, ctx)
        proceed()
    }
}
