package kr.jadekim.server.ktor

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.request.acceptLanguage
import io.ktor.request.receive
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toMap
import kr.jadekim.common.apiserver.exception.MissingParameterException
import kr.jadekim.common.apiserver.protocol.ApiResponse
import java.util.*
import kotlin.reflect.KClass

var jsonBodyMapper = jacksonObjectMapper()

inline val PipelineContext<*, ApplicationCall>.pathParam: Parameters get() = context.parameters

inline val PipelineContext<*, ApplicationCall>.queryParam: Parameters get() = context.request.queryParameters

suspend fun PipelineContext<*, ApplicationCall>.bodyParam(): Parameters? {
    var cache = context.attributes.getOrNull(RECEIVED_PARAMETERS)

    if (cache == null) {
        context.receiveOrNull<Parameters>()?.let {
            context.attributes.put(RECEIVED_PARAMETERS, it)
            cache = it
        }
    }

    return cache
}

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

suspend fun PipelineContext<*, ApplicationCall>.jsonBody(): JsonNode {
    var cache = context.attributes.getOrNull(RECEIVED_BODY)

    if (cache == null) {
        context.receive<JsonNode>().let {
            context.attributes.put(RECEIVED_BODY, it)
            cache = it
        }
    }

    return cache!!
}

suspend fun PipelineContext<*, ApplicationCall>.json(key: String): JsonNode? = jsonBody()[key]

suspend fun PipelineContext<*, ApplicationCall>.jsonStringSafe(key: String, default: String? = null): String? {
    return json(key)?.textValue() ?: default
}

suspend fun PipelineContext<*, ApplicationCall>.jsonString(key: String, default: String? = null): String {
    return jsonStringSafe(key, default) ?: throw MissingParameterException("required $key")
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T : Any> PipelineContext<*, ApplicationCall>.jsonBody(clazz: KClass<T>): T {
    return try {
        jsonBodyMapper.treeToValue(jsonBody(), clazz.java)
    } catch (e: JsonMappingException) {
        throw MissingParameterException("Require ${e.pathReference}", e)
    }
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

val PipelineContext<*, ApplicationCall>.locale: Locale
    get() {
        val acceptLanguage = context.request.acceptLanguage()

        return if (acceptLanguage == null || acceptLanguage.contains("KR", ignoreCase = true)) {
            Locale.KOREA
        } else {
            Locale.ENGLISH
        }
    }