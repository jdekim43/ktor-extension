package kr.jadekim.server.ktor.converter

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.ContentConverter
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.request.contentCharset
import io.ktor.util.pipeline.PipelineContext
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kr.jadekim.common.apiserver.exception.MissingParameterException

class JacksonConverter(private val objectmapper: ObjectMapper = jacksonObjectMapper()) : ContentConverter {

    override suspend fun convertForSend(
            context: PipelineContext<Any, ApplicationCall>,
            contentType: ContentType,
            value: Any
    ): Any? {
        @Suppress("BlockingMethodInNonBlockingContext")
        return TextContent(
                objectmapper.writeValueAsString(value),
                ContentType.Application.Json.withCharset(Charsets.UTF_8)
        )
    }

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val type = request.type
        val value = request.value as? ByteReadChannel ?: return null

        return value.toInputStream()
                .reader(context.call.request.contentCharset() ?: Charsets.UTF_8)
                .use {
                    try {
                        objectmapper.readValue(it, type.javaObjectType)
                    } catch (e: MismatchedInputException) {
                        objectmapper.createObjectNode()
                    } catch (e: JsonMappingException) {
                        throw MissingParameterException("Require ${e.pathReference}", e)
                    }
                }
    }
}