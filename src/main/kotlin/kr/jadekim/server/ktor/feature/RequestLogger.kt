package kr.jadekim.server.ktor.feature

import io.ktor.application.*
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.http.ContentType
import io.ktor.request.*
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toMap
import kotlinx.coroutines.withContext
import kr.jadekim.common.apiserver.enumuration.Environment
import kr.jadekim.logger.JLog
import kr.jadekim.logger.context.CoroutineLogContext
import kr.jadekim.server.ktor.*

class RequestLogFeature private constructor(
    private val serviceEnv: Environment?,
    private val release: String,
    private val filterParameters: List<String> = emptyList(),
    private val logContext: PipelineContext<Unit, ApplicationCall>.(CoroutineLogContext) -> Unit = {}
) {

    class Configuration {
        var serviceEnv: Environment? = null
        var release: String = "not_set"
        var filterParameters: List<String> = emptyList()
        var logContext: PipelineContext<Unit, ApplicationCall>.(CoroutineLogContext) -> Unit = {}
    }

    companion object Feature : ApplicationFeature<Application, Configuration, RequestLogFeature> {

        override val key: AttributeKey<RequestLogFeature> = AttributeKey("RequestLogFeature")

        private val logger = JLog.get("RequestLogger")

        override fun install(
            pipeline: Application,
            configure: Configuration.() -> Unit
        ): RequestLogFeature {
            val configuration = Configuration().apply(configure)
            val feature = RequestLogFeature(
                configuration.serviceEnv,
                configuration.release,
                configuration.filterParameters,
                configuration.logContext
            )

            pipeline.intercept(ApplicationCallPipeline.Call) {
                val method = call.request.httpMethod

                val logContext = coroutineContext[CoroutineLogContext] ?: CoroutineLogContext()

                val preHandleTime = System.currentTimeMillis()
                logContext["preHandleTime"] = preHandleTime
                logContext["serviceEnv"] = feature.serviceEnv?.name ?: "not_set"
                logContext["deployVersion"] = feature.release
                logContext["remoteAddress"] = call.request.host()
                logContext["userAgent"] = call.request.userAgent()
                logContext["headers"] = call.request.headers.toMap()
                logContext["method"] = method

                val parameters = (pathParam.toMap() + queryParam.toMap()).toMutableMap()

                if (method.canReadBody) {
                    when (call.request.contentType()) {
                        ContentType.Application.FormUrlEncoded -> {
                            try {
                                withContext(logContext) {
                                    bodyParam()?.toMap()?.let {
                                        parameters.putAll(it)
                                    }
                                }
                            } catch (e: UnsupportedMediaTypeException) {
                                //do nothing
                            }
                        }
                        ContentType.Application.Json -> {
                            jsonBody().fields().forEach {
                                parameters[it.key] = listOf(it.value.asText())
                            }
                        }
                    }
                }

                logContext["parameters"] = parameters.filter { it.key !in feature.filterParameters }

                feature.logContext(this@intercept, logContext)

                withContext(logContext) {
                    proceed()
                }

                val status = call.response.status()?.value?.toString()

                logContext["durationToHandle"] = "${System.currentTimeMillis() - preHandleTime}"
                logContext["request"] = context.request.path()
                logContext["status"] = status

                val request = call.attributes.getOrNull(PATH)
                    ?: "${context.request.path()}/(method:${method.value})"

                logger.sInfo("$request - $status")
            }

            return feature
        }
    }
}