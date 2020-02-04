package kr.jadekim.server.ktor

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.Parameters
import io.ktor.routing.Route
import io.ktor.util.AttributeKey
import kr.jadekim.logger.context.CoroutineLogContext

val RECEIVED_PARAMETERS = AttributeKey<Parameters>("received_parameters")
val RECEIVED_BODY = AttributeKey<JsonNode>("received_body")
val ROUTE = AttributeKey<Route>("route")
val PATH = AttributeKey<String>("path")