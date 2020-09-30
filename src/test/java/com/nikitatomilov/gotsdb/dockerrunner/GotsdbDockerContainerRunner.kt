package com.nikitatomilov.gotsdb.dockerrunner

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.core.command.LogContainerResultCallback
import mu.KLogging
import java.io.File
import java.nio.file.Files

class GotsdbDockerContainerRunner(
  private val nodeName: String,
  private val dockerClient: DockerClient,
  private val hostMachineClientPort: Int,
  private val hostMachineClusterPort: Int,
  private val clusterMode: String = "single-instance",
  private val clusterNodesString: String = "localhost:5123;localhost:5124"
) {

  private lateinit var containerId: String

  //docker run -v `pwd`/config:/config -p 5300:5300 -p 5123:5123 gotsdb-srv:latest
  fun setupContainer() {
    val tmp = createTempConfigDirectory()
    val cmd = dockerClient.createContainerCmd("gotsdb-srv:latest")
        .withName(nodeName)
        .withDomainName("localhost")
        .withPortBindings(
            PortBind(hostMachineClientPort.toLong(), 5300).toPortBinding(),
            PortBind(hostMachineClusterPort.toLong(), 5123).toPortBinding())
        .withBinds(FolderBind(tmp.absolutePath, "/config").toBinding())
        .withAttachStdout(true)
        .withAttachStdout(true)
    val container = cmd.exec()
    containerId = container.id
  }

  fun startContainer() {
    dockerClient.startContainerCmd(containerId).exec()
  }

  fun followOutput() {
    val cmd = dockerClient.logContainerCmd(containerId)
        .withFollowStream(false)
        .withStdOut(true)
        .withStdOut(true)
        .withSince(0)
    val bgt = Thread {
      cmd.exec(object : LogContainerResultCallback() {
        override fun onNext(item: Frame?) {
          logger.warn { "[$nodeName]: $item" }
        }
      }).awaitCompletion()
    }
    bgt.isDaemon = true
    bgt.start()
  }

  fun stopContainer() {
    dockerClient.stopContainerCmd(containerId).exec()
  }

  fun removeContainer() {
    dockerClient.removeContainerCmd(containerId).exec()
  }

  private fun createTempConfigDirectory(): File {
    val tmp = Files.createTempDirectory(nodeName).toFile()
    val logFile = File(tmp, LOG_FILE_NAME).toPath()
    Files.write(logFile, getLogFileContent().toByteArray())
    val cfgFile = File(tmp, APP_CONFIG_FILE_NAME).toPath()
    Files.write(cfgFile, getAppFileContent().toByteArray())
    return tmp
  }

  private fun getAppFileContent() = """
    grpc.listenAddress=0.0.0.0:5300
    kvs.engine=file
    kvs.fileKVSPath=/tmp/gotsdb/kvs
    tss.engine=lsm
    tss.filePath=/tmp/gotsdb/tss
    server.mode=$clusterMode
    #cluster, single-instance
    cluster.listenAddress=localhost:5123
    cluster.knownNodes=$clusterNodesString
    cluster.readingConsistency=none
    #none, any, all
    cluster.writingConsistency=none
    """.trimIndent()

  companion object : KLogging() {
    const val LOG_FILE_NAME = "log4go.json"
    fun getLogFileContent() = """
      {
        "console": {
          "enable": true,
          "level": "INFO",
          "pattern": "[%D %T] [%S] [%L] %M"
        }
      }
    """.trimIndent()

    const val APP_CONFIG_FILE_NAME = "app.properties"
  }
}