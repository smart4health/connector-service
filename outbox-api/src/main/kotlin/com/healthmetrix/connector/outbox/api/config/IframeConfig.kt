package com.healthmetrix.connector.outbox.api.config

import com.healthmetrix.connector.commons.Bcp47LanguageTag
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import org.springframework.web.servlet.i18n.SessionLocaleResolver

@Configuration
class IframeConfig(
    @Value("\${default-locale}") private val defaultLanguageTag: String,
) : WebMvcConfigurer {
    @Bean
    fun localeResolver(): LocaleResolver = SessionLocaleResolver().apply {
        setDefaultLocale(Bcp47LanguageTag(defaultLanguageTag).locale)
    }

    @Bean
    fun localeChangeInterceptor(): LocaleChangeInterceptor = LocaleChangeInterceptor().apply {
        paramName = "lang"
    }

    @Bean
    fun messageSource(): MessageSource = ReloadableResourceBundleMessageSource().apply {
        setBasename("classpath:locale/messages")
        setDefaultEncoding("UTF-8")
    }

    @Override
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(localeChangeInterceptor())
    }
}
