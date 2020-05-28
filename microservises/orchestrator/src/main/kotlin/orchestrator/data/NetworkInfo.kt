package orchestrator.data

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import orchestrator.clients.BlockClient
import orchestrator.clients.RepositoryClient

object NetworkInfo {
    private val blocks: HashMap<String, ArrayList<BlockData>> = HashMap()
    private var blockCounter: Int = 0

    private val repositories: ArrayList<String> = ArrayList()
    private var currentRepository: Int = 0

    private val sessions: HashMap<String, ProcessSession> = HashMap()

    private val blockClient: BlockClient = BlockClient()
    private val repositoryClient: RepositoryClient = RepositoryClient()

    private val repositoriesToRemove = ArrayList<RepositoryToRemove>()
    private val blocksToRemove = ArrayList<BlockToRemove>()

    private val maxCountOfIgnore: Int = 10

    init {
//        repositories.add("http://localhost:8000")

        GlobalScope.launch {
            while (true) {
                updateState()
                println("Network state updated ...")
                printNetworkState()
                delay(2000L)
            }
        }
    }

    fun setInProgress(pipelineId: Int, blockId: Int, sessionId: String): Boolean {
        if (!sessions.containsKey(sessionId)) {
            return false
        }

        val session = sessions[sessionId]!!
        session.pipeline.setBlockId(pipelineId, blockId)
        session.pipeline.setInProgress(blockId)
        return true
    }

    private fun addRepositoryToRemove(repositoryUri: String) {
        val repo = repositoriesToRemove.find { it ->
            it.repositoryUri == repositoryUri
        }

        if (repo == null) {
            repositoriesToRemove.add(RepositoryToRemove(repositoryUri))
            return
        }

        repo.countOfIgnore++
    }

    private fun addBlockToRemove(block: BlockData) {
        val blockToRemove = blocksToRemove.find { it ->
            it.blockData.id == block.id
        }

        if (blockToRemove == null) {
            blocksToRemove.add(BlockToRemove(block))
            return
        }

        blockToRemove.countOfIgnore++
    }

    private fun removeFailedServices() {
        // repositories
        for (repo in repositoriesToRemove) {
            if (repo.countOfIgnore >= maxCountOfIgnore) {
                removeRepository(repo.repositoryUri)
            }
        }

        repositoriesToRemove.removeIf { it.countOfIgnore >= maxCountOfIgnore }

        // blocks
        for (block in blocksToRemove) {
            if (block.countOfIgnore < maxCountOfIgnore) {
                continue
            }

            for ((k, v) in blocks) {
                if (v.removeIf { it.id == block.blockData.id }) {
                    continue
                }
            }
        }

        blocksToRemove.removeIf { it.countOfIgnore >= maxCountOfIgnore }

        val blockTypesToRemove = ArrayList<String>()
        for ((k, v) in blocks) {
            if (v.isEmpty()) {
                blockTypesToRemove.add(k)
            }
        }

        for (blockType in blockTypesToRemove) {
            blocks.remove(blockType)
        }
    }

    private fun updateState() {
        // repositories
        for (repo in repositories) {
            if (!repositoryClient.ping(repo)) {
                addRepositoryToRemove(repo)
            }
        }

        // blocks
        for ((k, v) in blocks) {
            for (block in v) {
                if (blockClient.ping(block.uri)) {
                    block.isAvailable = true
                } else {
                    addBlockToRemove(block)
                    block.isAvailable = false
                }
            }

//            for (block in blocksToRemove) {
//                v.removeIf { blockData -> blockData.id == block.id }
//            }
        }

        removeFailedServices()
    }

    private fun printNetworkState() {
        if (blocks.isEmpty() && repositories.isEmpty()) {
            println("Network is empty")
            return
        }

        println("Repositories:")

        if (repositories.isEmpty()) {
            println("empty")
        }

        for ((i, repo) in repositories.withIndex()) {
            println("$i. $repo")
        }

        println("Blocks:")

        if (blocks.isEmpty()) {
            println("empty")
        }

        var i = 0
        for ((k, v) in blocks) {
            for (block in v) {
                printBlock(i, block)
                i++
            }
        }

        printSessions()
    }

    private fun printSessions() {
        println("Sessions:")

        if (sessions.isEmpty()) {
            println("empty")
            return
        }

        for ((k, v) in sessions) {
            println("sessionId: ${v.sessionId}, repositoryUri: ${v.repositoryUri}")
            println("details:")
            printPipeline(pipeline = v.pipeline)
        }
    }

    private fun printPipeline(pipeline: Pipeline) {
        pipeline.printPipeline()
    }

    private fun printBlock(index: Int, block: BlockData) {
        // TODO: choose one variant

        // variant 1
        println(
            "$index. blockType: ${block.blockType} | "
                    + "blockId: ${block.id} | "
                    + "blockTaskLaunched: ${block.taskLaunched} | "
                    + "blockTaskLimit: ${block.taskLimit} | "
                    + "blockTaskUri: ${block.uri} | "
        )

        // variant 2
//        println("blockType:" + block.blockType)
//        println("blockId:" + block.id)
//        println("blockTaskLaunched:" + block.taskLaunched)
//        println("blockTaskLimit:" + block.taskLimit)
//        println("blockTaskUri:" + block.uri)
    }

    fun addSession(session: ProcessSession): Boolean {
        if (sessions.containsKey(session.sessionId)) {
            return false
        }

        sessions[session.sessionId] = session
        return true
    }

