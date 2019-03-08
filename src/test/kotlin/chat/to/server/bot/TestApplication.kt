package chat.to.server.bot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestTemplate

@SpringBootApplication
class TestApplication {

	@Bean
	fun restTemplate(): RestTemplate {
		return RestTemplateBuilder().build()
	}

}

fun main(args: Array<String>) {
	runApplication<TestApplication>(*args)
}
