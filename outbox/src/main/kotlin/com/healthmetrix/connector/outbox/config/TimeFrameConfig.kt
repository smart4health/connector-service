package com.healthmetrix.connector.outbox.config

import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.outbox.invitationtoken.TimeFrame
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class TimeFrameConfig {
    @Bean
    fun provideTimeFrame(timeFrameFactory: TimeFrameFactory) = timeFrameFactory.make()
}

@ConfigurationProperties("totp.timeframe")
@ConstructorBinding
class TimeFrameFactory(
    private val duration: Duration,
    private val num: Int,
) {
    init {
        logger.info("Created TimeFrameFactory with duration $duration")
    }

    fun make() = TimeFrame(duration, num)
}
