package chat.to.server.bot.message

import chat.to.server.bot.message.event.BotStatus
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class LastBotStatusForTesting {

    final var lastBotStatus: BotStatus? = null
        private set

    @EventListener
    fun updateStatus(status: BotStatus) {
        lastBotStatus = status
    }

    fun clear() {
        lastBotStatus = null
    }

}