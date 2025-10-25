package im.bigs.pg.api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger UI를 위한 OpenAPI 설정.
 *
 * API 문서 경로:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
class OpenApiConfig {

    /**
     * OpenAPI 문서 설정을 생성합니다.
     *
     * @return OpenAPI 설정 객체
     */
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("결제 API")
                    .description("결제 생성 및 조회")
            )
    }
}
