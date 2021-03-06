package chat.to.server.bot.message

import chat.to.server.bot.configuration.WeMaLaConfiguration
import chat.to.server.bot.message.event.BotStatus
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

@Service
class ServerRegistrationExchangeService(private var botConfiguration: WeMaLaConfiguration,
                                        private var applicationEventPublisher: ApplicationEventPublisher,
                                        private var restTemplate: RestTemplate) {

    private val log = LoggerFactory.getLogger(ServerRegistrationExchangeService::class.java)

    fun registerBot(): Boolean {
        log.info("Register new bot on wemala server")
        val httpEntity = HttpEntity<Any>(UserRegistrationRequest(botConfiguration.bot!!.identifier, botConfiguration.bot!!.password, botConfiguration.bot!!.username))

        return try {
            restTemplate.exchange(botConfiguration.server!!.url + "/api/user", HttpMethod.POST, httpEntity, Any::class.java)
            true
        } catch (e: Exception) {
            if (e is HttpStatusCodeException) {
                log.error("Register bot on wemala server failed with code '${e.statusCode}' and message '${e.message}'")
            } else {
                log.error("Register bot on wemala server failed with message '${e.message}'")
            }
            applicationEventPublisher.publishEvent(BotStatus.REGISTRATION_FAILED)
            false
        }
    }

    data class UserRegistrationRequest internal constructor(val email: String, val password: String, val username: String)

}