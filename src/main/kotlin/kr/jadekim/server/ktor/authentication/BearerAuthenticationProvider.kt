package kr.jadekim.server.ktor.authentication

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.request.ApplicationRequest
import kr.jadekim.common.apiserver.exception.UnauthorizedException
import kr.jadekim.logger.context.CoroutineLogContext

data class BearerCredential(val token: String) : Credential

class BearerAuthenticationProvider internal constructor(
    configuration: Configuration
) : AuthenticationProvider(configuration) {

    internal val authenticationFunction = configuration.authenticationFunction

    class Configuration internal constructor(name: String?) : AuthenticationProvider.Configuration(name) {

        internal var authenticationFunction: AuthenticationFunction<BearerCredential> = { null }

        fun validate(body: suspend ApplicationCall.(BearerCredential) -> Principal?) {
            authenticationFunction = body
        }
    }
}

fun Authentication.Configuration.bearer(
    name: String? = null,
    configure: BearerAuthenticationProvider.Configuration.() -> Unit
) {
    val provider = BearerAuthenticationProvider(BearerAuthenticationProvider.Configuration(name).apply(configure))
    val authenticate = provider.authenticationFunction

    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val credentials = call.request.bearerAuthenticationCredentials()
        val principal = credentials?.let { authenticate(call, it) }

        val cause = when {
            credentials == null -> AuthenticationFailedCause.NoCredentials
            principal == null -> AuthenticationFailedCause.InvalidCredentials
            else -> null
        }

        if (cause != null) {
            context.challenge(name ?: "BearerToken", cause) {
                throw UnauthorizedException(credentials?.token ?: "")
            }
        }

        if (principal != null) {
            context.principal(principal)
            coroutineContext[CoroutineLogContext]?.set("principal", principal)
        }
    }

    register(provider)
}

fun ApplicationRequest.bearerAuthenticationCredentials(): BearerCredential? {
    when (val authHeader = parseAuthorizationHeader()) {
        is HttpAuthHeader.Single -> {
            if (!authHeader.authScheme.equals("Bearer", ignoreCase = true)) {
                return null
            }

            return BearerCredential(authHeader.blob)
        }
        else -> return null
    }
}