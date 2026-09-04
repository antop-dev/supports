package ai.antop.supports.config

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.lang.reflect.Method
import java.util.concurrent.Executor

@Configuration
class AsyncConfig : AsyncConfigurer {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean("mailExecutor")
    override fun getAsyncExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 5
            queueCapacity = 100
            setThreadNamePrefix("async-mail-")
            initialize()
        }

    @Bean("slackExecutor")
    fun slackExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 1
            maxPoolSize = 2
            queueCapacity = 100
            setThreadNamePrefix("async-slack-")
            initialize()
        }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler =
        AsyncUncaughtExceptionHandler { ex: Throwable, method: Method, _: Array<out Any?> ->
            log.error("Async task failed: {}", method.name, ex)
        }
}
