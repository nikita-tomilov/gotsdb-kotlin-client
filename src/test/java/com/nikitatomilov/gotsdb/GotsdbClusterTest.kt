package com.nikitatomilov.gotsdb

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.nikitatomilov.gotsdb.client.Client
import com.nikitatomilov.gotsdb.client.KeyValueSaveDemo
import com.nikitatomilov.gotsdb.dockerrunner.GotsdbDockerContainerRunner
import mu.KLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GotsdbClusterTest {

  private val dockerClientConfig =
      DefaultDockerClientConfig.createDefaultConfigBuilder()

  private val dockerClient = DockerClientBuilder
      .getInstance(dockerClientConfig)
      .build()

  @BeforeEach
  fun setUpNodes() {
    logger.warn { "Setting up $nodesCount nodes..." }
    nodes.addAll((0 until nodesCount).map {
      GotsdbDockerContainerRunner("gotsdb-test-node-$it", dockerClient, 5300 + it, 5123 + it)
    })
    nodes.forEach {
      it.setupContainer()
      it.startContainer()
      it.followOutput()
    }
  }

  @AfterEach
  fun tearDownNodes() {
    GotsdbClusterTest.tearDownNodes()
  }

  @Test
  fun test() {
    logger.warn { "Main test thing" }
    //given
    val client = Client("127.0.0.1", 5300)
    client.connect()
    //when
    client.save("TEST_KEY".toByteArray(), "TEST_VALUE".toByteArray())
    //then
    val keys = client.listKeys()
    keys.forEach {
      KeyValueSaveDemo.logger.warn { " - ${String(it)}" }
    }
  }

  companion object : KLogging() {

    private val nodes = ArrayList<GotsdbDockerContainerRunner>()

    private const val nodesCount = 1

    @BeforeAll
    @JvmStatic
    fun addShutdownHook() {
      Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
          tearDownNodes()
        }
      })
      logger.warn { "Hook installed" }
    }

    private fun tearDownNodes() {
      logger.warn { "Tearing down $nodesCount nodes..." }
      nodes.forEach {
        it.stopContainer()
        it.removeContainer()
      }
      nodes.clear()
    }
  }
}