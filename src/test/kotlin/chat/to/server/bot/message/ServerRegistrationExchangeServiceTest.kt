package chat.to.server.bot.message

import chat.to.server.bot.message.event.BotStatus
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.*
import org.springframework.web.client.RestTemplate

@SpringBootTest
@ActiveProfiles("unittest")
class ServerRegistrationExchangeServiceTest {

    @Autowired
    lateinit var serverRegistrationExchangeService: ServerRegistrationExchangeService

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var lastBotStatusForTesting: LastBotStatusForTesting

    lateinit var server: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        server = MockRestServiceServer.bindTo(restTemplate).build()
        lastBotStatusForTesting.clear()
    }

    @Test
    fun `register light bot on wemala server`() {
        server.expect(requestTo("http://server.unit.test/api/user"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath<String>("email", equalTo<String>("unit@test.bot")))
                .andExpect(jsonPath<String>("password", equalTo<String>("unit-test-bot-password")))
                .andExpect(jsonPath<String>("username", equalTo<String>("unit-test-bot-username")))
                .andRespond(withSuccess())

        assertThat(serverRegistrationExchangeService.registerBot()).isTrue()
        assertThat(lastBotStatusForTesting.lastBotStatus).isNull()

        server.verify()
    }

    @Test
    fun `register light bot on wemala server and server responds bad request`() {
        server.expect(requestTo("http://server.unit.test/api/user"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath<String>("email", equalTo<String>("unit@test.bot")))
                .andExpect(jsonPath<String>("password", equalTo<String>("unit-test-bot-password")))
                .andExpect(jsonPath<String>("username", equalTo<String>("unit-test-bot-username")))
                .andRespond(withBadRequest())

        assertThat(serverRegistrationExchangeService.registerBot()).isFalse()
        assertThat(lastBotStatusForTesting.lastBotStatus).isEqualTo(BotStatus.REGISTRATION_FAILED)

        server.verify()
    }

    @Test
    fun `register light bot on wemala server and server responds conflict`() {
        server.expect(requestTo("http://server.unit.test/api/user"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath<String>("email", equalTo<String>("unit@test.bot")))
                .andExpect(jsonPath<String>("password", equalTo<String>("unit-test-bot-password")))
                .andExpect(jsonPath<String>("username", equalTo<String>("unit-test-bot-username")))
                .andRespond(withStatus(HttpStatus.CONFLICT))

        assertThat(serverRegistrationExchangeService.registerBot()).isFalse()
        assertThat(lastBotStatusForTesting.lastBotStatus).isEqualTo(BotStatus.REGISTRATION_FAILED)

        server.verify()
    }

}