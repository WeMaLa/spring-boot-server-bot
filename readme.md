# to.chat server bot library

A simple Spring Boot library to bind the WeMaLa server as a bot. 
This library is written in Kotlin.

## Requirements

* Kotlin 1.3.30 (comes as maven dependency)
* Spring Boot 2.1.x

#### Access to the nexus

In maven ```settings.xml```

```xml
<servers>
    <server>
        <id>wemala</id>
        <username>my-user</username>
        <password>my-password</password>
    </server>
    <server>
        <id>wemala-snapshots</id>
        <username>my-user</username>
        <password>my-password</password>
    </server>
</servers>
```

## Usage

#### Add dependency to maven

```xml
<dependency>
  <groupId>chat.to</groupId>
  <artifactId>spring-boot-server-bot</artifactId>
  <version>0.0.1</version>
</dependency>
```

#### Update application.properties

Add bot credentials to ```application.properties```

```properties
wemala.server.url=http://dev.to.chat
wemala.bot.identifier=my-bot@to.chat
wemala.bot.username=my-first-bot
wemala.bot.password=my-secure-bot-password
```

#### Add Spring Boot component scan

```kotlin
@ComponentScan("chat.to")
@SpringBootApplication
class MyWeMaLaBotAdapterApplication
```

#### Receive and send messages

By using ```ServerMessageExchangeService.kt``` you can receive and send messages:

```kotlin
@Component
class ServerMessageScheduler(private var serverMessageExchangeService: ServerMessageExchangeService) {

    private val log = LoggerFactory.getLogger(ServerMessageScheduler::class.java)

    @Scheduled(fixedRate = 3000)
    fun scheduleUnreadMessages() {
        log.info("Start retrieving latest messages")
        
        serverMessageExchangeService.retrieveMessages().forEach {
            val channelIdentifier = it._links.channel.href.substringAfter("/api/channel/")
            log.info("Received message ${it.text} from channel $channelIdentifier")
            serverMessageExchangeService.sendMessage(channelIdentifier, "pong")
        }
        
        log.info("${retrieveMessages.size} messages retrieved")
    }

}
```

## FAQ

#### Do I need an existing WeMaLa server account

No, you don't need an existing WeMaLa server account. 
If necessary, the bot is generated the first time the messages are loaded.

#### Are the messages reloaded again and again?

No, loaded messages are marked as read. This means that no message is loaded twice.

#### What happens if there is a server communication error?

There is a ```BotStatus.kt```, which indicates whether the last messages could be picked up.

You can observe a BotStatus event: 

```kotlin
@Component
class BotStatusEventListener {

    @EventListener
    fun updateStatus(status: BotStatus) {
        // do whatever you want
    }

}
```

```BotStatus.kt``` consists of the following values 

* OK
* AUTHENTICATION_FAILED
* REGISTRATION_FAILED
* RECEIVE_MESSAGES_FAILED
* MARK_MESSAGES_FAILED