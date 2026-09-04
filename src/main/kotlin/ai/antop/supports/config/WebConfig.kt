package ai.antop.supports.config

import ai.antop.supports.web.AdminAuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val adminAuthInterceptor: AdminAuthInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/admin/**")
            .excludePathPatterns("/admin/login", "/admin/logout")
    }
}
