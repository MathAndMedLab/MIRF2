package orchestrator

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.util.*
import kotlinx.coroutines.*

@SpringBootApplication
class App

fun main(args: Array<String>) {
    SpringApplication.run(App::class.java, *args)
    return
}
