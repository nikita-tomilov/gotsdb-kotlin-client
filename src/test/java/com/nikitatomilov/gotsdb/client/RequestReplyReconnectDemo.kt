package com.nikitatomilov.gotsdb.client

import mu.KLogging
import java.lang.Exception
import java.time.Duration

object RequestReplyReconnectDemo : KLogging() {
  @JvmStatic
  fun main(args: Array<String>) {
    val client = Client("127.0.0.1", 5300)
    client.connect()
    client.save("TEST_KEY".toByteArray(), "TEST_VALUE".toByteArray())
    while (true) {
      try {
        val keys = client.listKeys()
        keys.forEach {
          logger.warn { " - ${String(it)}" }
        }
      } catch (e: Exception) {
        logger.error { "Error in listKeys: ${e.message}" }
      } finally {
        Thread.sleep(Duration.ofSeconds(5).toMillis())
      }
    }
  }
}