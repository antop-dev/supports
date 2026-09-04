package ai.antop.supports.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.Locale

@Configuration
class LocaleConfig {
    companion object {
        val SUPPORTED: List<Locale> =
            listOf(Locale.KOREAN, Locale.ENGLISH, Locale.JAPANESE, Locale.CHINESE)
        val DEFAULT: Locale = Locale.KOREAN

        /** 저장해 둔 언어 코드(ko/en/ja/zh)를 Locale 로 되돌린다. 지원하지 않는 값이면 기본 언어. */
        fun localeOf(language: String?): Locale = SUPPORTED.firstOrNull { it.language == language } ?: DEFAULT
    }

    /**
     * 언어는 사용자가 선택하지 않고 브라우저(Accept-Language)에 따라서만 결정한다.
     * 지원 언어 중 일치하는 것이 없으면 기본(한국어)으로 표시한다.
     */
    @Bean
    fun localeResolver(): LocaleResolver =
        AcceptHeaderLocaleResolver().apply {
            supportedLocales = SUPPORTED
            setDefaultLocale(DEFAULT)
        }
}
