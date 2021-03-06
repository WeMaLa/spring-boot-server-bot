package chat.to.server.bot.message

import chat.to.server.bot.configuration.WeMaLaConfiguration
import chat.to.server.bot.message.event.BotStatus
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

@Service
class ServerMessageExchangeService(private var botConfiguration: WeMaLaConfiguration,
                                   private var restTemplate: RestTemplate,
                                   private var applicationEventPublisher: ApplicationEventPublisher,
                                   private var serverAuthenticationExchangeService: ServerAuthenticationExchangeService) {

    private val log = LoggerFactory.getLogger(ServerMessageExchangeService::class.java)

    fun retrieveMessages(): List<Message> {
        val token = serverAuthenticationExchangeService.authenticate()

        return if (token != null) {
            val httpEntity = createHttpEntity(token)
            val messages = loadUnreadMessages(httpEntity)

            messages.forEach { m -> markAsRead(m.identifier, httpEntity) }

            return messages
        } else {
            emptyList()
        }
    }

    fun sendMessage(channelIdentifier: String, message: String) {
        val token = serverAuthenticationExchangeService.authenticate()

        if (channelIdentifier.isBlank()) {
            log.warn("Could not send message '$message' to channel because channel identifier is blank")
        } else if (message.isBlank()) {
            log.warn("Could not send blank message to channel '$channelIdentifier")
        } else {
            if (token != null) {
                val url = botConfiguration.server!!.url + "/api/message"
                val httpEntity = createHttpEntity(token, SendMessageRequestBody(message, channelIdentifier))
                try {
                    restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void::class.java)
                } catch (e: Exception) {
                    log.error("Could not send message '$message' to channel '$channelIdentifier' because of an exception", e)
                }
            } else {
                log.error("Could not send message '$message' to channel '$channelIdentifier' because authentication failed")
            }
        }
    }

    private fun markAsRead(messageIdentifier: String, httpEntity: HttpEntity<Any>) {
        try {
            val url = botConfiguration.server!!.url + "/api/message/$messageIdentifier/read"
            restTemplate.exchange(url, HttpMethod.PATCH, httpEntity, String::class.java)
            applicationEventPublisher.publishEvent(BotStatus.OK)
        } catch (e: Exception) {
            if (e is HttpStatusCodeException) {
                log.error("Mark message '$messageIdentifier' as read on wemala server failed with code '${e.statusCode}' and message '${e.message}'")
            } else {
                log.error("Mark message '$messageIdentifier' as read on wemala server failed with message '${e.message}'")
            }
            applicationEventPublisher.publishEvent(BotStatus.MARK_MESSAGES_FAILED)
        }
    }

    private fun loadUnreadMessages(httpEntity: HttpEntity<Any>): List<Message> {
        return try {
            val url = botConfiguration.server!!.url + "/api/messages?status=SEND&status=RECEIVED"
            val messages = restTemplate.exchange(url, HttpMethod.GET, httpEntity, MessageResponse::class.java).body?.content!!.asList()
            applicationEventPublisher.publishEvent(BotStatus.OK)
            return messages
        } catch (e: Exception) {
            if (e is HttpStatusCodeException) {
                log.error("Retrieve message from wemala server failed with code '${e.statusCode}' and message '${e.message}'")
            } else {
                log.error("Retrieve message from wemala server failed with message '${e.message}'")
            }
            applicationEventPublisher.publishEvent(BotStatus.RECEIVE_MESSAGES_FAILED)
            emptyList()
        }
    }

    private fun createHttpEntity(token: String?, body: Any? = null): HttpEntity<Any> {
        val httpHeaders = HttpHeaders()
        httpHeaders.set("Authorization", token)
        return if (body != null) HttpEntity(body, httpHeaders) else HttpEntity(httpHeaders)
    }

    class MessageResponse {
        var content: Array<Message> = arrayOf()
    }

    class Message {
        var identifier: String = ""
        var text: String = ""
        var createDate: String = ""
        var _links: Links = Links()

        class Links {
            var channel: Link = Link()
            var sender: Link = Link()

            class Link {
                var href: String = ""
            }
        }
    }

    data class SendMessageRequestBody(val text: String, val channelIdentifier: String)
}