package kr.jadekim.server.ktor.feature

import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.routing.Routing
import io.ktor.util.AttributeKey
import kr.jadekim.logger.integration.REQUEST_LOG_CONTEXT
import kr.jadekim.server.ktor.PATH
import kr.jadekim.server.ktor.ROUTE

class PathNormalizeFeature private constructor() {

    class Configuration {

    }

    companion object Feature : ApplicationFeature<Application, Configuration, PathNormalizeFeature> {
        override val key: AttributeKey<PathNormalizeFeature> = AttributeKey("PathNormalizeFeature")

        override fun install(
            pipeline: Application,
            configure: Configuration.() -> Unit
        ): PathNormalizeFeature {
            val feature = PathNormalizeFeature()

            pipeline.environment.monitor.subscribe(Routing.RoutingCallStarted) {
                val route = it.route.toString()

                it.attributes.put(ROUTE, it.route)
                it.attributes.put(PATH, route)
                it.attributes.getOrNull(REQUEST_LOG_CONTEXT)?.set("route", route)
            }

            return feature
        }
    }
}