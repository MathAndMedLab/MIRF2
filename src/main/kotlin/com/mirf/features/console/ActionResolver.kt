package com.mirf.features.console

import java.util.function.Consumer

object ActionResolver {

    fun getAction(action: CommandAction): Consumer<ConsoleContext> {
        return when (action) {
            CommandAction.PrintHelp -> Consumer { printHelp() }
            CommandAction.AddBlockToPipeline -> Consumer { addBlock() }
        }
    }

    private fun addBlock() {

    }

    fun printHelp() {
        val result = CommandParser.commands.map { x -> x.name + " - " + x.description + System.lineSeparator() }
            .joinToString { x -> x }
        print(result)
    }
}