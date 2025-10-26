package im.bigs.pg.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

/**
 * RestTemplate 빈 설정.
 *
 * 외부 PG API 호출을 위해 RestTemplate을 생성합니다.
 */
@Configuration
class RestTemplateConfig {
    /**
     * RestTemplate 빈을 생성합니다.
     *
     * @return RestTemplate 인스턴스
     */
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}