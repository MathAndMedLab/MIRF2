package com.mirf.core.pipeline

import com.mirf.core.data.Data
import com.mirf.core.data.MirfException

class MultipleInputBlock<I : Data, O : Data>(
    name: String, pipelineKeeper: PipelineKeeper,
    private val senders: List<Any>,
    private val action: (HashMap<Any, I>) -> O,
) : PipelineBlock<I, O>(name, pipelineKeeper) {

    private var receivedData: HashMap<Any, I> = hashMapOf()

    override fun inputReady(sender: Any, input: I) {
        if (!senders.contains(sender))
            throw MirfException("$sender does not specified in $name senders list")

        receivedData[sender] = input
        if (senders.all { receivedData.containsKey(it) }) {
            val data = action(receivedData)
            onDataReady(this, data)
        }
    }

    override fun flush() {
        receivedData = hashMapOf()
    }
}