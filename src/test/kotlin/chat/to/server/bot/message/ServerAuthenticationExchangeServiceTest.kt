package chat.to.server.bot.message

import chat.to.server.bot.message.event.BotStatus
import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.IsEqual
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators.*
import org.springframework.web.client.RestTemplate

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("unittest")
class ServerAuthenticationExchangeServiceTest {

    @Autowired
    private lateinit var serverAuthenticationExchangeService: ServerAuthenticationExchangeService

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var lastBotStatusForTesting: LastBotStatusForTesting

    @MockBean
    private lateinit var serverRegistrationExchangeService: ServerRegistrationExchangeService

    private lateinit var server: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        server = MockRestServiceServer.bindTo(restTemplate).build()
        lastBotStatusForTesting.clear()
    }

    @Test
    fun `authenticate light bot on wemala server`() {
        val response = ServerAuthenticationExchangeService.JwtAuthenticationResponse()
        response.token = "unit-test-auth-token"
        server.expect(MockRestRequestMatchers.requestTo("http://server.unit.test/api/auth/login"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("identifier", IsEqual.equalTo<String>("unit@test.bot")))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("password", IsEqual.equalTo<String>("unit-test-bot-password")))
                .andRespond(withSuccess(ObjectMapper().writeValueAsString(response), MediaType.APPLICATION_JSON))

        assertThat(serverAuthenticationExchangeService.authenticate()).isEqualTo("unit-test-auth-token")
        assertThat(lastBotStatusForTesting.lastBotStatus).isNull()

        server.verify()
    }

    @Test
    fun `authenticate light bot on wemala server and servers first time responds unauthorized`() {
        val response = ServerAuthenticationExchangeService.JwtAuthenticationResponse()
        response.token = "unit-test-auth-token"
        whenever(serverRegistrationExchangeService.registerBot()).thenReturn(true)

        server.expect(MockRestRequestMatchers.requestTo("http://server.unit.test/api/auth/login"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("identifier", IsEqual.equalTo<String>("unit@test.bot")))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("password", IsEqual.equalTo<String>("unit-test-bot-password")))
                .andRespond(withUnauthorizedRequest())

        server.expect(MockRestRequestMatchers.requestTo("http://server.unit.test/api/auth/login"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("identifier", IsEqual.equalTo<String>("unit@test.bot")))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("password", IsEqual.equalTo<String>("unit-test-bot-password")))
                .andRespond(withSuccess(ObjectMapper().writeValueAsString(response), MediaType.APPLICATION_JSON))

        assertThat(serverAuthenticationExchangeService.authenticate()).isEqualTo("unit-test-auth-token")
        assertThat(lastBotStatusForTesting.lastBotStatus).isNull()

        server.verify()
        verify(serverRegistrationExchangeService).registerBot()
    }

    @Test
    fun `authenticate light bot on wemala server and servers responds unauthorized and registration failed too`() {
        val response = ServerAuthenticationExchangeService.JwtAuthenticationResponse()
        response.token = "unit-test-auth-token"
        whenever(serverRegistrationExchangeService.registerBot()).thenReturn(false)

        server.expect(MockRestRequestMatchers.requestTo("http://server.unit.test/api/auth/login"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("identifier", IsEqual.equalTo<String>("unit@test.bot")))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("password", IsEqual.equalTo<String>("unit-test-bot-password")))
                .andRespond(withUnauthorizedRequest())

        assertThat(serverAuthenticationExchangeService.authenticate()).isNull()
        assertThat(lastBotStatusForTesting.lastBotStatus).isEqualTo(BotStatus.AUTHENTICATION_FAILED)

        server.verify()
        verify(serverRegistrationExchangeService).registerBot()
    }

    @Test
    fun `authenticate light bot on wemala server and servers responds bad request`() {
        server.expect(MockRestRequestMatchers.requestTo("http://server.unit.test/api/auth/login"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("identifier", IsEqual.equalTo<String>("unit@test.bot")))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("password", IsEqual.equalTo<String>("unit-test-bot-password")))
                .andRespond(withBadRequest())

        assertThat(serverAuthenticationExchangeService.authenticate()).isNull()
        assertThat(lastBotStatusForTesting.lastBotStatus).isEqualTo(BotStatus.AUTHENTICATION_FAILED)

        server.verify()
    }

    @Test
    fun `authenticate light bot on wemala server and servers responds conflict`() {
        server.expect(MockRestRequestMatchers.requestTo("http://server.unit.test/api/auth/login"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("identifier", IsEqual.equalTo<String>("unit@test.bot")))
                .andExpect(MockRestRequestMatchers.jsonPath<String>("password", IsEqual.equalTo<String>("unit-test-bot-password")))
                .andRespond(withStatus(HttpStatus.CONFLICT))

        assertThat(serverAuthenticationExchangeService.authenticate()).isNull()
        assertThat(lastBotStatusForTesting.lastBotStatus).isEqualTo(BotStatus.AUTHENTICATION_FAILED)

        server.verify()
    }
}