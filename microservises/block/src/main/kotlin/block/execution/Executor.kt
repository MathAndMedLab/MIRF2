package block.execution

import block.clients.OrchestratorClient
import block.clients.ProcessingResult
import block.info.BlockInfo
import block.info.BlockType
import block.storage.StorageProperties
import com.mirf.core.data.CollectionData
import com.mirf.core.data.Data
import com.mirf.features.repository.LocalRepositoryCommander
import com.mirf.features.repositoryaccessors.data.RepoRequest
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.lang.reflect.Method
import java.net.URLClassLoader

object Executor {
    private var id: String? = null
    private lateinit var properties: StorageProperties
    private lateinit var blockInfo: BlockInfo
    private lateinit var dataCollector: DataCollector

    private lateinit var inputClass: Class<*>
    private lateinit var outputClass: Class<*>
    private lateinit var algorithmClass: Class<*>
    private lateinit var algorithmObject: Any

    private lateinit var executionMethod: Method

    private lateinit var orchestratorClient: OrchestratorClient

    fun init(info: BlockInfo, _orchestratorClient: OrchestratorClient, constructorParams: List<Any>) {
        blockInfo = info
        properties = StorageProperties
        orchestratorClient = _orchestratorClient

        //val classLoader = ClassLoader.getSystemClassLoader() as URLClassLoader

        // input data
        val input = javaClass.classLoader
            .loadClass(blockInfo.inputClassName)
        inputClass = input

        // output data
        val output = javaClass.classLoader
            .loadClass(blockInfo.outputClassName)
        outputClass = output

        // algorithm
        val algorithm =
            javaClass.classLoader
                .loadClass(blockInfo.algorithmClassName)//.getDeclaredConstructor().newInstance()
        algorithmClass = algorithm

        // execute Method
        val method = algorithm.getDeclaredMethod("execute", inputClass)
        executionMethod = method


        when {
            constructorParams.isEmpty() -> {
                algorithmObject = algorithmClass.getDeclaredConstructor().newInstance()
            }
            constructorParams.size == 1 -> {
                algorithmObject = algorithmClass.getDeclaredConstructor(
                    constructorParams[0].javaClass
                ).newInstance()
            }
            constructorParams.size == 2 -> {
                algorithmObject = algorithmClass.getDeclaredConstructor(
                    constructorParams[0].javaClass,
                    constructorParams[1].javaClass
                ).newInstance()
            }
            constructorParams.size == 3 -> {
                algorithmObject = algorithmClass.getDeclaredConstructor(
                    constructorParams[0].javaClass,
                    constructorParams[1].javaClass,
                    constructorParams[2].javaClass
                ).newInstance()
            }
            constructorParams.size > 3 -> {
                throw Exception("too much constructor params")
            }
        }

        // get Id
        id = _orchestratorClient.register(info.port)
        if (id == null) {
            throw Exception("Failed to register block in system. Try check configuration.")
        }
        dataCollector = DataCollector(id!!)
    }

    private fun readInputObjects(fileNames: List<String>): List<Any> {
        val objects = ArrayList<Any>()
        for (fileName in fileNames) {
            val file = File(fileName)
            val fileInputStream = FileInputStream(file)
            val objectInputStream = ObjectInputStream(fileInputStream)
            val inputObject = objectInputStream.readObject()
            objectInputStream.close()
            objects.add(inputObject)
        }

        return objects
    }

    fun run(sessionId: String, fileNames: List<String>): String {
        if (fileNames.isEmpty()) {
            throw Exception("empty input list")
        }

        val inputObject: Any

        inputObject =
            if (fileNames.size == 1) {
                readInputObjects(fileNames)[0]
            } else {
                val objects = readInputObjects(fileNames) as List<Data>
                val collectionData = CollectionData(objects)
                collectionData
            }

        val res = executionMethod.invoke(algorithmObject, inputObject)

        println("BLOCK METHOD EXECUTED")
        // folder name - sessionId_blockId
        return dataCollector.storeResult(res, sessionId)
    }

    fun notifyOrchestrator(result: ProcessingResult, sessionId: String, errorMessage: String = "") {
        orchestratorClient.notify(result, sessionId, errorMessage)
    }
}