package com.example
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span

inline fun <T> withSpan(
    name: String,
    attributes: Map<String, Any> = emptyMap(),
    block: () -> T
): T {
    val tracer = GlobalOpenTelemetry.getTracer("tracer")
    val span: Span = tracer.spanBuilder(name).startSpan()
    attributes.forEach { (key, value) ->
        span.setAttribute(key, value.toString())
    }

    return try {
        block()
    } catch (e: Exception) {
        span.recordException(e)
        throw e
    } finally {
        span.end()
    }
}
