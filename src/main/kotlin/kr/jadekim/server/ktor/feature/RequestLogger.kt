package kr.jadekim.server.ktor.feature

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.http.Parameters
import io.ktor.request.*
import io.ktor.util.AttributeKey
import io.ktor.util.flattenEntries
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toMap
import kotlinx.coroutines.withContext
import kr.jadekim.logger.JLog
import kr.jadekim.logger.context.CoroutineLogContext
import kr.jadekim.server.ktor.canReadBody
import kr.jadekim.server.ktor.canReadableBody

class RequestLogFeature private constructor(
        private val serviceEnv: String?,
        private val release: String,
        private val logBody: ApplicationCall.() -> Boolean = { false },
        private val logContext: PipelineContext<Unit, ApplicationCall>.(CoroutineLogContext) -> Unit = {}
) {

    class Configuration {
        var serviceEnv: String? = null
        var release: String = "not_set"
        var logBody: ApplicationCall.() -> Boolean = { false }
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
                    configuration.logBody,
                    configuration.logContext
            )

            pipeline.intercept(ApplicationCallPipeline.Call) {
                val method = context.request.httpMethod

                val logContext = coroutineContext[CoroutineLogContext] ?: CoroutineLogContext()

                val preHandleTime = System.currentTimeMillis()
                logContext["preHandleTime"] = preHandleTime
                logContext["serviceEnv"] = feature.serviceEnv ?: "not_set"
                logContext["deployVersion"] = feature.release
                logContext["remoteAddress"] = context.request.host()
                logContext["userAgent"] = context.request.userAgent()
                logContext["headers"] = context.request.headers.toMap()
                logContext["method"] = method
                logContext["pathParameter"] = context.parameters.toLogString()
                logContext["query"] = context.request.queryParameters.toLogString()

                feature.logContext(this@intercept, logContext)

                withContext(logContext) {
                    try {
                        proceed()
                    } finally {
                        val status = context.response.status()?.value?.toString()

                        logContext["durationToHandle"] = "${System.currentTimeMillis() - preHandleTime}"
                        logContext["request"] = context.request.path()
                        logContext["status"] = status

                        if (method.canReadBody
                                && context.request.contentType().canReadableBody
                                && feature.logBody(context)) {
                            logContext["body"] = context.receiveText()
                        }

                        val request = context.attributes.getOrNull(PATH)
                                ?: "${context.request.path()}/(method:${method.value})"

                        logger.sInfo("$request - $status")
                    }
                }
            }

            return feature
        }
    }
}

private fun Parameters.toLogString() = flattenEntries().joinToString { "${it.first} = ${it.second}" }