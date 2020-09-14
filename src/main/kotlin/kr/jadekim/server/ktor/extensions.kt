package kr.jadekim.server.ktor

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.request.acceptLanguage
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toMap
import kr.jadekim.common.apiserver.exception.MissingParameterException
import kr.jadekim.common.apiserver.protocol.ApiResponse
import java.util.*

inline val PipelineContext<*, ApplicationCall>.pathParam: Parameters get() = context.parameters

inline val PipelineContext<*, ApplicationCall>.queryParam: Parameters get() = context.request.queryParameters

suspend fun PipelineContext<*, ApplicationCall>.bodyParam(): Parameters? = context.receiveOrNull()

fun PipelineContext<*, ApplicationCall>.pathParamSafe(key: String, default: String? = null): String? {
    return pathParam[key] ?: default
}

fun PipelineContext<*, ApplicationCall>.pathParam(key: String, default: String? = null): String {
    return pathParamSafe(key, default) ?: throw MissingParameterException("required $key")
}

fun PipelineContext<*, ApplicationCall>.queryParamSafe(key: String, default: String? = null): String? {
    return queryParam[key] ?: default
}

fun PipelineContext<*, ApplicationCall>.queryParam(key: String, default: String? = null): String {
    return queryParamSafe(key, default) ?: throw MissingParameterException("required $key")
}

suspend fun PipelineContext<*, ApplicationCall>.bodyParamListSafe(key: String): List<String> {
    return bodyParam()?.getAll(key) ?: emptyList()
}

suspend fun PipelineContext<*, ApplicationCall>.bodyParamList(key: String): List<String> {
    val result = bodyParamListSafe(key)

    if (result.isEmpty()) {
        throw MissingParameterException("required $key")
    }

    return result
}

suspend fun PipelineContext<*, ApplicationCall>.bodyParamSafe(key: String, default: String? = null): String? {
    return bodyParam()?.get(key) ?: default
}

suspend fun PipelineContext<*, ApplicationCall>.bodyParam(key: String, default: String? = null): String {
    return bodyParamSafe(key, default) ?: throw MissingParameterException("required $key")
}

suspend fun PipelineContext<*, ApplicationCall>.response(value: Any? = null) {
    context.respond(ApiResponse(data = value))
}

fun Parameters?.toSingleValueMap(): Map<String, String> {
    return this?.toMap()
            ?.mapValues { it.value.firstOrNull() }
            ?.filterValues { !it.isNullOrBlank() }
            ?.mapValues { it.value!! }
            ?: emptyMap()
}

val HttpMethod.canReadBody
    get() = when (this) {
        HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch -> true
        else -> false
    }

val ContentType.canReadableBody
    get() = when (this) {
        ContentType.Application.Json, ContentType.Application.FormUrlEncoded -> true
        else -> false
    }

val PipelineContext<*, ApplicationCall>.locale: Locale
    get() {
        val acceptLanguage = context.request.acceptLanguage()

        return if (acceptLanguage == null || acceptLanguage.contains("KR", ignoreCase = true)) {
            Locale.KOREA
        } else {
            Locale.ENGLISH
        }
    }