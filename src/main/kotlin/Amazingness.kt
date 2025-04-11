package com.example

import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.extension.kotlin.getOpenTelemetryContext
import kotlinx.coroutines.withContext

inline fun <Result> withSpan(
    name: String = getDefaultSpanName(),
    crossinline block: () -> Result
): Result {
    val span = createSpan(name)

    return try {
        block()
    } catch (e: Exception) {
        span.setStatus(StatusCode.ERROR)
        span.recordException(e)
        throw e
    } finally {
        span.end()
    }
}

suspend inline fun <Result> withSpanSuspend(
    name: String = getDefaultSpanName(),
    crossinline block: suspend () -> Result
): Result {
    val span: Span = createSpan(name)

    return try {
        block()
    } catch (e: Exception) {
        span.setStatus(StatusCode.ERROR)
        span.recordException(e)
        throw e
    } finally {
        span.end()
    }
}

fun createSpan(name: String): Span {
    val tracer = GlobalOpenTelemetry.getTracer(object {}.javaClass.packageName)
    val span: Span = tracer.spanBuilder(name).startSpan()
    return span
}

// inlining to remove it from the call stack
@Suppress("NOTHING_TO_INLINE")
inline fun getDefaultSpanName(): String {
    val callingStackFrame = Thread.currentThread().stackTrace[1]

    val simpleClassName = Class.forName(callingStackFrame.className).simpleName
    val methodName = callingStackFrame.methodName

    return "$simpleClassName.$methodName"
}

fun Route.webSocketWithOtel(path: String, handler: suspend DefaultWebSocketServerSession.() -> Unit) {
    webSocket(path) {
        val otelContext = call.coroutineContext.getOpenTelemetryContext()

        withContext(coroutineContext + otelContext.asContextElement()) {
            handler()
        }
    }
}

