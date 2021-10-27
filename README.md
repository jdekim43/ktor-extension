# ktor-extension
Moved to https://github.com/jdekim43/jext

* Extension functions
    * pathParam(key: String)
    * queryParam(key: String)
    * bodyParam (form-urlencoded)
    * jsonBody(): JsonNode
        * json(key: String): JsonNode?
        * jsonString(key: String)
    * response : Wrap ApiResponse class
* PathNameNormalizeFeature
    * Route.toString() 을 attributes 에 저장
* RequestLogFeature
* BearerAuthenticationProvider

## Install
### Gradle Project
1. Add dependency
    ```
    build.gradle.kts
   
    implementation("kr.jadekim:ktor-extension:1.0.0")
    ```