    fun removeSession(sessionId: String): Boolean {
        if (!sessions.containsKey(sessionId)) {
            return false
        }

        sessions.remove(sessionId)
        return true
    }

    fun getBlock(blockType: String): BlockData? {
        if (!blocks.containsKey(blockType)) {
            return null
        }

//        for (block in blocks[blockType]!!) {
//            if (block.taskLaunched < block.taskLimit && block.isAvailable) {
//                return block
//            }
//        }

        // all blocks are busy, find less busy block
        var lessBusyBlock: BlockData = blocks[blockType]!![0]

        for (block in blocks[blockType]!!) {
            if (block.isAvailable && block.taskLaunched < lessBusyBlock.taskLaunched) {
                lessBusyBlock = block
            }
        }

        return lessBusyBlock
    }

    fun getNextPipelineBlocks(sessionId: String): List<PipelineBlock> {
        if (!sessions.containsKey(sessionId)) {
            throw Exception("There is no such session.")
        }

        return sessions[sessionId]!!.pipeline.getAvailable()
    }

    fun getFreeRepository(): String? {
        if (repositories.isEmpty()) {
            return null
        }

        return repositories[currentRepository++ % repositories.size]
    }

    fun registerRepository(uri: String) = addRepository(uri)

    private fun addRepository(uri: String) {
        if (!repositories.contains(uri)) {
            repositories.add(uri)
        }
    }

    private fun removeRepository(uri: String) {
        if (!repositories.contains(uri)) {
            throw IllegalArgumentException("There is not such repository")
        }
        repositories.remove(uri)
    }

    fun registerBlock(blockType: String, uri: String, taskLimit: Int = 8, taskLaunched: Int = 0): String {
        addBlock(blockCounter, blockType, uri, taskLimit, taskLaunched)
        return blockCounter++.toString()
    }

    private fun addBlock(blockId: Int, blockType: String, uri: String, taskLimit: Int = 8, taskLaunched: Int = 0) {
        if (blocks.containsKey(blockType)) {
            if (blocks[blockType]!!.find { it -> it.id == blockId } != null) {
                throw IllegalArgumentException("This block already added")
            }
            blocks[blockType]!!.add(
                BlockData(
                    blockId,
                    blockType,
                    uri,
                    taskLimit,
                    taskLaunched
                )
            )
            return
        }

        val list = ArrayList<BlockData>()
        list.add(BlockData(blockId, blockType, uri, taskLimit, taskLaunched))
        blocks[blockType] = list
    }

    fun removeBlock(id: Int) {
        for ((k, v) in blocks) {
            for (i in v.indices) {
                if (v[i].id == id) {
                    blocks[k]!!.removeAt(i)
                    return
                }
            }
        }
        throw IllegalArgumentException("There is not such block")
    }

    fun removeBlock(id: Int, blockType: String) {
        if (!blocks.containsKey(blockType)) {
            throw IllegalArgumentException("There is not such block")
        }

        for (i in blocks[blockType]!!.indices) {
            if (blocks[blockType]!![i].id == id) {
                blocks[blockType]!!.removeAt(i)
                return
            }
        }

        throw IllegalArgumentException("There is not such block")
    }

    fun incBlockTaskLaunchedCount(id: Int) {
        for ((k, v) in blocks) {
            for (i in v.indices) {
                if (v[i].id == id) {
                    blocks[k]!![i].taskLaunched++
                    return
                }
            }
        }
        throw IllegalArgumentException("There is not such block")
    }

    fun decBlockTaskLaunchedCount(id: Int) {
        for ((k, v) in blocks) {
            for (i in v.indices) {
                if (v[i].id == id) {
//                    if (blocks[k]!![i].taskLaunched == 0u) {
//                        throw Exception("There is no launched tasks in block Id=$id")
//                    }
                    blocks[k]!![i].taskLaunched--
                    return
                }
            }
        }
        throw IllegalArgumentException("There is not such block")
    }

    fun incBlockTaskLaunchedCount(blockType: String, id: Int): Boolean {
        return ++blocks[blockType]!![id].taskLaunched < blocks[blockType]!![id].taskLimit
    }

    fun decBlockTaskLaunchedCount(blockType: String, id: Int): Boolean {
        if (blocks[blockType]!![id].taskLaunched == 0) {
            return false
        }

        --blocks[blockType]!![id].taskLaunched
        return true
    }

    fun blockFinishedSuccess(sessionId: String, blockId: Int): Boolean {
        decBlockTaskLaunchedCount(blockId)

        if (!sessions.containsKey(sessionId)) {
            return false
        }

        sessions[sessionId]!!.pipeline.setFinished(blockId, "${sessionId}_$blockId.zip")
        return true
    }

    fun blockFinishedError(sessionId: String, blockId: Int): Boolean {
        decBlockTaskLaunchedCount(blockId)

        if (!sessions.containsKey(sessionId)) {
            return false
        }

        removeSession(sessionId)
        return true
    }

    fun getSessionRepositoryUri(sessionId: String): String? {
        if (!sessions.containsKey(sessionId)) {
            return null
        }

        return sessions[sessionId]!!.repositoryUri
    }

    private class RepositoryToRemove(val repositoryUri: String) {
        var countOfIgnore: Int = 0
    }

    private class BlockToRemove(val blockData: BlockData) {
        var countOfIgnore: Int = 0
    }
}