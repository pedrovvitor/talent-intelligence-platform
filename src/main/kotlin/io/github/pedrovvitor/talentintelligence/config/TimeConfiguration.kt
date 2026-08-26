package io.github.pedrovvitor.talentintelligence.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TimeConfiguration {
    @Bean
    fun systemClock(): Clock = Clock.systemUTC()
}
